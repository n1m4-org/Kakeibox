package com.kakeibox.ui.screens

import com.kakeibox.ui.theme.AcrylicPanel
import com.kakeibox.ui.theme.panelPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.SearchFilter
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.TransactionRow
import com.kakeibox.ui.fullLabel
import com.kakeibox.ui.categoryConfig
import com.kakeibox.ui.icon
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val filter by vm.searchFilter.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    SearchContent(filter, results, onBack, onOpen, onUpdate = vm::updateSearch)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchContent(
    filter: SearchFilter,
    results: List<Transaction>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onUpdate: ((SearchFilter) -> SearchFilter) -> Unit,
    autoFocus: Boolean = true,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var pickingFrom by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                title = {
                    TextField(
                        value = filter.keyword,
                        onValueChange = { v -> onUpdate { it.copy(keyword = v) } },
                        placeholder = { Text("メモ・カテゴリで検索") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                actions = {
                    if (!filter.isEmpty) {
                        IconButton(onClick = { onUpdate { SearchFilter() } }) { Icon(Icons.Outlined.Clear, "条件をクリア") }
                    }
                },
            )
        },
    ) { padding ->
        AcrylicPanel(Modifier.padding(padding).imePadding().panelPadding()) {
            LazyColumn(Modifier.fillMaxSize()) {
            item {
                TextButton(onClick = { showFilters = !showFilters }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.Outlined.FilterList, null)
                    Text(" 絞り込み" + if (activeFilterCount(filter) > 0) "（${activeFilterCount(filter)}）" else "")
                    Icon(if (showFilters) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
            }
            if (showFilters) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("種別", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(null to "すべて", TxType.EXPENSE to "支出", TxType.INCOME to "収入").forEach { (t, label) ->
                                FilterChip(selected = filter.type == t, onClick = { onUpdate { it.copy(type = t) } }, label = { Text(label) })
                            }
                        }
                        Text("カテゴリ（複数可）", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cfg = categoryConfig()
                            (cfg.ordered(TxType.EXPENSE) + cfg.ordered(TxType.INCOME))
                                .filter { filter.type == null || it.type == filter.type }.forEach { c ->
                                val selected = c in filter.categories
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdate { it.copy(categories = if (selected) it.categories - c else it.categories + c) } },
                                    label = { Text(if (c == Category.OTHER_INCOME) "その他(収入)" else c.label) },
                                    leadingIcon = { Icon(c.icon, null, Modifier.size(FilterChipDefaults.IconSize), tint = Color(c.color)) },
                                )
                            }
                        }
                        Text("支払い方法（複数可）", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethod.entries.forEach { p ->
                                val selected = p in filter.payments
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdate { it.copy(payments = if (selected) it.payments - p else it.payments + p) } },
                                    label = { Text(p.label) },
                                    leadingIcon = { Icon(p.icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
                                )
                            }
                        }
                        Text("金額", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AmountField(filter.minAmount, "下限", Modifier.weight(1f)) { v -> onUpdate { it.copy(minAmount = v) } }
                            Text("〜")
                            AmountField(filter.maxAmount, "上限", Modifier.weight(1f)) { v -> onUpdate { it.copy(maxAmount = v) } }
                        }
                        Text("期間", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickingFrom = true }, modifier = Modifier.weight(1f)) {
                                Text(filter.from?.fullLabel() ?: "開始日")
                            }
                            Text("〜")
                            OutlinedButton(onClick = { pickingFrom = false }, modifier = Modifier.weight(1f)) {
                                Text(filter.to?.fullLabel() ?: "終了日")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            if (!filter.isEmpty) {
                item {
                    val expense = results.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                    val income = results.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${results.size}件", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (expense > 0) Text("支出 ${yen(expense)}", color = ExpenseColor)
                        if (income > 0) Text("収入 ${yen(income)}", color = IncomeColor)
                    }
                }
                if (results.isEmpty()) {
                    item {
                        Text("見つかりませんでした", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
                    }
                }
                items(results, key = { it.id }) { tx ->
                    TransactionRow(tx, onClick = { onOpen(tx.id) }, showDate = true)
                    HorizontalDivider()
                }
            } else {
                item {
                    Text(
                        "キーワードを入力するか、絞り込み条件を選んでください",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
        }
    }

    pickingFrom?.let { isFrom ->
        val initial = (if (isFrom) filter.from else filter.to) ?: LocalDate.now()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { pickingFrom = null },
            confirmButton = {
                TextButton(onClick = {
                    val date = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    onUpdate { if (isFrom) it.copy(from = date) else it.copy(to = date) }
                    pickingFrom = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdate { if (isFrom) it.copy(from = null) else it.copy(to = null) }
                    pickingFrom = null
                }) { Text("指定しない") }
            },
        ) { DatePicker(state) }
    }
}

private fun activeFilterCount(f: SearchFilter) = listOf(
    f.type != null, f.categories.isNotEmpty(), f.payments.isNotEmpty(),
    f.minAmount != null || f.maxAmount != null, f.from != null || f.to != null,
).count { it }

@Composable
private fun AmountField(value: Long?, label: String, modifier: Modifier, onChange: (Long?) -> Unit) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(9).toLongOrNull()) },
        label = { Text(label) },
        prefix = { Text("¥") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
