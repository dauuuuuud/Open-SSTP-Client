package kittoku.osc.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import kittoku.osc.app.data.prefs.AppPrefs
import kittoku.osc.app.data.prefs.ThemeMode
import kittoku.osc.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOL_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOl_PAP
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setSetPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class SettingsUiState(
    val customDnsEnabled: Boolean = false,
    val customDnsAddress: String = "",
    val ipv6Enabled: Boolean = false,
    val blockIpv6: Boolean = true,
    val verifyCertificate: Boolean = true,
    val cipherSuiteSelectionEnabled: Boolean = false,
    val selectedCipherSuites: Set<String> = emptySet(),
    val reconnectionEnabled: Boolean = false,
    val reconnectionCount: Int = 3,
    val reconnectionInterval: Int = 10,
    val authProtocols: Set<String> = setOf(AUTH_PROTOCOl_PAP, AUTH_PROTOCOL_MSCHAPv2),
    val killSwitchEnabled: Boolean = false,
    val autoConnect: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultUsername: String = AppPrefs.DEFAULT_USERNAME,
    val defaultPassword: String = AppPrefs.DEFAULT_PASSWORD,
    val showConnectionLog: Boolean = true,
)

val KNOWN_CIPHER_SUITES = listOf(
    "TLS_AES_128_GCM_SHA256",
    "TLS_AES_256_GCM_SHA384",
    "TLS_CHACHA20_POLY1305_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
    "TLS_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_RSA_WITH_AES_256_GCM_SHA384",
)

class SettingsViewModel(
    private val context: Context,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadState(): SettingsUiState {
        return SettingsUiState(
            customDnsEnabled = getBooleanPrefValue(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER, prefs),
            customDnsAddress = getStringPrefValue(OscPrefKey.DNS_CUSTOM_ADDRESS, prefs),
            ipv6Enabled = getBooleanPrefValue(OscPrefKey.PPP_IPv6_ENABLED, prefs),
            blockIpv6 = getBooleanPrefValue(OscPrefKey.BLOCK_IPV6, prefs),
            verifyCertificate = getBooleanPrefValue(OscPrefKey.SSL_DO_VERIFY, prefs),
            cipherSuiteSelectionEnabled = getBooleanPrefValue(OscPrefKey.SSL_DO_SELECT_SUITES, prefs),
            selectedCipherSuites = getSetPrefValue(OscPrefKey.SSL_SUITES, prefs),
            reconnectionEnabled = getBooleanPrefValue(OscPrefKey.RECONNECTION_ENABLED, prefs),
            reconnectionCount = getIntPrefValue(OscPrefKey.RECONNECTION_COUNT, prefs),
            reconnectionInterval = getIntPrefValue(OscPrefKey.RECONNECTION_INTERVAL, prefs),
            authProtocols = getSetPrefValue(OscPrefKey.PPP_AUTH_PROTOCOLS, prefs),
            killSwitchEnabled = getBooleanPrefValue(OscPrefKey.KILL_SWITCH_ENABLED, prefs),
            autoConnect = getBooleanPrefValue(OscPrefKey.AUTO_CONNECT, prefs),
            themeMode = appPrefs.themeMode,
            defaultUsername = appPrefs.defaultUsername,
            defaultPassword = appPrefs.defaultPassword,
            showConnectionLog = appPrefs.showConnectionLog,
        )
    }

    private fun update(transform: (SettingsUiState) -> SettingsUiState) {
        _state.update(transform)
    }

    fun setCustomDnsEnabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.DNS_DO_USE_CUSTOM_SERVER, prefs)
        update { it.copy(customDnsEnabled = enabled) }
    }

    fun setCustomDnsAddress(address: String) {
        setStringPrefValue(address.trim(), OscPrefKey.DNS_CUSTOM_ADDRESS, prefs)
        update { it.copy(customDnsAddress = address.trim()) }
    }

    fun setIpv6Enabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.PPP_IPv6_ENABLED, prefs)
        update { it.copy(ipv6Enabled = enabled) }
    }

    fun setBlockIpv6(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.BLOCK_IPV6, prefs)
        update { it.copy(blockIpv6 = enabled) }
    }

    fun setVerifyCertificate(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.SSL_DO_VERIFY, prefs)
        update { it.copy(verifyCertificate = enabled) }
    }

    fun setCipherSuiteSelectionEnabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.SSL_DO_SELECT_SUITES, prefs)
        update { it.copy(cipherSuiteSelectionEnabled = enabled) }
    }

    fun toggleCipherSuite(suite: String) {
        val current = _state.value.selectedCipherSuites
        val next = if (suite in current) current - suite else current + suite

        setSetPrefValue(next, OscPrefKey.SSL_SUITES, prefs)
        update { it.copy(selectedCipherSuites = next) }
    }

    fun setReconnectionEnabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.RECONNECTION_ENABLED, prefs)
        update { it.copy(reconnectionEnabled = enabled) }
    }

    fun setReconnectionCount(count: Int) {
        val valid = count.coerceAtLeast(1)
        setIntPrefValue(valid, OscPrefKey.RECONNECTION_COUNT, prefs)
        update { it.copy(reconnectionCount = valid) }
    }

    fun setReconnectionInterval(seconds: Int) {
        val valid = seconds.coerceAtLeast(1)
        setIntPrefValue(valid, OscPrefKey.RECONNECTION_INTERVAL, prefs)
        update { it.copy(reconnectionInterval = valid) }
    }

    fun toggleAuthProtocol(protocol: String) {
        val current = _state.value.authProtocols
        val next = if (protocol in current) current - protocol else current + protocol
        if (next.isEmpty()) return

        setSetPrefValue(next, OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
        update { it.copy(authProtocols = next) }
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.KILL_SWITCH_ENABLED, prefs)
        update { it.copy(killSwitchEnabled = enabled) }
    }

    fun setAutoConnect(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.AUTO_CONNECT, prefs)
        update { it.copy(autoConnect = enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        appPrefs.themeMode = mode
        update { it.copy(themeMode = mode) }
    }

    fun setDefaultCredentials(username: String, password: String) {
        appPrefs.defaultUsername = username
        appPrefs.defaultPassword = password
        update { it.copy(defaultUsername = username, defaultPassword = password) }
    }

    fun setShowConnectionLog(show: Boolean) {
        appPrefs.showConnectionLog = show
        update { it.copy(showConnectionLog = show) }
    }
}
