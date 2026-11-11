package kittoku.osc.app.connection

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.preference.PreferenceManager
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.service.ACTION_VPN_CONNECT
import kittoku.osc.service.ACTION_VPN_DISCONNECT
import kittoku.osc.service.SstpVpnService


class VpnConnector(private val context: Context) {

    fun isPrepared(): Boolean = VpnService.prepare(context) == null

    fun prepareIntent(): Intent? = VpnService.prepare(context)

    fun connect(
        hostname: String,
        port: Int,
        username: String,
        password: String,
        killSwitchEnabled: Boolean,
    ): Boolean {
        if (!isPrepared()) return false

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        setStringPrefValue(hostname, OscPrefKey.HOME_HOSTNAME, prefs)
        setIntPrefValue(port, OscPrefKey.SSL_PORT, prefs)
        setStringPrefValue(username, OscPrefKey.HOME_USERNAME, prefs)
        setStringPrefValue(password, OscPrefKey.HOME_PASSWORD, prefs)

        val intent = Intent(context, SstpVpnService::class.java).setAction(ACTION_VPN_CONNECT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        return true
    }

    fun disconnect() {
        val intent = Intent(context, SstpVpnService::class.java).setAction(ACTION_VPN_DISCONNECT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
