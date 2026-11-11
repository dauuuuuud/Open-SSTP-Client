package kittoku.osc.app.ui.logs

import androidx.lifecycle.ViewModel
import kittoku.osc.bus.LogBus


class LogViewModel : ViewModel() {
    val entries = LogBus.entries

    fun clear() {
        LogBus.clear()
    }
}
