package com.kakeibox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.DonutChart
import com.kakeibox.ui.components.MonthSelector
import com.kakeibox.ui.icon
import com.kakeibox.ui.yen

@Composable
fun ReportScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val period by vm.period.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }

    val items = if (type == TxType.EXPENSE) summary.expenseByCategory else summary.incomeByCategory
    val total = if (type == TxType.EXPENSE) summary.expense else summary.income

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { MonthSelector(period, vm::prevMonth, vm::nextMonth, vm::thisMonth) }
        item {
            TypeToggle(type, onChange = { type = it }, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DonutChart(items, total) }
            Spacer(Modifier.height(16.dp))
        }
        if (items.isEmpty()) {
            item {
                Text("データがありません", Modifier.fillMaxWidth().padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(items, key = { it.category.name }) { ct ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(6.dp, 28.dp).background(Color(ct.category.color), CircleShape))
                CategoryBadge(ct.category, size = 32.dp)
                Text(ct.category.label, Modifier.weight(1f))
                Text("%.1f%%".format(ct.ratio * 100), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(yen(ct.total), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (type == TxType.EXPENSE && summary.expenseByPayment.isNotEmpty()) {
            item {
                Text(
                    "支払い方法別",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                )
            }
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
