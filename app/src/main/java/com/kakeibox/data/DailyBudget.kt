package com.kakeibox.data

import java.time.LocalDate

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
