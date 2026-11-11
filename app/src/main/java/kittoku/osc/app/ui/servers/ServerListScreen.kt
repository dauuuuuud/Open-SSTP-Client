package kittoku.osc.app.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kittoku.osc.R
import kittoku.osc.app.data.model.Server
import kittoku.osc.app.ui.components.pingColor
import kittoku.osc.app.util.ServerSortMode
import kittoku.osc.app.util.flagEmoji
import kittoku.osc.app.util.formatBitsPerSecond
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerListScreen(
    viewModel: ServerListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var countryMenuOpen by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4 && listState.layoutInfo.totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_custom_server))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_servers_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filter.favoritesOnly,
                        onClick = { viewModel.setFavoritesOnly(!state.filter.favoritesOnly) },
                        label = { Text(stringResource(R.string.filter_favorites)) },
                    )

                    Box {
                        FilterChip(
                            selected = state.filter.sortMode != ServerSortMode.SCORE,
                            onClick = { sortMenuOpen = true },
                            label = {
                                Text(
                                    when (state.filter.sortMode) {
                                        ServerSortMode.SCORE -> stringResource(R.string.sort_by)
                                        ServerSortMode.PING -> stringResource(R.string.sort_ping)
                                        ServerSortMode.SPEED -> stringResource(R.string.sort_speed)
                                        ServerSortMode.SESSIONS -> stringResource(R.string.sort_sessions)
                                    }
                                )
                            },
                        )
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_score)) },
                                onClick = { viewModel.setSortMode(ServerSortMode.SCORE); sortMenuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_ping)) },
                                onClick = { viewModel.setSortMode(ServerSortMode.PING); sortMenuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_speed)) },
                                onClick = { viewModel.setSortMode(ServerSortMode.SPEED); sortMenuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_sessions)) },
                                onClick = { viewModel.setSortMode(ServerSortMode.SESSIONS); sortMenuOpen = false },
                            )
                        }
                    }

                    Box {
                        FilterChip(
                            selected = state.filter.country != null,
                            onClick = { countryMenuOpen = true },
                            label = {
                                Text(
                                    state.filter.country
                                        ?: stringResource(R.string.filter_country)
                                )
                            },
                        )
                        DropdownMenu(expanded = countryMenuOpen, onDismissRequest = { countryMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_all_countries)) },
                                onClick = { viewModel.setCountry(null); countryMenuOpen = false },
                            )
                            state.countries.forEach { (code, count) ->
                                DropdownMenuItem(
                                    text = { Text("${flagEmoji(code)} $code ($count)") },
                                    onClick = { viewModel.setCountry(code); countryMenuOpen = false },
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.server_count, state.totalCount),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.error?.let { error ->
                    Text(
                        text = stringResource(R.string.load_failed, error),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (state.loading && state.visibleServers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.visibleServers.size, key = { state.visibleServers[it].hostname }) { index ->
                            val server = state.visibleServers[index]
                            ServerCard(
                                server = server,
                                isSelected = server.hostname == state.selectedHostname,
                                onConnect = { viewModel.select(server) },
                                onToggleFavorite = { viewModel.toggleFavorite(server) },
                                onDelete = if (server.isCustom) {
                                    { viewModel.deleteCustomServer(server) }
                                } else null,
                            )
                        }

                        if (state.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, host, port, user, pass ->
                viewModel.addCustomServer(name, host, port, user, pass)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    onConnect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Card(
        onClick = onConnect,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (server.isCustom) "\uD83D\uDDA5" else flagEmoji(server.countryShort),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.width(44.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (server.isCustom) server.countryLong else server.hostname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (server.isCustom) {
                        "${server.hostname}:${server.port}"
                    } else {
                        "${server.ip}:${server.port} - ${server.countryLong}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!server.isCustom) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill(text = "${server.ping} ms", color = pingColor(server.ping))
                        MetricPill(text = formatBitsPerSecond(server.speedBps), color = MaterialTheme.colorScheme.primary)
                        MetricPill(text = stringResource(R.string.sessions_n, server.sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.toggle_favorite),
                    tint = if (server.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_server))
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MetricPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, host: String, port: Int, user: String, pass: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    val valid = host.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_server)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(R.string.field_host)) }, singleLine = true)
                OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.field_port)) }, singleLine = true)
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text(stringResource(R.string.field_username)) }, singleLine = true)
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text(stringResource(R.string.field_password)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name.ifBlank { host }, host, port.toInt(), user, pass) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
