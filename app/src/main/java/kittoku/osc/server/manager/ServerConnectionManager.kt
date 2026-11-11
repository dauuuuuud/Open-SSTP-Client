package kittoku.osc.server.manager

import android.content.Context
import android.content.SharedPreferences
import android.net.VpnService
import android.util.Log
import androidx.preference.PreferenceManager
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.server.api.RetrofitClient
import kittoku.osc.server.model.ConnectionStatus
import kittoku.osc.server.model.Server
import kittoku.osc.server.repository.ServerRepository
import kittoku.osc.service.SstpVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages connection to multiple SSTP servers with automatic retry
 */
class ServerConnectionManager(
    private val context: Context,
    private val vpnService: SstpVpnService
) {
    
    private val TAG = "ServerConnectionManager"
    
    private val repository: ServerRepository
    private val prefs: SharedPreferences
    
    private var currentServerIndex = 0
    private var maxAttempts = 5
    private var isConnecting = false
    private var retryJob: Job? = null
    
    // Track tried servers
    private val triedServers = mutableListOf<Server>()
    private var currentServer: Server? = null
    
    // Connection status listeners
    private val listeners = mutableListOf<ConnectionListener>()
    
    init {
        repository = ServerRepository(RetrofitClient.serverApiService)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }
    
    /**
     * Start connecting to servers
     */
    fun startConnection() {
        if (isConnecting) return
        
        isConnecting = true
        triedServers.clear()
        currentServerIndex = 0
        
        notifyStatusChanged(ConnectionStatus.CONNECTING)
        
        // Load servers
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.fetchServers().onSuccess { countries ->
                    val allServers = repository.getAllServers()
                    if (allServers.isEmpty()) {
                        notifyError("No servers available")
                        notifyStatusChanged(ConnectionStatus.FAILED)
                        isConnecting = false
                        return@onSuccess
                    }
                    
                    // Try first server
                    tryNextServer(allServers)
                }.onFailure { e ->
                    notifyError(e.message ?: "Failed to fetch servers")
                    notifyStatusChanged(ConnectionStatus.FAILED)
                    isConnecting = false
                }
            } catch (e: Exception) {
                notifyError(e.message ?: "Unknown error")
                notifyStatusChanged(ConnectionStatus.FAILED)
                isConnecting = false
            }
        }
    }
    
    /**
     * Stop connection attempts
     */
    fun stopConnection() {
        isConnecting = false
        retryJob?.cancel()
        retryJob = null
        currentServer = null
        triedServers.clear()
        currentServerIndex = 0
        notifyStatusChanged(ConnectionStatus.DISCONNECTED)
    }
    
    /**
     * Try the next server in the list
     */
    private fun tryNextServer(allServers: List<Server>) {
        if (!isConnecting) return
        
        if (currentServerIndex >= allServers.size || currentServerIndex >= maxAttempts) {
            // No more servers to try
            notifyStatusChanged(ConnectionStatus.FAILED)
            isConnecting = false
            return
        }
        
        val server = allServers[currentServerIndex]
        currentServer = server
        triedServers.add(server)
        currentServerIndex++
        
        notifyServerChanged(server, currentServerIndex)
        notifyStatusChanged(ConnectionStatus.CONNECTING)
        
        // Set server details in preferences
        setServerPreferences(server)
        
        // Prepare VPN service
        VpnService.prepare(context)?.also { intent ->
            // In a real implementation, this would be handled by the activity
            // For now, we'll just start the service
            Log.d(TAG, "VPN preparation required")
        } ?: startVpnService()
    }
    
    /**
     * Set server details in preferences
     */
    private fun setServerPreferences(server: Server) {
        setStringPrefValue(server.sstpHostname, OscPrefKey.HOME_HOSTNAME, prefs)
        setStringPrefValue(server.sstpHostname, OscPrefKey.HOME_HOSTNAME, prefs)
        // Set other preferences as needed
    }
    
    /**
     * Start VPN service
     */
    private fun startVpnService() {
        // This should trigger the connection
        // The actual connection will be handled by SstpVpnService
    }
    
    /**
     * Notify that connection failed and try next server
     */
    fun onConnectionFailed(error: String? = null) {
        if (!isConnecting) return
        
        notifyError(error ?: "Connection failed")
        notifyStatusChanged(ConnectionStatus.RETRYING)
        
        // Wait a bit before retrying
        retryJob?.cancel()
        retryJob = CoroutineScope(Dispatchers.IO).launch {
            delay(2000) // 2 seconds delay
            
            // Get all servers again
            val allServers = repository.getAllServers()
            if (allServers.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    tryNextServer(allServers)
                }
            }
        }
    }
    
    /**
     * Notify that connection succeeded
     */
    fun onConnectionSuccess() {
        isConnecting = false
        retryJob?.cancel()
        retryJob = null
        notifyStatusChanged(ConnectionStatus.CONNECTED)
    }
    
    /**
     * Get current server
     */
    fun getCurrentServer(): Server? = currentServer
    
    /**
     * Get tried servers
     */
    fun getTriedServers(): List<Server> = triedServers.toList()
    
    /**
     * Get attempt count
     */
    fun getAttemptCount(): Int = currentServerIndex
    
    /**
     * Add connection listener
     */
    fun addListener(listener: ConnectionListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove connection listener
     */
    fun removeListener(listener: ConnectionListener) {
        listeners.remove(listener)
    }
    
    /**
     * Notify status changed
     */
    private fun notifyStatusChanged(status: ConnectionStatus) {
        listeners.forEach { it.onStatusChanged(status) }
    }
    
    /**
     * Notify server changed
     */
    private fun notifyServerChanged(server: Server, attempt: Int) {
        listeners.forEach { it.onServerChanged(server, attempt) }
    }
    
    /**
     * Notify error
     */
    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
    
    /**
     * Connection listener interface
     */
    interface ConnectionListener {
        fun onStatusChanged(status: ConnectionStatus)
        fun onServerChanged(server: Server, attempt: Int)
        fun onError(error: String)
    }
}
