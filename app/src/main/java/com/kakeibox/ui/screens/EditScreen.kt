package com.kakeibox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kakeibox.data.Category
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.fullLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(vm: MainViewModel, txId: Long?, onDone: () -> Unit) {
    var loaded by rememberSaveable { mutableStateOf(txId == null) }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var category by rememberSaveable { mutableStateOf(Category.FOOD) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var memo by rememberSaveable { mutableStateOf("") }
    var original by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var amountError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(txId) {
        if (txId != null && !loaded) {
            vm.find(txId)?.let { tx ->
                type = tx.type
                category = tx.categoryEnum
                amountText = tx.amount.toString()
                epochDay = tx.date
                memo = tx.memo
                original = tx.id
            }
            loaded = true
        }
    }

    fun buildTx() = Transaction(
        id = original ?: 0,
        type = type,
        category = category.name,
        amount = amountText.toLongOrNull() ?: 0,
        date = epochDay,
        memo = memo.trim(),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (txId == null) "記録を追加" else "記録を編集") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") }
                },
                actions = {
                    if (original != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "削除") }
                    }
                },
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeToggle(type, onChange = {
                if (it != type) {
                    type = it
                    category = Category.of(it).first()
                }
            })

            OutlinedTextField(
                value = amountText,
                onValueChange = { v ->
                    amountText = v.filter(Char::isDigit).trimStart('0').take(10)
                    amountError = false
                },
                label = { Text("金額（円）") },
                prefix = { Text("¥") },
                isError = amountError,
                supportingText = if (amountError) {{ Text("1円以上を入力してください") }} else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("カテゴリ", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.of(type).forEach { c ->
                    FilterChip(
                        selected = c == category,
                        onClick = { category = c },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CalendarMonth, null)
                Text("  " + LocalDate.ofEpochDay(epochDay).fullLabel())
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it.take(100) },
                label = { Text("メモ（任意）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val tx = buildTx()
                    if (tx.amount <= 0) {
                        amountError = true
                    } else {
                        vm.save(tx)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("保存") }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } },
        ) { DatePicker(state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("削除しますか？") },
            text = { Text("この記録を削除します。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(buildTx())
                    confirmDelete = false
                    onDone()
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
        )
    }
}
