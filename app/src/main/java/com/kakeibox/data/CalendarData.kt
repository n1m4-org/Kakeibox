package com.kakeibox.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 1日の収入・支出 */
data class DayTotals(val income: Long = 0, val expense: Long = 0)

fun dailyTotals(txs: List<Transaction>): Map<LocalDate, DayTotals> =
    txs.groupBy { it.localDate }.mapValues { (_, list) ->
        DayTotals(
            income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
            expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
        )
    }

/**
 * 日曜はじまりのカレンダーのマス（前後の月の日は null）。7の倍数個。
 */
fun calendarCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value % 7 // 日曜=0
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(leading) { null } + days
    val trailing = (7 - cells.size % 7) % 7
    return cells + List(trailing) { null }
}

/** 日曜=0 … 土曜=6 */
val LocalDate.sundayIndex: Int get() = dayOfWeek.value % 7

val SUNDAY_FIRST: List<DayOfWeek> = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
