package kittoku.osc.bus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong


data class TrafficSnapshot(
    val downloadBytesPerSecond: Long = 0,
    val uploadBytesPerSecond: Long = 0,
    val totalDownloadBytes: Long = 0,
    val totalUploadBytes: Long = 0,
)

internal object StatsBus {
    private val totalDownload = AtomicLong(0)
    private val totalUpload = AtomicLong(0)

    private var lastDownload = 0L
    private var lastUpload = 0L

    private val _snapshot = MutableStateFlow(TrafficSnapshot())
    val snapshot: StateFlow<TrafficSnapshot> = _snapshot.asStateFlow()

    private var jobSampler: Job? = null

    fun onBytesDownloaded(count: Int) {
        if (count > 0) totalDownload.addAndGet(count.toLong())
    }

    fun onBytesUploaded(count: Int) {
        if (count > 0) totalUpload.addAndGet(count.toLong())
    }

    fun reset() {
        totalDownload.set(0)
        totalUpload.set(0)
        lastDownload = 0
        lastUpload = 0
        _snapshot.value = TrafficSnapshot()
    }

    internal fun startSampling(scope: CoroutineScope) {
        stopSampling()
        jobSampler = scope.launch {
            while (isActive) {
                delay(1_000)
                val currentDownload = totalDownload.get()
                val currentUpload = totalUpload.get()

                _snapshot.value = TrafficSnapshot(
                    downloadBytesPerSecond = (currentDownload - lastDownload).coerceAtLeast(0),
                    uploadBytesPerSecond = (currentUpload - lastUpload).coerceAtLeast(0),
                    totalDownloadBytes = currentDownload,
                    totalUploadBytes = currentUpload,
                )

                lastDownload = currentDownload
                lastUpload = currentUpload
            }
        }
    }

    internal fun stopSampling() {
        jobSampler?.cancel()
        jobSampler = null
    }
}
