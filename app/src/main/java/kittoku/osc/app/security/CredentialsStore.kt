package kittoku.osc.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject


class CredentialsStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "sstp_vpn_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences("sstp_vpn_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    data class SelectedCredentials(
        val hostname: String,
        val port: Int,
        val username: String,
        val password: String,
        val displayName: String,
        val countryShort: String,
        val isCustom: Boolean,
    )

    fun saveSelection(
        hostname: String,
        port: Int,
        username: String,
        password: String,
        displayName: String,
        countryShort: String,
        isCustom: Boolean,
    ) {
        prefs.edit()
            .putString(KEY_HOSTNAME, hostname)
            .putInt(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_COUNTRY, countryShort)
            .putBoolean(KEY_IS_CUSTOM, isCustom)
            .apply()
    }

    fun clearSelection() {
        prefs.edit().clear().apply()
    }

    fun getSelected(): SelectedCredentials? {
        val hostname = prefs.getString(KEY_HOSTNAME, null) ?: return null

        return SelectedCredentials(
            hostname = hostname,
            port = prefs.getInt(KEY_PORT, 443),
            username = prefs.getString(KEY_USERNAME, "").orEmpty(),
            password = prefs.getString(KEY_PASSWORD, "").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            countryShort = prefs.getString(KEY_COUNTRY, "").orEmpty(),
            isCustom = prefs.getBoolean(KEY_IS_CUSTOM, false),
        )
    }

    fun saveProfileCredentials(profileId: Long, username: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        prefs.edit().putString("$KEY_PROFILE_PREFIX$profileId", json.toString()).apply()
    }

    fun getProfileCredentials(profileId: Long): Pair<String, String>? {
        val raw = prefs.getString("$KEY_PROFILE_PREFIX$profileId", null) ?: return null

        return try {
            val json = JSONObject(raw)
            json.getString("username") to json.getString("password")
        } catch (_: Exception) {
            null
        }
    }

    fun deleteProfileCredentials(profileId: Long) {
        prefs.edit().remove("$KEY_PROFILE_PREFIX$profileId").apply()
    }

    private companion object {
        const val KEY_HOSTNAME = "sel.hostname"
        const val KEY_PORT = "sel.port"
        const val KEY_USERNAME = "sel.username"
        const val KEY_PASSWORD = "sel.password"
        const val KEY_DISPLAY_NAME = "sel.display_name"
        const val KEY_IS_CUSTOM = "sel.is_custom"
        const val KEY_COUNTRY = "sel.country"
        const val KEY_PROFILE_PREFIX = "profile."
    }
}
