package kittoku.osc.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.osc.app.connection.VpnConnector
import kittoku.osc.app.security.CredentialsStore
import kittoku.osc.bus.ConnectionBus
import kittoku.osc.bus.ConnectionSnapshot
import kittoku.osc.bus.StatsBus
import kittoku.osc.bus.TrafficSnapshot
import kittoku.osc.bus.VpnState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


sealed interface HomeEvent {
    data object NeedVpnPermission : HomeEvent
    data object NeedServerSelection : HomeEvent
    data class ConnectFailed(val reason: String) : HomeEvent
}

class HomeViewModel(
    val connector: VpnConnector,
    private val credentialsStore: CredentialsStore,
) : ViewModel() {

    val connection: StateFlow<ConnectionSnapshot> = ConnectionBus.snapshot

    val traffic: StateFlow<TrafficSnapshot> = StatsBus.snapshot

    private val _selectedServer = MutableStateFlow<CredentialsStore.SelectedCredentials?>(null)
    val selectedServer: StateFlow<CredentialsStore.SelectedCredentials?> = _selectedServer.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()

    init {
        refreshSelectedServer()

        viewModelScope.launch {
            while (isActive) {
                if (ConnectionBus.state == VpnState.CONNECTED) {
                    val startedAt = ConnectionBus.snapshot.value.connectedAtEpochMs
                    if (startedAt != null) {
                        _elapsedMs.value = System.currentTimeMillis() - startedAt
                    }
                } else {
                    _elapsedMs.value = 0
                }
                delay(1_000)
            }
        }
    }

    fun refreshSelectedServer() {
        _selectedServer.value = credentialsStore.getSelected()
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun toggleConnection() {
        when (ConnectionBus.state) {
            VpnState.DISCONNECTED, VpnState.ERROR -> connect()
            VpnState.CONNECTED, VpnState.CONNECTING -> disconnect()
            VpnState.DISCONNECTING -> Unit
        }
    }

    fun connect() {
        val selected = credentialsStore.getSelected()
        if (selected == null) {
            _events.value = HomeEvent.NeedServerSelection
            return
        }

        if (!connector.isPrepared()) {
            _events.value = HomeEvent.NeedVpnPermission
            return
        }

        startConnection()
    }

    fun onVpnPermissionGranted() {
        startConnection()
    }

    private fun startConnection() {
        val selected = credentialsStore.getSelected() ?: run {
            _events.value = HomeEvent.NeedServerSelection
            return
        }

        val started = connector.connect(
            hostname = selected.hostname,
            port = selected.port,
            username = selected.username,
            password = selected.password,
            killSwitchEnabled = false,
        )

        if (!started) {
            _events.value = HomeEvent.ConnectFailed("VPN permission was not granted")
        }
    }

    fun disconnect() {
        connector.disconnect()
    }
}
