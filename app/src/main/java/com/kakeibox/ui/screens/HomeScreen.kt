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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.DailyBudgetStatus
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.MonthSelector
import com.kakeibox.ui.dayLabel
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.theme.WarningColor
import com.kakeibox.ui.yen
import java.time.YearMonth

@Composable
fun HomeScreen(vm: MainViewModel, contentPadding: PaddingValues, onEdit: (Long) -> Unit) {
    val month by vm.month.collectAsStateWithLifecycle()
    val list by vm.transactions.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()
    val daily by vm.dailyBudget.collectAsStateWithLifecycle()
    val today by vm.todayStatus.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<BudgetKind?>(null) }

    val grouped = remember(list) { list.groupBy { it.date } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item { MonthSelector(month, vm::prevMonth, vm::nextMonth, vm::thisMonth) }
        if (month == YearMonth.now()) {
            item { TodayCard(today, onEditDaily = { dialog = BudgetKind.DAILY }) }
        }
        item { SummaryCard(summary, budget, onEditBudget = { dialog = BudgetKind.MONTHLY }) }

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

    when (dialog) {
        BudgetKind.MONTHLY -> BudgetDialog(
            title = "月の予算",
            current = budget,
            onDismiss = { dialog = null },
            onSave = { vm.setBudget(it); dialog = null },
        )
        BudgetKind.DAILY -> BudgetDialog(
            title = "1日の予算",
            description = "使わなかった分は翌日に繰り越され、使いすぎた分は翌日の予算から差し引かれます（月初にリセット）。金額を変更すると、繰り越しは今日から数え直します。",
            current = daily.amount,
            onDismiss = { dialog = null },
            onSave = { vm.setDailyBudget(it); dialog = null },
        )
        null -> Unit
    }
}

private enum class BudgetKind { MONTHLY, DAILY }

@Composable
private fun TodayCard(status: DailyBudgetStatus?, onEditDaily: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
            if (status == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日の予算", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onEditDaily) { Text("1日の予算を設定") }
                }
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日の残り", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (status.remaining >= 0) yen(status.remaining) else "${yen(-status.remaining)} オーバー",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (status.remaining < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onEditDaily) { Icon(Icons.Outlined.Edit, "1日の予算を設定") }
            }
            Column(Modifier.padding(end = 12.dp)) {
                Spacer(Modifier.height(4.dp))
                BudgetProgress(status.ratio)
                Spacer(Modifier.height(4.dp))
                Text(
                    "使用 ${yen(status.spentToday)} / 予算 ${yen(status.todayBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "1日 ${yen(status.daily)}　繰り越し ${signedYen(status.carryover).let { if (status.carryover > 0) "+$it" else it }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.carryover < 0) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 予算の使用率バー（80%で黄、100%以上で赤） */
@Composable
private fun BudgetProgress(ratio: Float) {
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = when {
            ratio >= 1f -> ExpenseColor
            ratio >= 0.8f -> WarningColor
            else -> MaterialTheme.colorScheme.primary
        },
    )
}

@Composable
private fun SummaryCard(summary: MonthSummary, budget: Long, onEditBudget: () -> Unit) {
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
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("月の予算", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(if (budget > 0) yen(budget) else "未設定", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onEditBudget) { Icon(Icons.Outlined.Edit,"予算を設定") }
            }
            if (budget > 0) {
                val ratio = summary.expense.toFloat() / budget
                val remaining = budget - summary.expense
                BudgetProgress(ratio)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (remaining >= 0) "残り ${yen(remaining)}（${(ratio * 100).toInt()}% 使用）"
                    else "予算オーバー ${yen(-remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining < 0) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val cat = tx.categoryEnum
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(cat)
        Column(Modifier.weight(1f)) {
            Text(cat.label, style = MaterialTheme.typography.bodyLarge)
            if (tx.memo.isNotBlank()) {
                Text(tx.memo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(4.dp))
        val income = tx.type == TxType.INCOME
        Text(
            (if (income) "+" else "-") + yen(tx.amount),
            style = MaterialTheme.typography.titleMedium,
            color = if (income) IncomeColor else ExpenseColor,
        )
    }
}

@Composable
private fun BudgetDialog(
    title: String,
    current: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    description: String? = null,
) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter(Char::isDigit).take(10) },
                label = { Text("金額（円）・0で解除") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
          }
        },
        confirmButton = { TextButton(onClick = { onSave(text.toLongOrNull() ?: 0L) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
