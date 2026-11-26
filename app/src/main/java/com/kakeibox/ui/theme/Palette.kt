package com.kakeibox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * アプリのカラーパレット。
 * ここの5色を変えると、アプリ全体（テーマ・背景のグラデーション・収支の色）とウィジェットの色が変わる。
 */
object Palette {
    /** くすんだローズ：支出・アクセント */
    val Rose = Color(0xFFBF808C)
    /** 淡いピンク：背景・面 */
    val Blush = Color(0xFFF2DFEB)
    /** セージ：コンテナ・グラデーション */
    val Sage = Color(0xFF95BFBB)
    /** ティール：メインの色・収入 */
    val Teal = Color(0xFF51A696)
    /** 淡いローズ：警告（予算80%） */
    val Petal = Color(0xFFD9A7A7)
}

/** 黒に近づける（文字のコントラスト確保用） */
fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount)

/** 白に近づける */
fun Color.lighten(amount: Float): Color = lerp(this, Color.White, amount)

/** 収入の色（ライトは濃いめ、ダークは明るめにして読みやすく） */
val IncomeColor: Color
    @Composable @ReadOnlyComposable get() = if (isDarkScheme()) Palette.Sage.lighten(0.15f) else Palette.Teal.darken(0.22f)

/** 支出・予算超過の色 */
val ExpenseColor: Color
    @Composable @ReadOnlyComposable get() = if (isDarkScheme()) Palette.Petal.lighten(0.1f) else Palette.Rose.darken(0.25f)

/** 予算の80%を超えたときの色 */
val WarningColor: Color = Palette.Petal.darken(0.08f)
