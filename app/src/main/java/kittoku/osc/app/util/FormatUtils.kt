package kittoku.osc.app.util

fun flagEmoji(countryCode: String): String {
    val code = countryCode.uppercase().take(2)
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return "\uD83C\uDF10"

    return code.map { Character.toChars(0x1F1E6 + (it - 'A')) }
        .joinToString("") { chars -> String(chars) }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000 -> "%.0f KB/s".format(bytesPerSecond / 1_000.0)
        else -> "$bytesPerSecond B/s"
    }
}

fun formatBitsPerSecond(bitsPerSecond: Long): String {
    return when {
        bitsPerSecond >= 1_000_000_000 -> "%.2f Gbps".format(bitsPerSecond / 1_000_000_000.0)
        bitsPerSecond >= 1_000_000 -> "%.0f Mbps".format(bitsPerSecond / 1_000_000.0)
        bitsPerSecond >= 1_000 -> "%.0f Kbps".format(bitsPerSecond / 1_000.0)
        else -> "$bitsPerSecond bps"
    }
}

fun formatUptime(seconds: Long): String {
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
