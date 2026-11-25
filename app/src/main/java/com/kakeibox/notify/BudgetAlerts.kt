package com.kakeibox.notify

import android.content.Context
import com.kakeibox.AppGraph
import com.kakeibox.data.AlertCandidate
import com.kakeibox.data.BudgetKind
import com.kakeibox.data.Week
import com.kakeibox.data.alertsToSend
import com.kakeibox.data.computeTodayStatuses
import com.kakeibox.data.message
import com.kakeibox.data.plainBudgetStatuses
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** 記録の変更後に、予算の80%・100%を超えていれば通知する */
object BudgetAlerts {
    private val mutex = Mutex()
    private const val PREFS = "kakeibox_alerts"
    private const val KEY_SENT = "sent"

    suspend fun check(context: Context) = mutex.withLock {
        val app = context.applicationContext
        val settings = AppGraph.settingsStore(app)
        if (!settings.settings.value.budgetAlertEnabled) return@withLock
        val budgets = AppGraph.budgetStore(app)
        val repo = AppGraph.repository(app)
        val today = LocalDate.now()
        val period = settings.currentPeriod(today)
        val week = Week.of(today)
        val periodSpent = repo.dailyExpense(period)
        val weekSpent = repo.dailyExpense(week.start, week.endInclusive)

        val candidates = buildList {
            computeTodayStatuses(budgets.daily.value, periodSpent, today, period.start).forEach { ts ->
                // 日予算は「今日の予算（繰り越し込み）」に対する使用額で判定
                add(AlertCandidate(BudgetKind.DAILY, ts.target, today.toString(), ts.status.todayBudget.coerceAtLeast(0), ts.status.spentToday))
            }
            plainBudgetStatuses(budgets.weekly.value, weekSpent).forEach {
                add(AlertCandidate(BudgetKind.WEEKLY, it.target, week.start.toString(), it.budget, it.spent))
            }
            plainBudgetStatuses(budgets.monthly.value, periodSpent).forEach {
                add(AlertCandidate(BudgetKind.MONTHLY, it.target, period.start.toString(), it.budget, it.spent))
            }
        }.filter { it.spent > 0 }

        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // 古い期間の記録は捨てる（今の期間のキーだけ残す）
        val currentKeys = setOf(today.toString(), week.start.toString(), period.start.toString())
        val sent = prefs.getStringSet(KEY_SENT, emptySet())!!.filter { k -> currentKeys.any { k.contains(":$it:") } }.toMutableSet()

        alertsToSend(candidates, sent).forEachIndexed { i, alert ->
            val (title, body) = alert.message()
            Notifications.showBudgetAlert(app, i, title, body)
            sent += alert.key
        }
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
    }
}
