package kittoku.osc.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getURIPrefValue


class SaveCertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cert = intent.getByteArrayExtra(EXTRA_KEY_CERT) ?: return
        val filename = intent.getStringExtra(EXTRA_KEY_FILENAME) ?: return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dirURI = getURIPrefValue(OscPrefKey.SSL_CERT_DIR, prefs) ?: return

        val dir = DocumentFile.fromTreeUri(context, dirURI) ?: return
        val file = dir.createFile("application/x-x509-ca-cert", filename) ?: return

        try {
            context.contentResolver.openOutputStream(file.uri)?.use { stream ->
                stream.write(cert)
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val EXTRA_KEY_CERT = "CERT"
        const val EXTRA_KEY_FILENAME = "FILENAME"
    }
}
