package kittoku.osc.bus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR,
}

data class ConnectionSnapshot(
    val state: VpnState = VpnState.DISCONNECTED,
    val errorMessage: String? = null,
    val connectedAtEpochMs: Long? = null,
)

internal object ConnectionBus {
    private val _snapshot = MutableStateFlow(ConnectionSnapshot())
    val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    val state: VpnState
        get() = _snapshot.value.state

    fun setConnecting() {
        _snapshot.value = ConnectionSnapshot(VpnState.CONNECTING, null, null)
    }

    fun setConnected(connectedAtEpochMs: Long) {
        _snapshot.value = ConnectionSnapshot(VpnState.CONNECTED, null, connectedAtEpochMs)
    }

    fun setDisconnecting() {
        _snapshot.value = _snapshot.value.copy(state = VpnState.DISCONNECTING)
    }

    fun setDisconnected() {
        _snapshot.value = ConnectionSnapshot(VpnState.DISCONNECTED, null, null)
    }

    fun setError(message: String?) {
        _snapshot.value = ConnectionSnapshot(VpnState.ERROR, message, null)
    }
}
