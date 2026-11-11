package kittoku.osc.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kittoku.osc.R
import kittoku.osc.app.data.prefs.ThemeMode
import kittoku.osc.app.ui.components.LabeledRow
import kittoku.osc.app.ui.components.SectionCard
import org.koin.androidx.compose.koinViewModel


@Composable
fun SettingsScreen(
    onOpenSplitTunnel: () -> Unit = {},
    onOpenLog: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showCipherDialog by remember { mutableStateOf(false) }
    var showCredentialsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            Text(
                text = stringResource(R.string.settings_security),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LabeledRow(
                title = stringResource(R.string.kill_switch),
                subtitle = stringResource(R.string.kill_switch_desc),
            ) {
                Switch(checked = state.killSwitchEnabled, onCheckedChange = viewModel::setKillSwitchEnabled)
            }
            LabeledRow(
                title = stringResource(R.string.verify_certificate),
                subtitle = stringResource(R.string.verify_certificate_desc),
            ) {
                Switch(checked = state.verifyCertificate, onCheckedChange = viewModel::setVerifyCertificate)
            }
            LabeledRow(
                title = stringResource(R.string.auto_connect),
                subtitle = stringResource(R.string.auto_connect_desc),
            ) {
                Switch(checked = state.autoConnect, onCheckedChange = viewModel::setAutoConnect)
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_network),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LabeledRow(
                title = stringResource(R.string.custom_dns),
                subtitle = stringResource(R.string.custom_dns_desc),
            ) {
                Switch(checked = state.customDnsEnabled, onCheckedChange = viewModel::setCustomDnsEnabled)
            }
            if (state.customDnsEnabled) {
                var dns by remember(state.customDnsAddress) { mutableStateOf(state.customDnsAddress) }
                OutlinedTextField(
                    value = dns,
                    onValueChange = { dns = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dns_address)) },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { viewModel.setCustomDnsAddress(dns) }) {
                            Text(stringResource(R.string.action_save))
                        }
                    },
                )
            }
            LabeledRow(
                title = stringResource(R.string.enable_ipv6),
                subtitle = stringResource(R.string.enable_ipv6_desc),
            ) {
                Switch(checked = state.ipv6Enabled, onCheckedChange = viewModel::setIpv6Enabled)
            }
            if (!state.ipv6Enabled) {
                LabeledRow(
                    title = stringResource(R.string.block_ipv6),
                    subtitle = stringResource(R.string.block_ipv6_desc),
                ) {
                    Switch(checked = state.blockIpv6, onCheckedChange = viewModel::setBlockIpv6)
                }
            }
            LabeledRow(
                title = stringResource(R.string.cipher_suites),
                subtitle = stringResource(R.string.cipher_suites_desc),
            ) {
                Switch(
                    checked = state.cipherSuiteSelectionEnabled,
                    onCheckedChange = viewModel::setCipherSuiteSelectionEnabled,
                )
            }
            if (state.cipherSuiteSelectionEnabled) {
                TextButton(onClick = { showCipherDialog = true }) {
                    Text(
                        stringResource(
                            R.string.cipher_selected_count,
                            state.selectedCipherSuites.size,
                        )
                    )
                }
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_authentication),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            listOf("PAP", "MSCHAPv2", "EAP-MSCHAPv2").forEach { protocol ->
                LabeledRow(title = protocol) {
                    Checkbox(
                        checked = protocol in state.authProtocols,
                        onCheckedChange = { viewModel.toggleAuthProtocol(protocol) },
                    )
                }
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_reconnection),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LabeledRow(
                title = stringResource(R.string.auto_reconnect),
                subtitle = stringResource(R.string.auto_reconnect_desc),
            ) {
                Switch(checked = state.reconnectionEnabled, onCheckedChange = viewModel::setReconnectionEnabled)
            }
            if (state.reconnectionEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.reconnectionCount.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setReconnectionCount) },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.retry_count)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.reconnectionInterval.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setReconnectionInterval) },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.retry_interval)) },
                        singleLine = true,
                    )
                }
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ThemeOption(
                label = stringResource(R.string.theme_system),
                selected = state.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            ThemeOption(
                label = stringResource(R.string.theme_light),
                selected = state.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            ThemeOption(
                label = stringResource(R.string.theme_dark),
                selected = state.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_advanced),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onOpenSplitTunnel) {
                Text(stringResource(R.string.split_tunneling))
            }
            TextButton(onClick = onOpenLog) {
                Text(stringResource(R.string.connection_log))
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.settings_defaults),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.default_credentials_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { showCredentialsDialog = true }) {
                Text(stringResource(R.string.edit_default_credentials))
            }
            LabeledRow(
                title = stringResource(R.string.show_connection_log),
                subtitle = stringResource(R.string.show_connection_log_desc),
            ) {
                Switch(checked = state.showConnectionLog, onCheckedChange = viewModel::setShowConnectionLog)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCipherDialog) {
        AlertDialog(
            onDismissRequest = { showCipherDialog = false },
            title = { Text(stringResource(R.string.cipher_suites)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    KNOWN_CIPHER_SUITES.forEach { suite ->
                        LabeledRow(title = suite) {
                            Checkbox(
                                checked = suite in state.selectedCipherSuites,
                                onCheckedChange = { viewModel.toggleCipherSuite(suite) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCipherDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
        )
    }

    if (showCredentialsDialog) {
        var user by remember { mutableStateOf(state.defaultUsername) }
        var pass by remember { mutableStateOf(state.defaultPassword) }

        AlertDialog(
            onDismissRequest = { showCredentialsDialog = false },
            title = { Text(stringResource(R.string.edit_default_credentials)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text(stringResource(R.string.field_username)) }, singleLine = true)
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text(stringResource(R.string.field_password)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDefaultCredentials(user, pass)
                    showCredentialsDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
