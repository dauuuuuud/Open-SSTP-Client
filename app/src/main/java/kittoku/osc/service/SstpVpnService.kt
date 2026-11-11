package kittoku.osc.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import kittoku.osc.R
import kittoku.osc.SharedBridge
import kittoku.osc.bus.ConnectionBus
import kittoku.osc.bus.LogBus
import kittoku.osc.bus.StatsBus
import kittoku.osc.bus.VpnState
import kittoku.osc.control.Controller
import kittoku.osc.control.LogWriter
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getURIPrefValue
import kittoku.osc.preference.accessor.resetReconnectionLife
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


const val ACTION_VPN_CONNECT = "kittoku.osc.connect"
const val ACTION_VPN_DISCONNECT = "kittoku.osc.disconnect"

const val NOTIFICATION_ERROR_CHANNEL = "ERROR"
const val NOTIFICATION_RECONNECT_CHANNEL = "RECONNECT"
const val NOTIFICATION_DISCONNECT_CHANNEL = "DISCONNECT"

const val NOTIFICATION_ERROR_ID = 1
const val NOTIFICATION_RECONNECT_ID = 2
const val NOTIFICATION_DISCONNECT_ID = 3
const val NOTIFICATION_CERT_CHANNEL = "CERT"
const val NOTIFICATION_CERT_ID = 4


internal class SstpVpnService : VpnService() {
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManagerCompat
    internal lateinit var scope: CoroutineScope

    internal var logWriter: LogWriter? = null
    private var controller: Controller? = null

    private var jobReconnect: Job? = null
    private var jobNotification: Job? = null

    private var userRequestedDisconnect = false

    private val isKillSwitchEnabled: Boolean
        get() = getBooleanPrefValue(OscPrefKey.KILL_SWITCH_ENABLED, prefs)

    private fun setRootState(state: Boolean) {
        setBooleanPrefValue(state, OscPrefKey.ROOT_STATE, prefs)
    }

    private fun requestTileListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TileService.requestListeningState(this,
                ComponentName(this, SstpTileService::class.java)
            )
        }
    }

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(this)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_VPN_CONNECT -> {
                userRequestedDisconnect = false

                controller?.kill(false, null)

                beForegrounded()
                resetReconnectionLife(prefs)

                if (isKillSwitchEnabled) {
                    // the kill switch requires automatic reconnection attempts
                    setBooleanPrefValue(true, OscPrefKey.RECONNECTION_ENABLED, prefs)
                }

                if (getBooleanPrefValue(OscPrefKey.LOG_DO_SAVE_LOG, prefs)) {
                    prepareLogWriter()
                }

                LogBus.append("VPN", "Establishing SSTP connection")
                ConnectionBus.setConnecting()

                initializeClient()

                setRootState(true)
                requestTileListening()

                START_STICKY
            }

            ACTION_VPN_DISCONNECT -> {
                userRequestedDisconnect = true

                ConnectionBus.setDisconnecting()
                LogBus.append("VPN", "Disconnecting on user request")

                runBlocking { jobReconnect?.cancelAndJoin() }

                controller?.disconnect()
                controller = null

                close()

                START_NOT_STICKY
            }

            else -> {
                runBlocking { jobReconnect?.cancelAndJoin() }

                controller?.disconnect()
                controller = null

                close()

                START_NOT_STICKY
            }
        }
    }

    private fun initializeClient() {
        controller = Controller(SharedBridge(this)).also {
            it.launchJobMain()
        }

        watchConnectionState()
    }

    private fun watchConnectionState() {
        jobNotification?.cancel()
        jobNotification = scope.launch {
            var lastText: String? = null

            while (isActive) {
                val snapshot = ConnectionBus.snapshot.value
                val text = when (snapshot.state) {
                    VpnState.CONNECTED -> {
                        val traffic = StatsBus.snapshot.value
                        "Connected - down ${formatBytes(traffic.downloadBytesPerSecond)}/s, up ${formatBytes(traffic.uploadBytesPerSecond)}/s"
                    }
                    VpnState.CONNECTING -> "Connecting..."
                    VpnState.DISCONNECTING -> "Disconnecting..."
                    VpnState.ERROR -> "Error: ${snapshot.errorMessage ?: "unknown"}"
                    else -> "Disconnected"
                }

                if (text != lastText) {
                    updateForegroundNotification(text, snapshot.state)
                    lastText = text
                }

                delay(if (snapshot.state == VpnState.CONNECTED) 2_000 else 500)
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    internal fun launchJobReconnect() {
        jobReconnect = scope.launch {
            try {
                getIntPrefValue(OscPrefKey.RECONNECTION_LIFE, prefs).also {
                    val life = it - 1
                    setIntPrefValue(life, OscPrefKey.RECONNECTION_LIFE, prefs)

                    val message = "Reconnection will be tried (LIFE = $life)"
                    LogBus.append("VPN", message)
                    notifyMessage(message, NOTIFICATION_RECONNECT_ID, NOTIFICATION_RECONNECT_CHANNEL)
                    logWriter?.report(message)
                }

                delay(getIntPrefValue(OscPrefKey.RECONNECTION_INTERVAL, prefs) * 1000L)

                if (isKillSwitchEnabled) {
                    // never give up while the kill switch is armed
                    resetReconnectionLife(prefs)
                }

                LogBus.append("VPN", "Reconnecting")
                ConnectionBus.setConnecting()
                initializeClient()
            } catch (_: CancellationException) { }
            finally {
                cancelNotification(NOTIFICATION_RECONNECT_ID)
            }
        }
    }

    private fun prepareLogWriter() {
        val currentDateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val filename = "log_osc_${currentDateTime}.txt"

        val prefURI = getURIPrefValue(OscPrefKey.LOG_DIR, prefs)
        if (prefURI == null) {
            notifyError("LOG: ERR_NULL_PREFERENCE")
            return
        }

        val dirURI = DocumentFile.fromTreeUri(this, prefURI)
        if (dirURI == null) {
            notifyError("LOG: ERR_NULL_DIRECTORY")
            return
        }

        val fileURI = dirURI.createFile("text/plain", filename)
        if (fileURI == null) {
            notifyError("LOG: ERR_NULL_FILE")
            return
        }

        val stream = contentResolver.openOutputStream(fileURI.uri, "wa")
        if (stream == null) {
            notifyError("LOG: ERR_NULL_STREAM")
            return
        }

        logWriter = LogWriter(stream)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf(
                NOTIFICATION_ERROR_CHANNEL,
                NOTIFICATION_RECONNECT_CHANNEL,
                NOTIFICATION_DISCONNECT_CHANNEL,
            ).map {
                NotificationChannel(it, it, NotificationManager.IMPORTANCE_DEFAULT)
            }.also {
                notificationManager.createNotificationChannels(it)
            }
        }
    }

    private fun buildAction(action: String, title: String, icon: Int): NotificationCompat.Action {
        val pendingIntent = PendingIntent.getService(
            this,
            if (action == ACTION_VPN_CONNECT) 1 else 0,
            Intent(this, SstpVpnService::class.java).setAction(action),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action(icon, title, pendingIntent)
    }

    private fun beForegrounded() {
        createChannels()

        val builder = NotificationCompat.Builder(this, NOTIFICATION_DISCONNECT_CHANNEL).also {
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setOngoing(true)
            it.setAutoCancel(false)
            it.setOnlyAlertOnce(true)
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            it.setContentTitle("SSTP VPN")
            it.setContentText("Connecting...")
            it.addAction(buildAction(ACTION_VPN_DISCONNECT, "DISCONNECT", R.drawable.ic_baseline_close_24))
        }

        startForeground(NOTIFICATION_DISCONNECT_ID, builder.build())
    }

    private fun updateForegroundNotification(text: String, state: VpnState) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_DISCONNECT_CHANNEL).also {
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setOngoing(true)
            it.setAutoCancel(false)
            it.setOnlyAlertOnce(true)
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            it.setContentTitle("SSTP VPN")
            it.setContentText(text)
            it.clearActions()

            when (state) {
                VpnState.CONNECTED, VpnState.CONNECTING, VpnState.DISCONNECTING ->
                    it.addAction(buildAction(ACTION_VPN_DISCONNECT, "DISCONNECT", R.drawable.ic_baseline_close_24))
                else ->
                    it.addAction(buildAction(ACTION_VPN_CONNECT, "CONNECT", R.drawable.ic_baseline_vpn_lock_24))
            }
        }

        tryNotify(builder.build(), NOTIFICATION_DISCONNECT_ID)
    }

    internal fun notifyMessage(message: String, id: Int, channel: String) {
        NotificationCompat.Builder(this, channel).also {
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            it.setContentText(message)
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setAutoCancel(true)

            tryNotify(it.build(), id)
        }
    }

    internal fun notifyError(message: String) {
        LogBus.appendError("ERROR", message)
        ConnectionBus.setError(message)

        if (isKillSwitchEnabled) {
            LogBus.append("KILL SWITCH", "Connection lost - traffic must not leak, reconnecting")
        }

        notifyMessage(message, NOTIFICATION_ERROR_ID, NOTIFICATION_ERROR_CHANNEL)
    }

    internal fun tryNotify(notification: Notification, id: Int) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(id, notification)
        }
    }

    internal fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    internal fun close() {
        jobNotification?.cancel()
        StatsBus.stopSampling()

        if (ConnectionBus.state != VpnState.ERROR) {
            ConnectionBus.setDisconnected()
        }

        LogBus.append("VPN", "Connection closed")

        setRootState(false)
        requestTileListening()

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        logWriter?.write("Terminate VPN connection")
        logWriter?.close()
        logWriter = null

        controller?.kill(false, null)
        controller = null

        scope.cancel()

        setRootState(false)
    }
}
