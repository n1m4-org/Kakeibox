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

    private val _daily = MutableStateFlow(loadDaily())
    /** 設定済みの1日予算（対象 → 設定）。未設定の対象は含まない。 */
    val daily: StateFlow<Map<BudgetTarget, DailyBudgetSetting>> = _daily.asStateFlow()

    fun set(value: Long) {
        prefs.edit().putLong(KEY_MONTHLY, value).apply()
        _budget.value = value
    }

    /** 金額が変わった場合のみ、繰り越しの起算日を今日にリセットする。0で解除。 */
    fun setDaily(target: BudgetTarget, value: Long) {
        val current = _daily.value[target]?.amount ?: 0L
        if (value == current) return
        val today = LocalDate.now().toEpochDay()
        prefs.edit().putLong(amountKey(target), value).putLong(sinceKey(target), today).apply()
        _daily.value = if (value > 0) {
            _daily.value + (target to DailyBudgetSetting(value, today))
        } else {
            _daily.value - target
        }
    }

    private fun loadDaily(): Map<BudgetTarget, DailyBudgetSetting> {
        val today = LocalDate.now().toEpochDay()
        return BudgetTarget.entries.mapNotNull { t ->
            val amount = prefs.getLong(amountKey(t), 0L)
            if (amount > 0) t to DailyBudgetSetting(amount, prefs.getLong(sinceKey(t), today)) else null
        }.toMap()
    }

    private companion object {
        const val KEY_MONTHLY = "monthly_budget"

        // 全体は以前のバージョンと同じキーを使い、既存の設定を引き継ぐ
        fun amountKey(t: BudgetTarget) = if (t is BudgetTarget.All) "daily_budget" else "daily_budget_${t.key}"
        fun sinceKey(t: BudgetTarget) = if (t is BudgetTarget.All) "daily_budget_since" else "daily_budget_since_${t.key}"
    }
}
