package com.kakeibox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.kakeibox.AppGraph
import com.kakeibox.data.ThemeMode

/** パレットから作るライトの配色 */
val LightColors: ColorScheme = lightColorScheme(
    primary = Palette.Teal.darken(0.12f),
    onPrimary = Color.White,
    primaryContainer = Palette.Sage.lighten(0.55f),
    onPrimaryContainer = Palette.Teal.darken(0.6f),
    secondary = Palette.Rose.darken(0.1f),
    onSecondary = Color.White,
    secondaryContainer = Palette.Blush,
    onSecondaryContainer = Palette.Rose.darken(0.6f),
    tertiary = Palette.Petal.darken(0.2f),
    tertiaryContainer = Palette.Petal.lighten(0.6f),
    background = Palette.Blush.lighten(0.5f),
    onBackground = Color(0xFF2B2326),
    surface = Palette.Blush.lighten(0.7f),
    onSurface = Color(0xFF2B2326),
    surfaceVariant = lerp(Palette.Blush, Palette.Sage, 0.25f).lighten(0.2f),
    onSurfaceVariant = Color(0xFF5C5157),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Palette.Blush.lighten(0.75f),
    surfaceContainer = Palette.Blush.lighten(0.6f),
    surfaceContainerHigh = Palette.Blush.lighten(0.4f),
    surfaceContainerHighest = Palette.Blush.lighten(0.2f),
    outline = Palette.Rose.lighten(0.2f),
    outlineVariant = Palette.Blush.darken(0.1f),
    error = Palette.Rose.darken(0.35f),
)

/** パレットから作るダークの配色 */
val DarkColors: ColorScheme = darkColorScheme(
    primary = Palette.Sage,
    onPrimary = Palette.Teal.darken(0.7f),
    primaryContainer = Palette.Teal.darken(0.45f),
    onPrimaryContainer = Palette.Sage.lighten(0.6f),
    secondary = Palette.Petal,
    onSecondary = Palette.Rose.darken(0.7f),
    secondaryContainer = Palette.Rose.darken(0.55f),
    onSecondaryContainer = Palette.Blush,
    tertiary = Palette.Blush,
    background = Color(0xFF1A2120),
    onBackground = Color(0xFFEDE3E8),
    surface = Color(0xFF1F2726),
    onSurface = Color(0xFFEDE3E8),
    surfaceVariant = Color(0xFF34403E),
    onSurfaceVariant = Color(0xFFC9BEC4),
    surfaceContainerLowest = Color(0xFF141A19),
    surfaceContainerLow = Color(0xFF1D2524),
    surfaceContainer = Color(0xFF222B2A),
    surfaceContainerHigh = Color(0xFF2B3534),
    surfaceContainerHighest = Color(0xFF34403E),
    outline = Palette.Rose.darken(0.2f),
    outlineVariant = Color(0xFF3E4A48),
    error = Palette.Petal,
)

/** 設定のテーマ（端末に合わせる／ライト／ダーク）を反映したテーマ */
@Composable
fun KakeiboxTheme(content: @Composable () -> Unit) {
    val store = AppGraph.settingsStore(LocalContext.current)
    val settings by store.settings.collectAsState()
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    KakeiboxTheme(darkTheme = dark, content = content)
}

/** パレットの配色（端末の壁紙色＝dynamic color は使わない） */
@Composable
fun KakeiboxTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
