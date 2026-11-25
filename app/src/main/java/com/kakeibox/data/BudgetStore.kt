package com.kakeibox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** 予算設定（円）。0は未設定。 */
class BudgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox", Context.MODE_PRIVATE)

    private val _budget = MutableStateFlow(prefs.getLong(KEY_MONTHLY, 0L))
    /** 月の予算 */
    val budget: StateFlow<Long> = _budget.asStateFlow()

    private val _daily = MutableStateFlow(
        DailyBudgetSetting(
            amount = prefs.getLong(KEY_DAILY, 0L),
            since = prefs.getLong(KEY_DAILY_SINCE, LocalDate.now().toEpochDay()),
        )
    )
    /** 1日の予算 */
    val daily: StateFlow<DailyBudgetSetting> = _daily.asStateFlow()

    fun set(value: Long) {
        prefs.edit().putLong(KEY_MONTHLY, value).apply()
        _budget.value = value
    }

    /** 金額が変わった場合のみ、繰り越しの起算日を今日にリセットする */
    fun setDaily(value: Long) {
        if (value == _daily.value.amount) return
        val setting = DailyBudgetSetting(value, LocalDate.now().toEpochDay())
        prefs.edit().putLong(KEY_DAILY, setting.amount).putLong(KEY_DAILY_SINCE, setting.since).apply()
        _daily.value = setting
    }

    private companion object {
        const val KEY_MONTHLY = "monthly_budget"
        const val KEY_DAILY = "daily_budget"
        const val KEY_DAILY_SINCE = "daily_budget_since"
    }
}
