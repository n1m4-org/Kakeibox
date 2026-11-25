package com.kakeibox.data

import java.time.DayOfWeek
import java.time.LocalDate

/** 週・月の予算の状況（繰り越しなし） */
data class PlainBudgetStatus(val target: BudgetTarget, val budget: Long, val spent: Long) {
    val remaining: Long get() = budget - spent
    val ratio: Float get() = if (budget > 0) spent.toFloat() / budget else 1f
}

/** 設定済みの予算について、対象ごとの支出と比べる（並び：全体 → カテゴリ） */
fun plainBudgetStatuses(budgets: Map<BudgetTarget, Long>, spent: List<DayCategoryTotal>): List<PlainBudgetStatus> =
    BudgetTarget.entries.mapNotNull { t ->
        val budget = budgets[t] ?: return@mapNotNull null
        PlainBudgetStatus(t, budget, spent.spentByDay(t).values.sum())
    }

/** date を含む週（月曜〜日曜） */
data class Week(val start: LocalDate) {
    val endInclusive: LocalDate get() = start.plusDays(6)
    operator fun contains(date: LocalDate) = date in start..endInclusive

    /** 今日を含めた残り日数 */
    fun daysLeft(today: LocalDate): Int = (endInclusive.toEpochDay() - today.toEpochDay() + 1).toInt().coerceIn(0, 7)

    companion object {
        fun of(date: LocalDate) = Week(date.with(DayOfWeek.MONDAY))
    }
}

/** カテゴリ別の支出ランキング（多い順、上位 top 件） */
fun categoryRanking(spent: List<DayCategoryTotal>, top: Int): List<Pair<Category, Long>> =
    spent.groupBy { Category.fromName(it.category) }
        .map { (c, list) -> c to list.sumOf { it.total } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(top)
