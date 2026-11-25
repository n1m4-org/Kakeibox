package com.kakeibox.data

import java.time.LocalDate

/** 期間ごとの収入・支出 */
data class PeriodTotal(val period: Period, val income: Long, val expense: Long) {
    val balance: Long get() = income - expense
}

/** periods（古い順）ごとに集計する */
fun periodTotals(txs: List<Transaction>, periods: List<Period>): List<PeriodTotal> =
    periods.map { p ->
        val inP = txs.filter { it.date in p.startEpochDay..p.endEpochDay }
        PeriodTotal(
            period = p,
            income = inP.filter { it.type == TxType.INCOME }.sumOf { it.amount },
            expense = inP.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
        )
    }

/** last を最後とする直近 count 期間（古い順） */
fun recentPeriods(last: Period, count: Int): List<Period> = (count - 1 downTo 0).map { last.shift(-it.toLong()) }

/** 前の期間との比較 */
data class Change(val current: Long, val previous: Long) {
    val diff: Long get() = current - previous
    /** 増減率。前期が0なら null */
    val rate: Float? get() = if (previous > 0) diff.toFloat() / previous else null
}

/** カテゴリごとの前期比（今期・前期のどちらかにあるカテゴリ） */
fun categoryChanges(current: List<CategoryTotal>, previous: List<CategoryTotal>): Map<Category, Change> {
    val prev = previous.associate { it.category to it.total }
    val cur = current.associate { it.category to it.total }
    return (cur.keys + prev.keys).associateWith { Change(cur[it] ?: 0L, prev[it] ?: 0L) }
}

/** 年間レポート。月 = 開始日がその年にある期間（締め日に対応） */
data class YearReport(
    val year: Int,
    val months: List<PeriodTotal>,
    val summary: MonthSummary,
    /** 平均の母数（経過した月の数、1以上） */
    val elapsedMonths: Int,
) {
    val averageIncome: Long get() = summary.income / elapsedMonths
    val averageExpense: Long get() = summary.expense / elapsedMonths

    companion object {
        fun periodsOf(year: Int, startDay: Int): List<Period> =
            (1..12).map { m -> Period(LocalDate.of(year, m, startDay), startDay) }

        fun build(year: Int, startDay: Int, txs: List<Transaction>, today: LocalDate): YearReport {
            val periods = periodsOf(year, startDay)
            val range = periods.first().startEpochDay..periods.last().endEpochDay
            val inYear = txs.filter { it.date in range }
            return YearReport(
                year = year,
                months = periodTotals(inYear, periods),
                summary = MonthSummary.from(inYear),
                elapsedMonths = periods.count { it.start <= today }.coerceIn(1, 12),
            )
        }
    }
}
