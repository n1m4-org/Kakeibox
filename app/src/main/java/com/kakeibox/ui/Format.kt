package com.kakeibox.ui

import java.text.NumberFormat
import com.kakeibox.data.Period
import com.kakeibox.data.monthlyBudget
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val yenFormat = NumberFormat.getNumberInstance(Locale.JAPAN)
private val dayFormat = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN)
private val fullDateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPAN)

fun yen(v: Long): String = "¥" + yenFormat.format(v)
fun signedYen(v: Long): String = (if (v < 0) "-" else "") + yen(kotlin.math.abs(v))
fun LocalDate.dayLabel(): String = format(dayFormat)
fun LocalDate.fullLabel(): String = format(fullDateFormat)

/** 繰り越しなど増減を表す金額（+¥300 / -¥400 / ¥0） */
fun plusMinusYen(v: Long): String = when {
    v > 0 -> "+" + yen(v)
    else -> signedYen(v)
}

/** 「1日 ¥2,000 × 30日 ＝ 今月 ¥60,000」 */
fun monthlyBudgetLabel(daily: Long, period: Period): String =
    "1日 ${yen(daily)} × ${period.days}日 ＝ ${period.shortName()} ${yen(monthlyBudget(daily, period))}"

/** 今日を含む期間なら「今月」、それ以外は期間のラベル */
fun Period.shortName(today: LocalDate = LocalDate.now()): String = if (today in this) "今月" else label
