package com.kakeibox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.DayTotals
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.data.calendarCells
import com.kakeibox.data.dailyTotals
import com.kakeibox.data.sundayIndex
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.compactYen
import com.kakeibox.ui.components.TransactionRow
import com.kakeibox.ui.fullLabel
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: MainViewModel, contentPadding: PaddingValues, onOpen: (Long) -> Unit) {
    val month by vm.calendarMonth.collectAsStateWithLifecycle()
    val txs by vm.calendarTransactions.collectAsStateWithLifecycle()
    val daily by vm.dailyBudget.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    CalendarContent(
        month = month,
        txs = txs,
        dailyLimit = daily[BudgetTarget.All]?.amount,
        today = today,
        contentPadding = contentPadding,
        onPrev = { vm.changeCalendarMonth(-1) },
        onNext = { vm.changeCalendarMonth(1) },
        onToday = vm::calendarToday,
        onOpen = onOpen,
    )
}

@Composable
internal fun CalendarContent(
    month: YearMonth,
    txs: List<Transaction>,
    dailyLimit: Long?,
    today: LocalDate,
    contentPadding: PaddingValues,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val totals = remember(txs) { dailyTotals(txs) }
    var selectedDay by rememberSaveable(month) {
        mutableStateOf(if (YearMonth.from(today) == month) today.dayOfMonth else 1)
    }
    val selected = month.atDay(selectedDay.coerceIn(1, month.lengthOfMonth()))
    val dayTxs = txs.filter { it.localDate == selected }.sortedByDescending { it.id }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "前の月") }
                TextButton(onClick = onToday) { Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.titleLarge) }
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "次の月") }
            }
        }
        item {
            val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            val income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("収入 ${yen(income)}", color = IncomeColor, style = MaterialTheme.typography.labelLarge)
                Text("支出 ${yen(expense)}", color = ExpenseColor, style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "月", "火", "水", "木", "金", "土").forEachIndexed { i, w ->
                        Text(
                            w,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = weekdayColor(i) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                    }
                }
                calendarCells(month).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(Modifier.weight(1f).aspectRatio(0.8f).padding(2.dp)) {
                                if (date != null) {
                                    DayCell(
                                        date = date,
                                        totals = totals[date],
                                        isToday = date == today,
                                        isSelected = date == selected,
                                        overBudget = dailyLimit != null && (totals[date]?.expense ?: 0) > dailyLimit,
                                        onClick = { selectedDay = date.dayOfMonth },
                                    )
                                }
                            }
                        }
                    }
                }
                if (dailyLimit != null) {
                    Text(
                        "赤いマスは1日の予算（全体 ${yen(dailyLimit)}）を超えた日",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )
                }
            }
        }
        item {
            val t = totals[selected] ?: DayTotals()
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(selected.fullLabel(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (t.income > 0) Text("+${yen(t.income)}", color = IncomeColor, style = MaterialTheme.typography.labelLarge)
                if (t.expense > 0) Text("-${yen(t.expense)}", color = ExpenseColor, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (dayTxs.isEmpty()) {
            item {
                Text("この日の記録はありません", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
            }
        }
        items(dayTxs, key = { it.id }) { tx ->
            TransactionRow(tx, onClick = { onOpen(tx.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    totals: DayTotals?,
    isToday: Boolean,
    isSelected: Boolean,
    overBudget: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        overBudget -> ExpenseColor.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Column(
        Modifier.fillMaxSize()
            .background(bg, shape)
            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = weekdayColor(date.sundayIndex) ?: MaterialTheme.colorScheme.onSurface,
        )
        if (totals != null && totals.expense > 0) {
            Text(compactYen(totals.expense), style = MaterialTheme.typography.labelSmall, color = ExpenseColor, maxLines = 1)
        }
        if (totals != null && totals.income > 0) {
            Text(compactYen(totals.income), style = MaterialTheme.typography.labelSmall, color = IncomeColor, maxLines = 1)
        }
    }
}

/** 日曜は赤、土曜は青 */
private fun weekdayColor(sundayIndex: Int): Color? = when (sundayIndex) {
    0 -> Color(0xFFD32F2F)
    6 -> Color(0xFF1976D2)
    else -> null
}
