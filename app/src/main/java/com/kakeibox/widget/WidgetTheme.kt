package com.kakeibox.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider


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
    val ok: ColorProvider = ColorProvider(day = Color(0xFF00897B), night = Color(0xFF7FD1C1))
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

/** 今日の予算ウィジェットの配置 */
enum class BudgetWidgetLayout {
    /** 縦1マス（2〜3マス幅）：アイコン・金額・＋ を詰めて1行 */
    ROW,
    /** 縦1マス（4マス幅以上）：アイコンは左端、金額は右寄せ */
    ROW_WIDE,
    /** 横長の2マス以上（3×2・4×2 など）：名前・金額・バー */
    CARD,
    /** 正方形に近い（2×2・3×3）：ドーナツ */
    DONUT,
    /** 縦長（2×3）：名前の行＋ドーナツ */
    DONUT_LABELED,
}

/**
 * ウィジェットの実寸から配置を決める。
 * ランチャーによって1マスの実寸が違うので、マス数ではなく高さと縦横比で判定する。
 */
fun budgetWidgetLayout(size: DpSize): BudgetWidgetLayout = when {
    size.height < WIDGET_TALL.height ->
        if (size.width >= 250.dp) BudgetWidgetLayout.ROW_WIDE else BudgetWidgetLayout.ROW
    size.height >= 100.dp && size.width < size.height * 1.5f ->
        if (size.height >= size.width * 1.3f) BudgetWidgetLayout.DONUT_LABELED else BudgetWidgetLayout.DONUT
    else -> BudgetWidgetLayout.CARD
}
