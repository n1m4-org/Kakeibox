package com.kakeibox.ui.screens

import com.kakeibox.ui.theme.AcrylicPanel
import com.kakeibox.ui.theme.panelPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.Category
import com.kakeibox.data.Recurring
import com.kakeibox.data.TxType
import com.kakeibox.data.nextDate
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.CategoryChips
import com.kakeibox.ui.components.PaymentChips
import com.kakeibox.ui.dayLabel
import com.kakeibox.ui.icon
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen
import java.time.LocalDate

/** 定期の収支の一覧 */
@Composable
fun RecurringScreen(vm: MainViewModel, onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val list by vm.recurring.collectAsStateWithLifecycle()
    RecurringContent(list, LocalDate.now(), onBack, onEdit, onToggle = { vm.saveRecurring(it) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecurringContent(
    list: List<Recurring>,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: (Long?) -> Unit,
    onToggle: (Recurring) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定期の収支") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(null) }) { Icon(Icons.Outlined.Add, "定期の収支を追加") }
        },
    ) { padding ->
        AcrylicPanel(Modifier.padding(padding).panelPadding()) {
            LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "家賃・サブスク・給料など、毎月決まった日に自動で明細に記録します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (list.isEmpty()) {
                item {
                    Text("まだ登録されていません", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
                }
            }
            items(list, key = { it.id }) { r ->
                val income = r.type == TxType.INCOME
                ListItem(
                    modifier = Modifier.clickable { onEdit(r.id) },
                    leadingContent = { CategoryBadge(r.categoryEnum) },
                    headlineContent = { Text(r.name) },
                    supportingContent = {
                        Column {
                            Text(if (r.enabled) "${r.dayLabel}・次回 ${r.nextDate(today).dayLabel()}" else "${r.dayLabel}・停止中")
                            // 支払い方法は2行目に（明細の行と同じくアイコン＋名前）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(r.payment.icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" " + r.payment.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                (if (income) "+" else "-") + yen(r.amount),
                                color = if (income) IncomeColor else ExpenseColor,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Switch(checked = r.enabled, onCheckedChange = { onToggle(r.copy(enabled = it)) })
                        }
                    },
                )
            }
        }
        }
    }
}

/** 定期の収支の追加・編集 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditScreen(vm: MainViewModel, id: Long?, onDone: () -> Unit) {
    var loaded by rememberSaveable { mutableStateOf(id == null) }
    // 画面回転でも失われないよう、元の設定のうち画面で編集しない値も保存しておく
    var originalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var processedUntil by rememberSaveable { mutableStateOf<Long?>(null) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var category by rememberSaveable { mutableStateOf(Category.HOUSING) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf(vm.settings.value.defaultPayment) }
    var day by rememberSaveable { mutableStateOf(LocalDate.now().dayOfMonth) }
    var memo by rememberSaveable { mutableStateOf("") }
    var dayMenu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && !loaded) {
            vm.findRecurring(id)?.let { r ->
                originalId = r.id; startDate = r.startDate; processedUntil = r.processedUntil; enabled = r.enabled
                name = r.name; type = r.type; category = r.categoryEnum; amountText = r.amount.toString()
                payment = r.payment; day = r.dayOfMonth; memo = r.memo
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "定期の収支を追加" else "定期の収支を編集") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                actions = {
                    if (originalId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "削除") }
                },
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        AcrylicPanel(Modifier.padding(padding).imePadding().panelPadding()) {
            Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeToggle(type, onChange = {
                if (it != type) { type = it; category = Category.of(it).first() }
            })
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(20); error = false },
                label = { Text("名前（例：家賃、動画サブスク）") }, singleLine = true,
                isError = error && name.isBlank(), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { v -> amountText = v.filter(Char::isDigit).trimStart('0').take(9); error = false },
                label = { Text("金額（円）") }, prefix = { Text("¥") }, singleLine = true,
                isError = error && (amountText.toLongOrNull() ?: 0) <= 0,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("記録する日", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { dayMenu = true }) { Text(dayOptionLabel(day)) }
                    DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        (1..31).forEach { d ->
                            DropdownMenuItem(text = { Text(dayOptionLabel(d)) }, onClick = { day = d; dayMenu = false })
                        }
                    }
                }
            }
            CategoryChips(type, category, onSelect = { category = it })
            PaymentChips(type, payment, onSelect = { payment = it })
            OutlinedTextField(
                value = memo, onValueChange = { memo = it.take(100) },
                label = { Text("メモ（空欄なら名前を記録）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "登録した日以降、記録日になると自動で明細に追加されます。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0
                    if (name.isBlank() || amount <= 0) {
                        error = true
                        return@Button
                    }
                    vm.saveRecurring(
                        Recurring(
                            id = originalId ?: 0, name = name.trim(), type = type, category = category.name,
                            amount = amount, payment = payment, memo = memo.trim(), dayOfMonth = day,
                            startDate = startDate, processedUntil = processedUntil, enabled = enabled,
                        )
                    )
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("保存") }
        }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("削除しますか？") },
            text = { Text("これまでに記録された明細は残ります。") },
            confirmButton = {
                TextButton(onClick = {
                    originalId?.let { vm.deleteRecurringById(it) }
                    confirmDelete = false
                    onDone()
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
        )
    }
}

private fun dayOptionLabel(d: Int) = if (d >= 31) "毎月末" else "毎月${d}日"
