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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.kakeibox.data.CategoryTotal
import com.kakeibox.data.Change
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.PeriodTotal
import com.kakeibox.data.TxType
import com.kakeibox.data.YearReport
import com.kakeibox.data.categoryChanges
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetProgress
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.DonutChart
import com.kakeibox.ui.components.MonthSelector
import com.kakeibox.ui.components.TrendChart
import com.kakeibox.ui.components.axisLabel
import com.kakeibox.ui.components.changeLabel
import com.kakeibox.ui.icon
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen
import java.time.LocalDate

private enum class ReportMode(val label: String) { MONTH("月"), YEAR("年") }

@Composable
fun ReportScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    var mode by rememberSaveable { mutableStateOf(ReportMode.MONTH) }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    val period by vm.period.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val previous by vm.previousSummary.collectAsStateWithLifecycle()
    val trend by vm.trend.collectAsStateWithLifecycle()
    val monthlyBudgets by vm.monthlyBudget.collectAsStateWithLifecycle()
    val yearReport by vm.yearReport.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                ReportMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, ReportMode.entries.size),
                    ) { Text(m.label) }
                }
            }
        }
        when (mode) {
            ReportMode.MONTH -> {
                item { MonthSelector(period, vm::prevMonth, vm::nextMonth, vm::thisMonth) }
                monthReport(summary, previous, trend, type, { type = it }, monthlyBudgets)
            }
            ReportMode.YEAR -> {
                item {
                    YearSelector(yearReport?.year ?: LocalDate.now().year, { vm.changeYear(-1) }, { vm.changeYear(1) }, vm::thisYear)
                }
                yearReport?.let { yearReport(it, type, { t -> type = t }) }
            }
        }
    }
}

/** 月のレポート：内訳・前月比・支払い方法・推移 */
internal fun LazyListScope.monthReport(
    summary: MonthSummary,
    previous: MonthSummary,
    trend: List<PeriodTotal>,
    type: TxType,
    onType: (TxType) -> Unit,
    monthlyBudgets: Map<BudgetTarget, Long>,
) {
    val items = if (type == TxType.EXPENSE) summary.expenseByCategory else summary.incomeByCategory
    val prevItems = if (type == TxType.EXPENSE) previous.expenseByCategory else previous.incomeByCategory
    val total = if (type == TxType.EXPENSE) summary.expense else summary.income
    val prevTotal = if (type == TxType.EXPENSE) previous.expense else previous.income
    val changes = categoryChanges(items, prevItems)

    item {
        TypeToggle(type, onChange = onType, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(16.dp))
    }
    item {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DonutChart(items, total) }
        val change = Change(total, prevTotal)
        Text(
            "前月比 ${changeLabel(change.diff, change.rate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = changeColor(type, change.diff),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        )
    }
    if (items.isEmpty()) {
        item {
            Text("データがありません", Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(items, key = { "cat_" + it.category.name }) { ct ->
        val ch = changes[ct.category]
        CategoryRow(
            ct,
            sub = ch?.let { "前月比 ${changeLabel(it.diff, it.rate)}" },
            subColor = ch?.let { changeColor(type, it.diff) },
            budget = monthlyBudgets[BudgetTarget.Of(ct.category)]?.takeIf { type == TxType.EXPENSE },
        )
    }
    // 今月はゼロだが前月はあったカテゴリ
    val gone = changes.filter { (c, ch) -> ch.current == 0L && ch.previous > 0 && c.type == type }
    if (gone.isNotEmpty()) {
        item {
            Text(
                "今月はなし：" + gone.entries.joinToString("、") { (c, ch) -> "${c.label}（前月 ${yen(ch.previous)}）" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    if (type == TxType.EXPENSE && summary.expenseByPayment.isNotEmpty()) {
        item { SectionTitle("支払い方法別") }
        items(summary.expenseByPayment, key = { "pay_" + it.first.name }) { (method, sum) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(method.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(method.label, Modifier.weight(1f))
                Text(
                    "%.1f%%".format(if (summary.expense > 0) sum * 100f / summary.expense else 0f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(yen(sum), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
    if (trend.isNotEmpty()) {
        item { SectionTitle("推移（直近12か月）") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                TrendChart(trend, Modifier.padding(16.dp))
            }
        }
    }
}

/** 年のレポート：合計・月平均・月別推移・カテゴリ内訳・月別一覧 */
internal fun LazyListScope.yearReport(report: YearReport, type: TxType, onType: (TxType) -> Unit) {
    val s = report.summary
    item {
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${report.year}年の収支", style = MaterialTheme.typography.labelLarge)
                Text(
                    signedYen(s.balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (s.balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                )
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("収入", style = MaterialTheme.typography.labelMedium)
                        Text(yen(s.income), style = MaterialTheme.typography.titleMedium, color = IncomeColor)
                        Text("月平均 ${yen(report.averageIncome)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("支出", style = MaterialTheme.typography.labelMedium)
                        Text(yen(s.expense), style = MaterialTheme.typography.titleMedium, color = ExpenseColor)
                        Text("月平均 ${yen(report.averageExpense)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "月平均は経過した${report.elapsedMonths}か月で計算",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    item { SectionTitle("月別の推移") }
    item {
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TrendChart(report.months.take(report.elapsedMonths), Modifier.padding(16.dp))
        }
    }
    item {
        SectionTitle("カテゴリ別")
        TypeToggle(type, onChange = onType, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    val items = if (type == TxType.EXPENSE) s.expenseByCategory else s.incomeByCategory
    if (items.isEmpty()) {
        item { Text("データがありません", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(items, key = { "ycat_" + it.category.name }) { ct ->
        CategoryRow(ct, sub = "月平均 ${yen(ct.total / report.elapsedMonths)}", subColor = null, budget = null)
    }
    item { SectionTitle("月別一覧") }
    item {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf("月", "収入", "支出", "収支").forEachIndexed { i, h ->
                Text(
                    h,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                    modifier = Modifier.weight(if (i == 0) 0.8f else 1f),
                )
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    }
    items(report.months, key = { "ym_" + it.period.start }) { m ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(m.period.axisLabel(), modifier = Modifier.weight(0.8f))
            Text(yen(m.income), color = IncomeColor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(yen(m.expense), color = ExpenseColor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(
                signedYen(m.balance),
                fontWeight = FontWeight.Bold,
                color = if (m.balance < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CategoryRow(ct: CategoryTotal, sub: String?, subColor: Color?, budget: Long?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(6.dp, 28.dp).background(Color(ct.category.color), CircleShape))
            CategoryBadge(ct.category, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(ct.category.label)
                if (sub != null) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = subColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("%.1f%%".format(ct.ratio * 100), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(yen(ct.total), style = MaterialTheme.typography.titleSmall)
        }
        // カテゴリの月予算があれば進捗を表示
        if (budget != null) {
            val ratio = ct.total.toFloat() / budget
            Column(Modifier.padding(start = 62.dp, top = 6.dp)) {
                BudgetProgress(ratio)
                Text(
                    "月予算 ${yen(budget)} の ${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 支出の増加・収入の減少は赤、それ以外は緑 */
private fun changeColor(type: TxType, diff: Long): Color = when {
    diff == 0L -> Color.Gray
    (type == TxType.EXPENSE) == (diff > 0) -> ExpenseColor
    else -> IncomeColor
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun YearSelector(year: Int, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "前の年") }
        TextButton(onClick = onToday) { Text("${year}年", style = MaterialTheme.typography.titleLarge) }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "次の年") }
    }
}

@Composable
fun TypeToggle(type: TxType, onChange: (TxType) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(TxType.EXPENSE to "支出", TxType.INCOME to "収入")
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (t, label) ->
            SegmentedButton(
                selected = type == t,
                onClick = { onChange(t) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}
