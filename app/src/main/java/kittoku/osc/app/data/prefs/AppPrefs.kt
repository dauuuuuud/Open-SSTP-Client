package kittoku.osc.app.data.prefs

import android.content.Context
import android.content.SharedPreferences


enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sstp_vpn_app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = kotlinx.coroutines.flow.MutableStateFlow(readThemeMode())
    val themeModeFlow: kotlinx.coroutines.flow.StateFlow<ThemeMode> = _themeMode

    var themeMode: ThemeMode
        get() = readThemeMode()
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
            _themeMode.value = value
        }

    private fun readThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    var defaultUsername: String
        get() = prefs.getString(KEY_DEFAULT_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
        set(value) = prefs.edit().putString(KEY_DEFAULT_USERNAME, value).apply()

    var defaultPassword: String
        get() = prefs.getString(KEY_DEFAULT_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        set(value) = prefs.edit().putString(KEY_DEFAULT_PASSWORD, value).apply()

    var showConnectionLog: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOG, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LOG, value).apply()

    companion object {
        const val DEFAULT_USERNAME = "vpn"
        const val DEFAULT_PASSWORD = "vpn"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_DEFAULT_USERNAME = "default_username"
        private const val KEY_DEFAULT_PASSWORD = "default_password"
        private const val KEY_SHOW_LOG = "show_connection_log"
    }
}
