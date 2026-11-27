package com.kakeibox.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import com.kakeibox.ui.theme.DarkColors
import com.kakeibox.ui.theme.LightColors
import com.kakeibox.ui.theme.Palette
import com.kakeibox.ui.theme.darken

/** ウィジェットの面の色はアプリと同じパレットの配色（端末のライト／ダークに追従） */
val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/**
 * ウィジェットの文字・バーの色。ホーム画面の上でも読みやすいよう、ダークでは白と明るい赤などを使う。
 * （アプリ内の ExpenseColor などは MaterialTheme でライト／ダークを判定するため、ウィジェットでは使わない）
 */
object WidgetText {
    /** 金額・名前など */
    val main: ColorProvider = ColorProvider(day = Color(0xFF2B2326), night = Color.White)
    /** 補足（/ 2,400・月名） */
    val sub: ColorProvider = ColorProvider(day = Color(0xFF5C5157), night = Color.White.copy(alpha = 0.72f))
    /** 超過（マイナス） */
    val over: ColorProvider = ColorProvider(day = Color(0xFFC62828), night = Color(0xFFFF6B6B))
    /** バー：80%超 */
    val warn: ColorProvider = ColorProvider(day = Color(0xFFE0A100), night = Color(0xFFFFD54F))
    /** バー：通常 */
    val ok: ColorProvider = ColorProvider(day = Palette.Teal.darken(0.1f), night = Color(0xFF7FD1C1))
    /** バーの下地 */
    val track: ColorProvider = ColorProvider(day = Color.Black.copy(alpha = 0.12f), night = Color.White.copy(alpha = 0.2f))

    /** 使用率に応じたバーの色 */
    fun bar(ratio: Float): ColorProvider = when {
        ratio >= 1f -> over
        ratio >= 0.8f -> warn
        else -> ok
    }
}

/** 縦1マス（コンパクト）と2マス以上の切り替え */
val WIDGET_SMALL = DpSize(110.dp, 40.dp)
val WIDGET_TALL = DpSize(110.dp, 90.dp)

/** 高さが足りないときはコンパクトな配置にする */
fun DpSize.isCompact(): Boolean = height < WIDGET_TALL.height
