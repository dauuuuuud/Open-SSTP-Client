package kittoku.osc.app.ui.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kittoku.osc.R
import kittoku.osc.app.ui.components.LabeledRow
import kittoku.osc.app.ui.components.SectionCard
import org.koin.androidx.compose.koinViewModel


@Composable
fun SplitTunnelScreen(
    viewModel: SplitTunnelViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val filtered = state.apps.filter {
        state.query.isBlank() || it.label.contains(state.query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            LabeledRow(
                title = stringResource(R.string.split_tunneling),
                subtitle = stringResource(R.string.split_tunneling_desc),
            ) {
                androidx.compose.material3.Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
            }

            if (state.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.isAllowedList,
                        onClick = { viewModel.setListType(true) },
                        label = { Text(stringResource(R.string.allowed_apps)) },
                    )
                    FilterChip(
                        selected = !state.isAllowedList,
                        onClick = { viewModel.setListType(false) },
                        label = { Text(stringResource(R.string.disallowed_apps)) },
                    )
                }

                LabeledRow(
                    title = stringResource(R.string.show_background_apps),
                ) {
                    androidx.compose.material3.Switch(
                        checked = state.showBackgroundApps,
                        onCheckedChange = viewModel::setShowBackgroundApps,
                    )
                }
            }
        }

        if (state.enabled) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                singleLine = true,
            )

            Text(
                text = if (state.isAllowedList) {
                    stringResource(R.string.selected_apps_go_through_vpn, state.selectedPackages.size)
                } else {
                    stringResource(R.string.selected_apps_bypass_vpn, state.selectedPackages.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = app.packageName in state.selectedPackages,
                            onCheckedChange = { viewModel.toggleApp(app.packageName) },
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.split_tunneling_disabled_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
