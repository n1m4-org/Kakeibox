package com.kakeibox.data

import java.time.LocalDate
import java.time.YearMonth

/** 1日予算の対象：全体（すべての支出）または支出カテゴリ */
sealed interface BudgetTarget {
    val key: String
    val label: String

    data object All : BudgetTarget {
        override val key = "ALL"
        override val label = "全体"
    }

    data class Of(val category: Category) : BudgetTarget {
        override val key: String get() = category.name
        override val label: String get() = category.label
    }

    companion object {
        /** 選択肢の並び：全体 → 支出カテゴリ */
        val entries: List<BudgetTarget> = listOf(All) + Category.of(TxType.EXPENSE).map { Of(it) }

        fun fromKey(key: String?): BudgetTarget = entries.firstOrNull { it.key == key } ?: All
    }
}

/** 今月の予算 = 1日の予算 × 月の日数 */
fun monthlyBudget(daily: Long, month: YearMonth): Long = daily * month.lengthOfMonth()

/** 対象の日別支出（epochDay → 合計）。全体は全カテゴリを合算する。 */
fun List<DayCategoryTotal>.spentByDay(target: BudgetTarget): Map<Long, Long> =
    asSequence()
        .filter { target is BudgetTarget.All || (target is BudgetTarget.Of && it.category == target.category.name) }
        .groupBy({ it.date }, { it.total })
        .mapValues { (_, totals) -> totals.sum() }

/** 1日予算の設定。since は設定（変更）した日の epochDay。 */
data class DailyBudgetSetting(val amount: Long, val since: Long)

/** 今日の予算の状況 */
data class DailyBudgetStatus(
    /** 設定した1日の予算 */
    val daily: Long,
    /** 前日までの余り（＋）・超過（−） */
    val carryover: Long,
    /** 今日の予算 = daily + carryover（マイナスもありうる） */
    val todayBudget: Long,
    val spentToday: Long,
) {
    val remaining: Long get() = todayBudget - spentToday

    /** 使用率。今日の予算が0以下なら使い切り扱い(1.0) */
    val ratio: Float
        get() = if (todayBudget > 0) spentToday.toFloat() / todayBudget else 1f
}

/**
 * 固定額＋繰り越しで今日の予算を計算する。
 *
 * 繰り越し = Σ(日予算 − その日の支出)  対象: max(月初, 設定日) 〜 昨日
 *
 * @param spentByDay epochDay → その日の支出合計
 * @return 日予算が未設定(0以下)なら null
 */
fun computeDailyBudget(
    daily: Long,
    since: LocalDate,
    today: LocalDate,
    spentByDay: Map<Long, Long>,
): DailyBudgetStatus? {
    if (daily <= 0) return null
    val start = maxOf(today.withDayOfMonth(1), since).toEpochDay()
    val end = today.toEpochDay()
    var carryover = 0L
    for (day in start until end) {
        carryover += daily - (spentByDay[day] ?: 0L)
    }
    return DailyBudgetStatus(
        daily = daily,
        carryover = carryover,
        todayBudget = daily + carryover,
        spentToday = spentByDay[end] ?: 0L,
    )
}

/** 対象ごとの今日の状況 */
data class TargetStatus(val target: BudgetTarget, val status: DailyBudgetStatus)

/** 設定済みの全対象について今日の状況を計算する（並び：全体 → カテゴリ） */
fun computeTodayStatuses(
    settings: Map<BudgetTarget, DailyBudgetSetting>,
    spent: List<DayCategoryTotal>,
    today: LocalDate,
): List<TargetStatus> =
    BudgetTarget.entries.mapNotNull { target ->
        val setting = settings[target] ?: return@mapNotNull null
        computeDailyBudget(setting.amount, LocalDate.ofEpochDay(setting.since), today, spent.spentByDay(target))
            ?.let { TargetStatus(target, it) }
    }
