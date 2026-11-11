package kittoku.osc.app

import kittoku.osc.app.util.ErrorMessages
import kittoku.osc.app.util.flagEmoji
import kittoku.osc.app.util.formatBitsPerSecond
import kittoku.osc.app.util.formatBytes
import kittoku.osc.app.util.formatDuration
import kittoku.osc.app.util.formatSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class FormatUtilsTest {

    @Test
    fun `flag emoji maps country codes to regional indicators`() {
        assertEquals("\uD83C\uDDEF\uD83C\uDDF5", flagEmoji("JP"))
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", flagEmoji("de"))
        assertEquals("\uD83C\uDF10", flagEmoji(""))
        assertEquals("\uD83C\uDF10", flagEmoji("1"))
    }

    @Test
    fun `byte and speed formatters use human units`() {
        assertEquals("999 B", formatBytes(999))
        assertEquals("1.0 KB", formatBytes(1_000))
        assertEquals("1.5 MB", formatBytes(1_500_000))
        assertEquals("2.00 GB", formatBytes(2_000_000_000))

        assertEquals("500 B/s", formatSpeed(500))
        assertEquals("1.5 MB/s", formatSpeed(1_500_000))

        assertEquals("1 Kbps", formatBitsPerSecond(1_000))
        assertEquals("1 Mbps", formatBitsPerSecond(1_000_000))
    }

    @Test
    fun `duration is zero padded`() {
        assertEquals("00:00:59", formatDuration(59_000))
        assertEquals("01:01:01", formatDuration(3_661_000))
    }

    @Test
    fun `error messages are friendly and never blank`() {
        assertTrue(ErrorMessages.friendlyMessage("SSL: ERR_TIMEOUT").contains("respond"))
        assertTrue(ErrorMessages.friendlyMessage("CHAP: ERR_AUTHENTICATION_FAILED").contains("Authentication"))
        assertTrue(ErrorMessages.friendlyMessage(null).isNotBlank())
        assertTrue(ErrorMessages.friendlyMessage("").isNotBlank())
    }
}
