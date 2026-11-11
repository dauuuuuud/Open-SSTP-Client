package kittoku.osc.app

import kittoku.osc.app.data.model.Server
import kittoku.osc.app.util.LazyServerPager
import kittoku.osc.app.util.ServerFilter
import kittoku.osc.app.util.ServerSortMode
import kittoku.osc.app.util.applyServerFilter
import kittoku.osc.app.util.availableCountries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class ServerFilterTest {

    private fun server(
        hostname: String,
        country: String = "JP",
        ping: Int = 50,
        speed: Long = 1_000_000,
        score: Long = 100,
        sessions: Int = 10,
        favorite: Boolean = false,
    ) = Server(
        hostname = hostname,
        ip = "1.2.3.4",
        port = 443,
        countryLong = country,
        countryShort = country,
        ping = ping,
        speedBps = speed,
        score = score,
        sessions = sessions,
        uptimeSeconds = 1000,
        isFavorite = favorite,
    )

    private val servers = listOf(
        server("a.opengw.net", country = "JP", ping = 20, score = 300, speed = 5_000_000),
        server("b.opengw.net", country = "US", ping = 150, score = 200, speed = 9_000_000),
        server("c.opengw.net", country = "DE", ping = 400, score = 100, speed = 1_000_000, favorite = true),
    )

    @Test
    fun `query matches hostname and country`() {
        val result = applyServerFilter(servers, ServerFilter(query = "opengw"))

        assertEquals(3, result.size)

        val byCountry = applyServerFilter(servers, ServerFilter(query = "us"))
        assertEquals(1, byCountry.size)
        assertEquals("b.opengw.net", byCountry[0].hostname)
    }

    @Test
    fun `country filter narrows list`() {
        val result = applyServerFilter(servers, ServerFilter(country = "DE"))

        assertEquals(1, result.size)
        assertEquals("c.opengw.net", result[0].hostname)
    }

    @Test
    fun `favorites filter keeps only favorites`() {
        val result = applyServerFilter(servers, ServerFilter(favoritesOnly = true))

        assertEquals(listOf("c.opengw.net"), result.map { it.hostname })
    }

    @Test
    fun `sort by ping puts unknown ping last`() {
        val withUnknown = servers + server("d.opengw.net", ping = -1)

        val result = applyServerFilter(withUnknown, ServerFilter(sortMode = ServerSortMode.PING))

        assertEquals("a.opengw.net", result.first().hostname)
        assertEquals("d.opengw.net", result.last().hostname)
    }

    @Test
    fun `sort by score and speed descending`() {
        assertEquals("a.opengw.net", applyServerFilter(servers, ServerFilter(sortMode = ServerSortMode.SCORE)).first().hostname)
        assertEquals("b.opengw.net", applyServerFilter(servers, ServerFilter(sortMode = ServerSortMode.SPEED)).first().hostname)
    }

    @Test
    fun `countries are deduplicated and sorted`() {
        val countries = availableCountries(servers + server("e.opengw.net", country = "JP"))

        assertEquals(listOf("DE", "JP", "US"), countries.map { it.first })
        assertEquals(2, countries.first { it.first == "JP" }.second)
    }

    @Test
    fun `lazy pager shows first batch then advances incrementally`() {
        val pager = LazyServerPager(initialBatch = 20, nextBatch = 15)
        val total = 78

        pager.reset(total)
        assertEquals(20, pager.visibleCount)
        assertTrue(pager.hasMore(total))

        pager.advance(total)
        assertEquals(35, pager.visibleCount)

        repeat(5) { pager.advance(total) }
        assertEquals(total, pager.visibleCount)
        assertFalse(pager.hasMore(total))

        pager.advance(total)
        assertEquals(total, pager.visibleCount)
    }

    @Test
    fun `lazy pager handles empty list`() {
        val pager = LazyServerPager()
        pager.reset(0)

        assertEquals(0, pager.visibleCount)
        assertFalse(pager.hasMore(0))
    }
}
