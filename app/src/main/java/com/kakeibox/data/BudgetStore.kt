package com.kakeibox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** 予算の単位 */
enum class BudgetKind(val label: String) {
    DAILY("1日"),
    WEEKLY("1週間"),
    MONTHLY("1か月"),
}

/** 予算設定（円）。0は未設定。対象（全体／カテゴリ）ごとに、1日・1週間・1か月の予算を持つ。 */
class BudgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox", Context.MODE_PRIVATE)

    private val _daily = MutableStateFlow(loadDaily())
    /** 設定済みの1日予算（対象 → 設定）。未設定の対象は含まない。 */
    val daily: StateFlow<Map<BudgetTarget, DailyBudgetSetting>> = _daily.asStateFlow()

    private val _weekly = MutableStateFlow(loadPlain(BudgetKind.WEEKLY))
    /** 設定済みの週予算（月曜はじまり） */
    val weekly: StateFlow<Map<BudgetTarget, Long>> = _weekly.asStateFlow()

    private val _monthly = MutableStateFlow(loadPlain(BudgetKind.MONTHLY))
    /** 設定済みの月予算（締め日で区切った1か月） */
    val monthly: StateFlow<Map<BudgetTarget, Long>> = _monthly.asStateFlow()

    fun set(kind: BudgetKind, target: BudgetTarget, value: Long) = when (kind) {
        BudgetKind.DAILY -> setDaily(target, value)
        BudgetKind.WEEKLY -> setPlain(kind, _weekly, target, value)
        BudgetKind.MONTHLY -> setPlain(kind, _monthly, target, value)
    }

    fun amount(kind: BudgetKind, target: BudgetTarget): Long = when (kind) {
        BudgetKind.DAILY -> _daily.value[target]?.amount
        BudgetKind.WEEKLY -> _weekly.value[target]
        BudgetKind.MONTHLY -> _monthly.value[target]
    } ?: 0L

    /** 金額が変わった場合のみ、繰り越しの起算日を今日にリセットする。0で解除。 */
    fun setDaily(target: BudgetTarget, value: Long) {
        val current = _daily.value[target]?.amount ?: 0L
        if (value == current) return
        val today = LocalDate.now().toEpochDay()
        prefs.edit().putLong(amountKey(BudgetKind.DAILY, target), value).putLong(sinceKey(target), today).apply()
        _daily.value = if (value > 0) {
            _daily.value + (target to DailyBudgetSetting(value, today))
        } else {
            _daily.value - target
        }
    }

    private fun setPlain(kind: BudgetKind, state: MutableStateFlow<Map<BudgetTarget, Long>>, target: BudgetTarget, value: Long) {
        prefs.edit().putLong(amountKey(kind, target), value).apply()
        state.value = if (value > 0) state.value + (target to value) else state.value - target
    }

    private fun loadDaily(): Map<BudgetTarget, DailyBudgetSetting> {
        val today = LocalDate.now().toEpochDay()
        return BudgetTarget.entries.mapNotNull { t ->
            val amount = prefs.getLong(amountKey(BudgetKind.DAILY, t), 0L)
            if (amount > 0) t to DailyBudgetSetting(amount, prefs.getLong(sinceKey(t), today)) else null
        }.toMap()
    }

    private fun loadPlain(kind: BudgetKind): Map<BudgetTarget, Long> =
        BudgetTarget.entries.mapNotNull { t ->
            prefs.getLong(amountKey(kind, t), 0L).takeIf { it > 0 }?.let { t to it }
        }.toMap()

    private companion object {
        // 全体は以前のバージョンと同じキーを使い、既存の設定を引き継ぐ
        fun amountKey(kind: BudgetKind, t: BudgetTarget): String {
            val base = when (kind) {
                BudgetKind.DAILY -> "daily_budget"
                BudgetKind.WEEKLY -> "weekly_budget"
                BudgetKind.MONTHLY -> "monthly_budget"
            }
            return if (t is BudgetTarget.All) base else "${base}_${t.key}"
        }

        fun sinceKey(t: BudgetTarget) = if (t is BudgetTarget.All) "daily_budget_since" else "daily_budget_since_${t.key}"
    }
}
