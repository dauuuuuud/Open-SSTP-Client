package kittoku.osc.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kittoku.osc.R
import kittoku.osc.app.ui.components.StatTile
import kittoku.osc.app.util.ErrorMessages
import kittoku.osc.app.util.flagEmoji
import kittoku.osc.app.util.formatBytes
import kittoku.osc.app.util.formatDuration
import kittoku.osc.app.util.formatSpeed
import kittoku.osc.bus.VpnState
import org.koin.androidx.compose.koinViewModel


@Composable
fun HomeScreen(
    onOpenServers: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val connection by viewModel.connection.collectAsState()
    val traffic by viewModel.traffic.collectAsState()
    val selected by viewModel.selectedServer.collectAsState()
    val elapsed by viewModel.elapsedMs.collectAsState()
    val event by viewModel.events.collectAsState()

    val snackbar = remember { SnackbarHostState() }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        }
    }

    LaunchedEffect(event) {
        when (val current = event) {
            is HomeEvent.NeedVpnPermission -> {
                viewModel.connector.prepareIntent()?.let { vpnPermissionLauncher.launch(it) }
                viewModel.consumeEvent()
            }
            is HomeEvent.NeedServerSelection -> {
                snackbar.showSnackbar("Select a server first")
                viewModel.consumeEvent()
            }
            is HomeEvent.ConnectFailed -> {
                snackbar.showSnackbar(current.reason)
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusHeader(connection.state)

            Spacer(modifier = Modifier.height(28.dp))

            ConnectButton(state = connection.state, onClick = viewModel::toggleConnection)

            Spacer(modifier = Modifier.height(24.dp))

            if (connection.state == VpnState.CONNECTED) {
                Text(
                    text = formatDuration(elapsed),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (connection.state == VpnState.ERROR) {
                ErrorCard(message = ErrorMessages.friendlyMessage(connection.errorMessage))
                Spacer(modifier = Modifier.height(16.dp))
            }

            SelectedServerCard(
                name = selected?.displayName,
                hostname = selected?.hostname,
                countryShort = selected?.countryShort,
                onOpenServers = onOpenServers,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stat_download),
                    value = formatSpeed(traffic.downloadBytesPerSecond),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stat_upload),
                    value = formatSpeed(traffic.uploadBytesPerSecond),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stat_total_down),
                    value = formatBytes(traffic.totalDownloadBytes),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stat_total_up),
                    value = formatBytes(traffic.totalUploadBytes),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun StatusHeader(state: VpnState) {
    val (label, color) = when (state) {
        VpnState.CONNECTED -> stringResource(R.string.state_connected) to MaterialTheme.colorScheme.secondary
        VpnState.CONNECTING -> stringResource(R.string.state_connecting) to MaterialTheme.colorScheme.tertiary
        VpnState.DISCONNECTING -> stringResource(R.string.state_disconnecting) to MaterialTheme.colorScheme.tertiary
        VpnState.ERROR -> stringResource(R.string.state_error) to MaterialTheme.colorScheme.error
        VpnState.DISCONNECTED -> stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ConnectButton(state: VpnState, onClick: () -> Unit) {
    val pressed = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        .collectIsPressedAsState()

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )

    val isActive = state == VpnState.CONNECTED
    val isBusy = state == VpnState.CONNECTING || state == VpnState.DISCONNECTING

    val baseColor = when {
        state == VpnState.ERROR -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isBusy || isActive) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(baseColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
        }

        Surface(
            modifier = Modifier
                .size(160.dp)
                .scale(if (pressed.value) 0.96f else 1f)
                .clip(CircleShape),
            shape = CircleShape,
            color = baseColor,
            shadowElevation = 8.dp,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isActive) {
                                stringResource(R.string.action_disconnect)
                            } else {
                                stringResource(R.string.action_connect)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SelectedServerCard(
    name: String?,
    hostname: String?,
    countryShort: String?,
    onOpenServers: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onOpenServers,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = flagEmoji(countryShort.orEmpty()),
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name ?: stringResource(R.string.no_server_selected),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hostname ?: stringResource(R.string.tap_to_choose_server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onOpenServers) {
                Text(text = stringResource(R.string.action_change))
            }
        }
    }
}
