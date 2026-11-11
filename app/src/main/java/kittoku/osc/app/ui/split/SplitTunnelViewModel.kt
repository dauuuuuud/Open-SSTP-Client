package kittoku.osc.app.ui.split

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import kittoku.osc.preference.LIST_TYPE_ALLOWED
import kittoku.osc.preference.LIST_TYPE_DISALLOWED
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setSetPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.preference.getInstalledAppInfos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class SplitTunnelUiState(
    val enabled: Boolean = false,
    val isAllowedList: Boolean = true,
    val showBackgroundApps: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
)

data class AppEntry(
    val packageName: String,
    val label: String,
)

class SplitTunnelViewModel(private val context: Context) : ViewModel() {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = MutableStateFlow(SplitTunnelUiState())
    val state: StateFlow<SplitTunnelUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val enabled = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)
        val isAllowedList = getStringPrefValue(OscPrefKey.ROUTE_APP_LIST_TYPE, prefs) == LIST_TYPE_ALLOWED
        val showBackground = getBooleanPrefValue(OscPrefKey.ROUTE_DO_SHOW_BACKGROUND_APPS, prefs)
        val selected = getSetPrefValue(OscPrefKey.ROUTE_SELECTED_APPS, prefs)

        _state.update {
            it.copy(
                enabled = enabled,
                isAllowedList = isAllowedList,
                showBackgroundApps = showBackground,
                selectedPackages = selected,
                apps = loadApps(showBackground),
            )
        }
    }

    private fun loadApps(showBackground: Boolean): List<AppEntry> {
        return getInstalledAppInfos(showBackground, context.packageManager)
            .mapNotNull { info ->
                val label = info.loadLabel(context.packageManager).toString()
                if (label.isBlank()) null else AppEntry(info.packageName, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun setEnabled(enabled: Boolean) {
        setBooleanPrefValue(enabled, OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)
        _state.update { it.copy(enabled = enabled) }
    }

    fun setListType(isAllowedList: Boolean) {
        val value = if (isAllowedList) LIST_TYPE_ALLOWED else LIST_TYPE_DISALLOWED
        setStringPrefValue(value, OscPrefKey.ROUTE_APP_LIST_TYPE, prefs)
        _state.update { it.copy(isAllowedList = isAllowedList) }
    }

    fun setShowBackgroundApps(show: Boolean) {
        setBooleanPrefValue(show, OscPrefKey.ROUTE_DO_SHOW_BACKGROUND_APPS, prefs)
        _state.update {
            it.copy(showBackgroundApps = show, apps = loadApps(show))
        }
    }

    fun toggleApp(packageName: String) {
        val current = _state.value.selectedPackages
        val next = if (packageName in current) current - packageName else current + packageName

        setSetPrefValue(next, OscPrefKey.ROUTE_SELECTED_APPS, prefs)
        _state.update { it.copy(selectedPackages = next) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }
}
