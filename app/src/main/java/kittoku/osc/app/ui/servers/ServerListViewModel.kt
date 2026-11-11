package kittoku.osc.app.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.osc.app.data.model.Server
import kittoku.osc.app.data.prefs.AppPrefs
import kittoku.osc.app.data.repo.ServerRepository
import kittoku.osc.app.security.CredentialsStore
import kittoku.osc.app.util.LazyServerPager
import kittoku.osc.app.util.ServerFilter
import kittoku.osc.app.util.ServerSortMode
import kittoku.osc.app.util.applyServerFilter
import kittoku.osc.app.util.availableCountries
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ServerListUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val servers: List<Server> = emptyList(),
    val visibleServers: List<Server> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val filter: ServerFilter = ServerFilter(),
    val countries: List<Pair<String, Int>> = emptyList(),
    val selectedHostname: String? = null,
    val lastFetchedAt: Long = 0,
)

internal class ServerListViewModel(
    private val repository: ServerRepository,
    private val credentialsStore: CredentialsStore,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerListUiState())
    val state: StateFlow<ServerListUiState> = _state.asStateFlow()

    private val pager = LazyServerPager()

    private var lazyLoadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeServers().collect { servers ->
                val filter = _state.value.filter
                val filtered = applyServerFilter(servers, filter)

                pager.reset(filtered.size)
                _state.update {
                    it.copy(
                        loading = false,
                        servers = servers,
                        totalCount = servers.size,
                        countries = availableCountries(servers),
                        visibleServers = filtered.take(pager.visibleCount),
                        hasMore = pager.hasMore(filtered.size),
                    )
                }
            }
        }

        viewModelScope.launch {
            if (!repository.isCacheFresh() || repository.cachedServerCount() == 0) {
                refresh()
            } else {
                _state.update { it.copy(lastFetchedAt = repository.lastFetchedAt()) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }

            val result = repository.refresh()

            _state.update {
                it.copy(
                    refreshing = false,
                    error = result.exceptionOrNull()?.message,
                    lastFetchedAt = repository.lastFetchedAt(),
                )
            }
        }
    }

    fun setQuery(query: String) = updateFilter { it.copy(query = query) }

    fun setCountry(country: String?) = updateFilter { it.copy(country = country) }

    fun setSortMode(mode: ServerSortMode) = updateFilter { it.copy(sortMode = mode) }

    fun setFavoritesOnly(favoritesOnly: Boolean) = updateFilter { it.copy(favoritesOnly = favoritesOnly) }

    private fun updateFilter(transform: (ServerFilter) -> ServerFilter) {
        val newFilter = transform(_state.value.filter)
        val filtered = applyServerFilter(_state.value.servers, newFilter)

        pager.reset(filtered.size)

        _state.update {
            it.copy(
                filter = newFilter,
                visibleServers = filtered.take(pager.visibleCount),
                hasMore = pager.hasMore(filtered.size),
            )
        }
    }

    fun loadMore() {
        if (lazyLoadJob?.isActive == true) return

        lazyLoadJob = viewModelScope.launch {
            val filter = _state.value.filter
            val filtered = applyServerFilter(_state.value.servers, filter)

            if (!pager.hasMore(filtered.size)) return@launch

            pager.advance(filtered.size)

            _state.update {
                it.copy(
                    visibleServers = filtered.take(pager.visibleCount),
                    hasMore = pager.hasMore(filtered.size),
                )
            }

            delay(60)
        }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch {
            repository.setFavorite(server, !server.isFavorite)
        }
    }

    fun select(server: Server) {
        val (username, password) = if (server.isCustom && server.customId != null) {
            credentialsStore.getProfileCredentials(server.customId) ?: ("" to "")
        } else {
            appPrefs.defaultUsername to appPrefs.defaultPassword
        }

        credentialsStore.saveSelection(
            hostname = server.hostname,
            port = server.port,
            username = username,
            password = password,
            displayName = if (server.isCustom) server.countryLong else server.hostname,
            countryShort = server.countryShort,
            isCustom = server.isCustom,
        )

        _state.update { it.copy(selectedHostname = server.hostname) }
    }

    fun addCustomServer(name: String, hostname: String, port: Int, username: String, password: String) {
        viewModelScope.launch {
            val id = repository.addCustomServer(name, hostname, port)
            credentialsStore.saveProfileCredentials(id, username, password)
        }
    }

    fun deleteCustomServer(server: Server) {
        viewModelScope.launch {
            server.customId?.let {
                repository.deleteCustomServer(it)
                credentialsStore.deleteProfileCredentials(it)
            }
        }
    }
}
