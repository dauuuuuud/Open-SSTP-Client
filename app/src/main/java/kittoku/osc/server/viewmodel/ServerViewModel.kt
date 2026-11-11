package kittoku.osc.server.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kittoku.osc.server.api.RetrofitClient
import kittoku.osc.server.model.ConnectionStatus
import kittoku.osc.server.model.Country
import kittoku.osc.server.model.Server
import kittoku.osc.server.model.ServerWithState
import kittoku.osc.server.repository.ServerRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing server list and connection state
 */
class ServerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "ServerViewModel"
    
    private val repository: ServerRepository
    
    // LiveData for UI observation
    private val _countries = MutableLiveData<List<Country>>()
    val countries: LiveData<List<Country>> = _countries
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus
    
    private val _currentServer = MutableLiveData<ServerWithState?>()
    val currentServer: LiveData<ServerWithState?> = _currentServer
    
    private val _attemptCount = MutableLiveData<Int>()
    val attemptCount: LiveData<Int> = _attemptCount
    
    private val _serversTried = MutableLiveData<List<ServerWithState>>()
    val serversTried: LiveData<List<ServerWithState>> = _serversTried
    
    // Track which servers are being tried
    private val triedServers = mutableListOf<ServerWithState>()
    private var currentServerIndex = 0
    private var isConnecting = false
    
    init {
        repository = ServerRepository(RetrofitClient.serverApiService)
        _connectionStatus.value = ConnectionStatus.IDLE
        _attemptCount.value = 0
    }
    
    /**
     * Fetch servers from API
     */
    fun fetchServers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.fetchServers().onSuccess { countries ->
                _countries.value = countries
                _isLoading.value = false
            }.onFailure { e ->
                _error.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh servers
     */
    fun refreshServers() {
        repository.clearCache()
        fetchServers()
    }
    
    /**
     * Start connecting to servers
     */
    fun startConnection() {
        if (isConnecting) return
        
        val servers = repository.getAllServers()
        if (servers.isEmpty()) {
            _error.value = "No servers available"
            return
        }
        
        isConnecting = true
        triedServers.clear()
        currentServerIndex = 0
        _attemptCount.value = 0
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _serversTried.value = emptyList()
        
        tryNextServer()
    }
    
    /**
     * Stop connection attempts
     */
    fun stopConnection() {
        isConnecting = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _currentServer.value = null
    }
    
    /**
     * Try the next server in the list
     */
    private fun tryNextServer() {
        if (!isConnecting) return
        
        val servers = repository.getAllServers()
        if (currentServerIndex >= servers.size || currentServerIndex >= 5) {
            // Tried 5 servers or no more servers
            _connectionStatus.value = ConnectionStatus.FAILED
            _currentServer.value = null
            isConnecting = false
            return
        }
        
        val server = servers[currentServerIndex]
        val serverWithState = ServerWithState(
            server = server,
            connectionStatus = ConnectionStatus.CONNECTING,
            attemptCount = currentServerIndex + 1
        )
        
        triedServers.add(serverWithState)
        currentServerIndex++
        _currentServer.value = serverWithState
        _attemptCount.value = currentServerIndex
        _serversTried.value = triedServers.toList()
        _connectionStatus.value = ConnectionStatus.CONNECTING
        
        // In a real implementation, this would trigger the actual connection
        // For now, we'll simulate it or let the service handle it
    }
    
    /**
     * Notify that connection failed and try next server
     */
    fun onConnectionFailed(error: String? = null) {
        _currentServer.value?.let { current ->
            val updated = current.copy(
                connectionStatus = ConnectionStatus.FAILED,
                lastError = error
            )
            triedServers[triedServers.lastIndex] = updated
            _serversTried.value = triedServers.toList()
        }
        
        _connectionStatus.value = ConnectionStatus.RETRYING
        
        // Try next server after a short delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // 1 second delay
            tryNextServer()
        }
    }
    
    /**
     * Notify that connection succeeded
     */
    fun onConnectionSuccess() {
        _currentServer.value?.let { current ->
            val updated = current.copy(
                connectionStatus = ConnectionStatus.CONNECTED
            )
            triedServers[triedServers.lastIndex] = updated
            _serversTried.value = triedServers.toList()
            _currentServer.value = updated
        }
        
        _connectionStatus.value = ConnectionStatus.CONNECTED
        isConnecting = false
    }
    
    /**
     * Get servers for a specific country
     */
    fun getServersForCountry(countryCode: String): List<Server> {
        return repository.getServersForCountry(countryCode)
    }
    
    /**
     * Search servers
     */
    fun searchServers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val results = repository.searchServers(query)
            _countries.value = results
            _isLoading.value = false
        }
    }
    
    /**
     * Sort countries by different criteria
     */
    fun sortCountries(sortType: SortType) {
        _countries.value?.let { countries ->
            val sorted = when (sortType) {
                SortType.BY_NAME -> countries.sortedBy { it.name }
                SortType.BY_SERVER_COUNT -> countries.sortedByDescending { it.serverCount }
                SortType.BY_SCORE -> {
                    countries.map { country ->
                        country.copy(
                            servers = country.servers.sortedByDescending { it.score }.toMutableList()
                        )
                    }.sortedBy { it.servers.firstOrNull()?.score ?: 0 }
                }
                SortType.BY_PING -> {
                    countries.map { country ->
                        country.copy(
                            servers = country.servers.sortedBy { it.ping }.toMutableList()
                        )
                    }.sortedBy { it.servers.firstOrNull()?.ping ?: Int.MAX_VALUE }
                }
            }
            _countries.value = sorted
        }
    }
    
    enum class SortType {
        BY_NAME,
        BY_SERVER_COUNT,
        BY_SCORE,
        BY_PING
    }
}
