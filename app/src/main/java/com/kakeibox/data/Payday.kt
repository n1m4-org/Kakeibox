package com.kakeibox.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 次の給料日（today を含む）。payday が月の日数より大きい場合は月末。
 * @param shiftWeekend 土日に当たる場合は直前の金曜にする
 */
fun nextPayday(today: LocalDate, payday: Int, shiftWeekend: Boolean): LocalDate {
    fun inMonth(m: YearMonth): LocalDate {
        var d = m.atDay(minOf(payday, m.lengthOfMonth()))
        if (shiftWeekend) {
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
        }
        return d
    }
    val thisMonth = inMonth(YearMonth.from(today))
    return if (thisMonth >= today) thisMonth else inMonth(YearMonth.from(today).plusMonths(1))
}

/** 給料日までの状況 */
data class PaydayStatus(
    val payday: LocalDate,
    /** 給料日までの日数（今日が給料日なら0） */
    val daysLeft: Int,
    /** 使える残り金額（null は算出できない） */
    val remaining: Long?,
    /** 残り金額の根拠（「今月の予算の残り」など） */
    val basis: String?,
) {
    /** 給料日の前日まで、1日あたり使える金額 */
    val perDay: Long? get() = remaining?.let { if (daysLeft > 0) it / daysLeft else it }
}

/**
 * @param monthlyBudget 全体の月予算（未設定は null）
 * @param period 今の期間の集計（支出・収入）
 */
fun paydayStatus(today: LocalDate, payday: Int, shiftWeekend: Boolean, monthlyBudget: Long?, period: MonthSummary): PaydayStatus {
    val date = nextPayday(today, payday, shiftWeekend)
    val days = ChronoUnit.DAYS.between(today, date).toInt()
    val (remaining, basis) = when {
        monthlyBudget != null -> (monthlyBudget - period.expense) to "今月の予算の残り"
        period.income > 0 -> period.balance to "今月の収入の残り"
        else -> null to null
    }
    return PaydayStatus(date, days, remaining, basis)
}
