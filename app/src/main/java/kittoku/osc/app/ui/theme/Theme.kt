package kittoku.osc.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kittoku.osc.app.data.prefs.ThemeMode


private val LightColors = lightColorScheme(
    primary = DeepBlue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DeepBlue90,
    onPrimaryContainer = OceanNavy,
    secondary = AccentTealDark,
    secondaryContainer = DeepBlue10,
    onSecondaryContainer = OceanNavy,
    background = SurfaceLight,
    onBackground = OceanNavy,
    surface = SurfaceLight,
    onSurface = OceanNavy,
    surfaceVariant = DeepBlue10,
    onSurfaceVariant = NeutralGray,
    outline = NeutralGray,
)

private val DarkColors = darkColorScheme(
    primary = DeepBlue80,
    onPrimary = OceanNavy,
    primaryContainer = DeepBlue60,
    onPrimaryContainer = DeepBlue10,
    secondary = AccentTeal,
    secondaryContainer = OceanNavySoft,
    onSecondaryContainer = DeepBlue20,
    background = SurfaceDark,
    onBackground = DeepBlue20,
    surface = SurfaceDark,
    onSurface = DeepBlue20,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = NeutralGray,
    outline = NeutralGray,
)

@Composable
fun SstpVpnTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SstpTypography,
        content = content,
    )
}
