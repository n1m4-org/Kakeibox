package com.kakeibox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.BudgetKind
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.PaydayStatus
import com.kakeibox.data.PlainBudgetStatus
import com.kakeibox.data.TargetStatus
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.Period
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetDialog
import com.kakeibox.ui.components.BudgetProgress
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.components.TransactionRow
import com.kakeibox.ui.components.MonthSelector
import com.kakeibox.ui.dayLabel
import com.kakeibox.ui.monthlyBudgetLabel
import com.kakeibox.ui.plusMinusYen
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                MonthSelector(period, vm::prevMonth, vm::nextMonth, vm::thisMonth)
                IconButton(onClick = onSearch, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Outlined.Search, "検索")
                }
            }
        }
        if (period == currentPeriod) {
            payday?.let { p -> item { PaydayCard(p) } }
            item { TodayCard(todayStatuses, period) { onOpenBudgets(BudgetKind.DAILY) } }
            if (weeklyStatuses.isNotEmpty()) {
                item { PlainBudgetsCard("今週の予算（月〜日）", weeklyStatuses) { onOpenBudgets(BudgetKind.WEEKLY) } }
            }
        }
        if (monthlyStatuses.isNotEmpty()) {
            item { PlainBudgetsCard(if (period == currentPeriod) "今月の予算" else "この月の予算", monthlyStatuses) { onOpenBudgets(BudgetKind.MONTHLY) } }
        }
        item { SummaryCard(summary) }

        if (list.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("この月の記録はまだありません\n右下の＋から追加できます",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        grouped.forEach { (day, txs) ->
            item(key = "h$day") {
                val dayTotal = txs.sumOf { if (it.type == TxType.INCOME) it.amount else -it.amount }
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(txs.first().localDate.dayLabel(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(signedYen(dayTotal), style = MaterialTheme.typography.labelLarge)
                }
            }
            items(txs, key = { it.id }) { tx ->
                TransactionRow(tx, onClick = { onEdit(tx.id) })
                HorizontalDivider()
            }
        }
    }
}

/** 給料日までのカウントダウン */
@Composable
internal fun PaydayCard(status: PaydayStatus) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Payments, null)
            Column(Modifier.weight(1f)) {
                if (status.daysLeft == 0) {
                    Text("今日は給料日です", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        "給料日まであと${status.daysLeft}日",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(status.payday.dayLabel(), style = MaterialTheme.typography.bodySmall)
                }
            }
            val perDay = status.perDay
            if (perDay != null && status.daysLeft > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("1日あたり", style = MaterialTheme.typography.labelSmall)
                    Text(
                        signedYen(perDay),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (perDay < 0) ExpenseColor else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text("${status.basis} ${signedYen(status.remaining ?: 0)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** 週・月の予算（設定済みの対象ごと） */
@Composable
internal fun PlainBudgetsCard(title: String, statuses: List<PlainBudgetStatus>, onOpenBudgets: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenBudgets) { Text("設定") }
            }
            statuses.forEachIndexed { i, st ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                val over = st.remaining < 0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TargetBadge(st.target, size = 28.dp)
                        Text(st.target.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            if (over) "${yen(-st.remaining)} オーバー" else "残り ${yen(st.remaining)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    BudgetProgress(st.ratio)
                    Text(
                        "使用 ${yen(st.spent)} / 予算 ${yen(st.budget)}（${(st.ratio * 100).toInt()}%）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 今日の予算（設定済みの対象ごと） */
@Composable
internal fun TodayCard(statuses: List<TargetStatus>, period: Period, onOpenBudgets: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日の予算", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenBudgets) { Text(if (statuses.isEmpty()) "1日の予算を設定" else "設定") }
            }
            if (statuses.isEmpty()) {
                Text(
                    "全体やカテゴリごとに1日の予算を設定すると、今日使える金額がここに表示されます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            statuses.forEachIndexed { i, ts ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                TargetStatusRow(ts, period)
            }
            val categories = statuses.filter { it.target is BudgetTarget.Of }
            if (categories.size >= 2) {
                val dailySum = categories.sumOf { it.status.daily }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Text(
                    "カテゴリ合計　${monthlyBudgetLabel(dailySum, period)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TargetStatusRow(ts: TargetStatus, period: Period) {
    val status = ts.status
    val over = status.remaining < 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TargetBadge(ts.target, size = 32.dp)
            Text(ts.target.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                if (over) "${yen(-status.remaining)} オーバー" else "残り ${yen(status.remaining)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurface,
            )
        }
        BudgetProgress(status.ratio)
        Text(
            "使用 ${yen(status.spentToday)} / 今日の予算 ${signedYen(status.todayBudget)}（繰り越し ${plusMinusYen(status.carryover)}）",
            style = MaterialTheme.typography.bodySmall,
            color = if (status.carryover < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            monthlyBudgetLabel(status.daily, period),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCard(summary: MonthSummary) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("収支", style = MaterialTheme.typography.labelLarge)
            Text(
                signedYen(summary.balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (summary.balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                LabeledAmount("収入", summary.income, IncomeColor, Modifier.weight(1f))
                LabeledAmount("支出", summary.expense, ExpenseColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LabeledAmount(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(yen(amount), style = MaterialTheme.typography.titleMedium, color = color)
    }
}

