package kittoku.osc.app.data.repo

import android.content.Context
import androidx.preference.PreferenceManager
import kittoku.osc.app.data.api.VpnGateApi
import kittoku.osc.app.data.db.AppDatabase
import kittoku.osc.app.data.db.CustomServerEntity
import kittoku.osc.app.data.mapper.toDomain
import kittoku.osc.app.data.mapper.toEntity
import kittoku.osc.app.data.mapper.toSstpServers
import kittoku.osc.app.data.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap


internal class ServerRepository(
    private val context: Context,
    private val api: VpnGateApi,
    private val db: AppDatabase,
) {
    private val serverDao = db.serverDao()
    private val customServerDao = db.customServerDao()

    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(context)

    private val favoriteHostnames = ConcurrentHashMap.newKeySet<String>()

    val CACHE_TTL_MS = 30 * 60 * 1000L

    init {
        kotlinx.coroutines.runBlocking {
            favoriteHostnames.addAll(
                serverDao.observeAll().first().filter { it.isFavorite }.map { it.hostname }
            )
        }
    }

    fun observeServers(): Flow<List<Server>> {
        return combine(
            serverDao.observeAll(),
            customServerDao.observeAll(),
        ) { cached, custom ->
            custom.map { it.toDomain() } + cached.map { it.toDomain() }
        }
    }

    fun observeFavorites(): Flow<List<Server>> {
        return observeServers().map { list -> list.filter { it.isFavorite } }
    }

    suspend fun cachedServerCount(): Int = serverDao.count()

    fun isCacheFresh(): Boolean {
        val fetchedAt = prefs.getLong(KEY_LAST_FETCHED, 0)
        return fetchedAt > 0 && System.currentTimeMillis() - fetchedAt < CACHE_TTL_MS
    }

    fun lastFetchedAt(): Long = prefs.getLong(KEY_LAST_FETCHED, 0)

    suspend fun refresh(): Result<Int> {
        return try {
            val response = api.fetchServers()
            if (!response.success) {
                return Result.failure(IllegalStateException("Server list request was not successful"))
            }

            val servers = response.toSstpServers()
            val now = System.currentTimeMillis()

            serverDao.upsertAll(
                servers.map { server ->
                    server.toEntity(now).copy(isFavorite = server.hostname in favoriteHostnames)
                }
            )
            serverDao.deleteStale(now - CACHE_TTL_MS)

            prefs.edit().putLong(KEY_LAST_FETCHED, now).apply()

            Result.success(servers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setFavorite(server: Server, favorite: Boolean) {
        if (server.isCustom) {
            server.customId?.let { customServerDao.setFavorite(it, favorite) }
            return
        }

        if (favorite) favoriteHostnames.add(server.hostname) else favoriteHostnames.remove(server.hostname)
        serverDao.setFavorite(server.hostname, favorite)
    }

    suspend fun addCustomServer(name: String, hostname: String, port: Int): Long {
        return customServerDao.insert(
            CustomServerEntity(
                name = name,
                hostname = hostname,
                port = port,
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteCustomServer(id: Long) {
        customServerDao.delete(id)
    }

    suspend fun setCustomFavorite(id: Long, favorite: Boolean) {
        customServerDao.setFavorite(id, favorite)
    }

    companion object {
        private const val KEY_LAST_FETCHED = "app.server.last_fetched"
    }
}
