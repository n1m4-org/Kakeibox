package com.kakeibox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** 黒に近づける */
fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount)

/** 白に近づける */
fun Color.lighten(amount: Float): Color = lerp(this, Color.White, amount)

/** 収入の色（ライトは濃い緑、ダークは明るい緑） */
val IncomeColor: Color
    @Composable @ReadOnlyComposable get() = if (isDarkScheme()) Color(0xFF81C784) else Color(0xFF2E7D32)

/** 支出・予算超過の色（ライトは濃い赤、ダークは明るい赤） */
val ExpenseColor: Color
    @Composable @ReadOnlyComposable get() = if (isDarkScheme()) Color(0xFFEF9A9A) else Color(0xFFC62828)

/** 予算の80%を超えたときの色 */
val WarningColor: Color
    @Composable @ReadOnlyComposable get() = if (isDarkScheme()) Color(0xFFFFD54F) else Color(0xFFF9A825)
