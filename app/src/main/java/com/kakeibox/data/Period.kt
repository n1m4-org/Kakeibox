package com.kakeibox.data

import java.time.LocalDate

/**
 * 集計の1か月（締め日で区切った期間）。
 * startDay=1 ならカレンダーどおりの月、25 なら「25日〜翌月24日」。
 */
data class Period(val start: LocalDate, val startDay: Int) {
    init {
        require(startDay in 1..MAX_START_DAY)
        require(start.dayOfMonth == startDay)
    }

    val endInclusive: LocalDate get() = start.plusMonths(1).minusDays(1)
    val days: Int get() = (endInclusive.toEpochDay() - start.toEpochDay() + 1).toInt()
    val startEpochDay: Long get() = start.toEpochDay()
    val endEpochDay: Long get() = endInclusive.toEpochDay()

    operator fun contains(date: LocalDate): Boolean = date in start..endInclusive

    fun shift(months: Long): Period = Period(start.plusMonths(months), startDay)
    fun next(): Period = shift(1)
    fun previous(): Period = shift(-1)

    /** 「2026年11月」（開始日1）／「2026/11/25〜12/24」 */
    val label: String
        get() = if (startDay == 1) {
            "${start.year}年${start.monthValue}月"
        } else {
            "${start.year}/${start.monthValue}/${start.dayOfMonth}〜${endInclusive.monthValue}/${endInclusive.dayOfMonth}"
        }

    companion object {
        /** 締め日として選べる最大値（すべての月に存在する日） */
        const val MAX_START_DAY = 28

        /** date を含む期間 */
        fun of(date: LocalDate, startDay: Int): Period {
            val base = if (date.dayOfMonth >= startDay) date else date.minusMonths(1)
            return Period(base.withDayOfMonth(startDay), startDay)
        }
    }
}
