package kittoku.osc.bus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class LogEntry(
    val timestampEpochMs: Long,
    val tag: String,
    val message: String,
)

internal object LogBus {
    private const val MAX_ENTRIES = 500

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun append(tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), tag, message)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
    }

    fun appendError(tag: String, message: String?) {
        append(tag, message ?: "Unknown error")
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
