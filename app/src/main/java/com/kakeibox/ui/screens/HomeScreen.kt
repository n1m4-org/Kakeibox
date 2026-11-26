package com.kakeibox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.BudgetKind
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.PaydayStatus
import com.kakeibox.data.PlainBudgetStatus
import com.kakeibox.data.TargetStatus
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetProgress
import com.kakeibox.ui.components.MonthSelector
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.components.TransactionRow
import com.kakeibox.ui.plusMinusYen
import com.kakeibox.ui.shortDayLabel
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.theme.AcrylicCard
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen

/** カード同士・画面端との間隔 */
private val CardPadding = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)

@Composable
fun HomeScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onOpenBudgets: (BudgetKind) -> Unit,
    onSearch: () -> Unit,
) {
    val period by vm.period.collectAsStateWithLifecycle()
    val currentPeriod by vm.currentPeriod.collectAsStateWithLifecycle()
    val list by vm.transactions.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val todayStatuses by vm.todayStatuses.collectAsStateWithLifecycle()
    val weeklyStatuses by vm.weeklyStatuses.collectAsStateWithLifecycle()
    val monthlyStatuses by vm.monthlyStatuses.collectAsStateWithLifecycle()
    val payday by vm.paydayStatus.collectAsStateWithLifecycle()

    val grouped = remember(list) { list.groupBy { it.date } }
    val isCurrent = period == currentPeriod

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item {
            AcrylicCard(CardPadding) {
                Box(Modifier.fillMaxWidth()) {
                    MonthSelector(period, vm::prevMonth, vm::nextMonth, vm::thisMonth)
                    IconButton(onClick = onSearch, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Outlined.Search, "検索")
                    }
                }
                SummaryContent(summary)
            }
        }
        if (isCurrent) {
            payday?.let { p -> item { PaydayCard(p) } }
            item { TodayCard(todayStatuses) { onOpenBudgets(BudgetKind.DAILY) } }
            if (weeklyStatuses.isNotEmpty()) {
                item { PlainBudgetsCard("今週の予算", weeklyStatuses) { onOpenBudgets(BudgetKind.WEEKLY) } }
            }
        }
        if (monthlyStatuses.isNotEmpty()) {
            item { PlainBudgetsCard(if (isCurrent) "今月の予算" else "この月の予算", monthlyStatuses) { onOpenBudgets(BudgetKind.MONTHLY) } }
        }

        if (list.isEmpty()) {
            item {
                AcrylicCard(CardPadding) {
                    Text(
                        "記録はまだありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
        }

        // 日ごとに1枚のカード
        grouped.forEach { (day, txs) ->
            item(key = "d$day") {
                AcrylicCard(CardPadding) {
                    val dayTotal = txs.sumOf { if (it.type == TxType.INCOME) it.amount else -it.amount }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)) {
                        Text(txs.first().localDate.shortDayLabel(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text(
                            signedYen(dayTotal),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    txs.forEachIndexed { i, tx ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        TransactionRow(tx, onClick = { onEdit(tx.id) })
                    }
                }
            }
        }
    }
}

/** 収支（大）と収入・支出 */
@Composable
private fun SummaryContent(summary: MonthSummary) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
        Text(
            signedYen(summary.balance),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (summary.balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("収入 ${yen(summary.income)}", style = MaterialTheme.typography.bodyMedium, color = IncomeColor)
            Text("支出 ${yen(summary.expense)}", style = MaterialTheme.typography.bodyMedium, color = ExpenseColor)
        }
    }
}

/** 給料日までのカウントダウン（1行） */
@Composable
internal fun PaydayCard(status: PaydayStatus) {
    AcrylicCard(CardPadding) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            if (status.daysLeft == 0) {
                Text("今日は給料日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                return@Row
            }
            Text("給料日まで ", style = MaterialTheme.typography.bodyMedium)
            Text("${status.daysLeft}日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            status.perDay?.let { perDay ->
                Text(
                    signedYen(perDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (perDay < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                )
                Text("/日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** カードの見出し（タイトル＋予算の設定アイコン） */
@Composable
private fun CardHeader(title: String, onOpenBudgets: () -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenBudgets) { Icon(Icons.Outlined.Tune, "予算の設定", Modifier.size(20.dp)) }
    }
}

/** 1つの予算：アイコン・名前・残り／バー／使用額 / 予算 */
@Composable
private fun BudgetRow(target: BudgetTarget, remaining: Long, ratio: Float, spent: Long, budget: Long, carryover: Long = 0) {
    val over = remaining < 0
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TargetBadge(target, size = 28.dp)
            Text(target.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                if (over) "${yen(-remaining)} オーバー" else "残り ${yen(remaining)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurface,
            )
        }
        BudgetProgress(ratio)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${yen(spent)} / ${signedYen(budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // 繰り越しは小さなチップで
            if (carryover != 0L) {
                Text(
                    plusMinusYen(carryover),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (carryover < 0) ExpenseColor else IncomeColor,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** 週・月の予算（設定済みの対象ごと） */
@Composable
internal fun PlainBudgetsCard(title: String, statuses: List<PlainBudgetStatus>, onOpenBudgets: () -> Unit) {
    AcrylicCard(CardPadding) {
        CardHeader(title, onOpenBudgets)
        statuses.forEachIndexed { i, st ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            BudgetRow(st.target, st.remaining, st.ratio, st.spent, st.budget)
        }
        Spacer(Modifier.height(14.dp))
    }
}

/** 今日の予算（設定済みの対象ごと） */
@Composable
internal fun TodayCard(statuses: List<TargetStatus>, onOpenBudgets: () -> Unit) {
    AcrylicCard(CardPadding) {
        if (statuses.isEmpty()) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("今日の予算", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenBudgets) { Text("設定する") }
            }
            return@AcrylicCard
        }
        CardHeader("今日の予算", onOpenBudgets)
        statuses.forEachIndexed { i, ts ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            val s = ts.status
            BudgetRow(ts.target, s.remaining, s.ratio, s.spentToday, s.todayBudget, s.carryover)
        }
        Spacer(Modifier.height(14.dp))
    }
}
