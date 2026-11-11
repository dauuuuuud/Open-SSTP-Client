package kittoku.osc.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.preference.PreferenceManager
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.service.ACTION_VPN_CONNECT
import kittoku.osc.service.SstpVpnService


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!getBooleanPrefValue(OscPrefKey.AUTO_CONNECT, prefs)) return
        if (VpnService.prepare(context) != null) return

        context.startService(
            Intent(context, SstpVpnService::class.java).setAction(ACTION_VPN_CONNECT)
        )
    }
}
