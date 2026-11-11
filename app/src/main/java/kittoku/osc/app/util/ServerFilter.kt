package kittoku.osc.app.util

import kittoku.osc.app.data.model.Server

enum class ServerSortMode(val value: String) {
    SCORE("score"),
    PING("ping"),
    SPEED("speed"),
    SESSIONS("sessions"),
}

data class ServerFilter(
    val query: String = "",
    val country: String? = null,
    val favoritesOnly: Boolean = false,
    val sortMode: ServerSortMode = ServerSortMode.SCORE,
)

fun applyServerFilter(
    servers: List<Server>,
    filter: ServerFilter,
): List<Server> {
    val query = filter.query.trim()

    val filtered = servers.filter { server ->
        val matchesQuery = query.isEmpty() || server.run {
            hostname.contains(query, ignoreCase = true) ||
                countryLong.contains(query, ignoreCase = true) ||
                countryShort.contains(query, ignoreCase = true) ||
                ip.contains(query, ignoreCase = true)
        }
        val matchesCountry = filter.country == null ||
            server.countryShort.equals(filter.country, ignoreCase = true)
        val matchesFavorites = !filter.favoritesOnly || server.isFavorite

        matchesQuery && matchesCountry && matchesFavorites
    }

    return when (filter.sortMode) {
        ServerSortMode.SCORE -> filtered.sortedByDescending { it.score }
        ServerSortMode.PING -> filtered.sortedWith(
            compareBy<Server> { it.ping <= 0 }.thenBy { it.ping }
        )
        ServerSortMode.SPEED -> filtered.sortedByDescending { it.speedBps }
        ServerSortMode.SESSIONS -> filtered.sortedByDescending { it.sessions }
    }
}

fun availableCountries(servers: List<Server>): List<Pair<String, Int>> {
    return servers
        .filter { it.countryShort.isNotBlank() }
        .groupBy { it.countryShort }
        .map { (code, list) -> code to list.size }
        .sortedBy { it.first }
}

class LazyServerPager(
    private val initialBatch: Int = 20,
    private val nextBatch: Int = 15,
) {
    var visibleCount = 0
        private set

    fun reset(totalAvailable: Int) {
        visibleCount = if (totalAvailable == 0) 0 else minOf(initialBatch, totalAvailable)
    }

    fun hasMore(totalAvailable: Int): Boolean = visibleCount < totalAvailable

    fun advance(totalAvailable: Int) {
        if (!hasMore(totalAvailable)) return
        visibleCount = minOf(visibleCount + nextBatch, totalAvailable)
    }
}
