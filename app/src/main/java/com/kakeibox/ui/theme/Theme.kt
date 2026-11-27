package com.kakeibox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kakeibox.AppGraph
import com.kakeibox.data.ThemeMode

/** Android 11以前（壁紙の色が使えない端末）の配色 */
val LightColors: ColorScheme = lightColorScheme(primary = Color(0xFF00897B), secondary = Color(0xFF5D8A85))
val DarkColors: ColorScheme = darkColorScheme(primary = Color(0xFF4DB6AC), secondary = Color(0xFF9CCBC5))

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

/** Android 12以降は壁紙の色（Material You）、それ以前はティールの配色 */
@Composable
fun KakeiboxTheme(darkTheme: Boolean, dynamic: Boolean = true, content: @Composable () -> Unit) {
    val colors = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
