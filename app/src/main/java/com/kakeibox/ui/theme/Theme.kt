package com.kakeibox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)
val WarningColor = Color(0xFFF9A825)

private val Light = lightColorScheme(primary = Color(0xFF00897B), secondary = Color(0xFF5D8A85))
private val Dark = darkColorScheme(primary = Color(0xFF4DB6AC), secondary = Color(0xFF9CCBC5))

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

@Composable
fun KakeiboxTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}
