package kittoku.osc.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kittoku.osc.R
import kittoku.osc.app.data.prefs.AppPrefs
import kittoku.osc.app.data.prefs.ThemeMode
import kittoku.osc.app.ui.home.HomeScreen
import kittoku.osc.app.ui.logs.LogScreen
import kittoku.osc.app.ui.servers.ServerListScreen
import kittoku.osc.app.ui.settings.SettingsScreen
import kittoku.osc.app.ui.split.SplitTunnelScreen
import kittoku.osc.app.ui.theme.SstpVpnTheme
import org.koin.android.ext.android.inject


class MainActivity : ComponentActivity() {

    private val appPrefs: AppPrefs by inject()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by appPrefs.themeModeFlow.collectAsState()

            SstpVpnTheme(themeMode = themeMode) {
                SstpVpnApp()
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun SstpVpnApp() {
    val navController = rememberNavController()

    val destinations = buildList {
        add(BottomDestination("home", R.string.tab_home, Icons.Filled.Home))
        add(BottomDestination("servers", R.string.tab_servers, Icons.Filled.Public))
        add(BottomDestination("settings", R.string.tab_settings, Icons.Filled.Settings))
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(onOpenServers = { navController.navigate("servers") })
            }
            composable("servers") {
                ServerListScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onOpenSplitTunnel = { navController.navigate("split") },
                    onOpenLog = { navController.navigate("logs") },
                )
            }
            composable("split") {
                SplitTunnelScreen()
            }
            composable("logs") {
                LogScreen()
            }
        }
    }
}
