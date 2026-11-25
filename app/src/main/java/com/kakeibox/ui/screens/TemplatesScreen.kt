package com.kakeibox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.Template
import com.kakeibox.data.TxType
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.yen

/** テンプレート（よく使う記録）の一覧・編集・削除 */
@Composable
fun TemplatesScreen(vm: MainViewModel, onBack: () -> Unit, onCreate: () -> Unit) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    TemplatesContent(templates, onBack, onCreate, onSave = { vm.saveTemplate(it) }, onDelete = { vm.deleteTemplate(it) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesContent(
    templates: List<Template>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onSave: (Template) -> Unit,
    onDelete: (Template) -> Unit,
) {
    var editing by remember { mutableStateOf<Template?>(null) }
    var deleting by remember { mutableStateOf<Template?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("テンプレート") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("記録から作成") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item {
                Text(
                    "クイック入力の上部に表示され、タップするだけで今日の記録になります。" +
                        "入力画面の右上「テンプレートとして保存」からも追加できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (templates.isEmpty()) {
                item {
                    Text(
                        "テンプレートはまだありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(templates, key = { it.id }) { t ->
                ListItem(
                    modifier = Modifier.clickable { editing = t },
                    leadingContent = { CategoryBadge(t.categoryEnum) },
                    headlineContent = { Text(t.name) },
                    supportingContent = { Text(templateSummary(t)) },
                    trailingContent = {
                        IconButton(onClick = { deleting = t }) { Icon(Icons.Outlined.Delete, "削除") }
                    },
                )
            }
        }
    }

    editing?.let { t ->
        TemplateDialog(
            initialName = t.name,
            initialAmount = t.amount,
            title = "テンプレートを編集",
            onDismiss = { editing = null },
            onSave = { name, amount -> onSave(t.copy(name = name, amount = amount)); editing = null },
        )
    }
    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("「${t.name}」を削除しますか？") },
            confirmButton = { TextButton(onClick = { onDelete(t); deleting = null }) { Text("削除") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("キャンセル") } },
        )
    }
}

/** 「¥800・食費・現金・メモ」 */
internal fun templateSummary(t: Template): String = buildList {
    add(if (t.amount > 0) yen(t.amount) else "金額はその都度入力")
    add((if (t.type == TxType.INCOME) "収入：" else "") + t.categoryEnum.label)
    add(t.payment.label)
    if (t.memo.isNotBlank()) add(t.memo)
}.joinToString("・")

/** テンプレートの名前と金額の入力ダイアログ（金額は空欄でその都度入力） */
@Composable
fun TemplateDialog(
    initialName: String,
    initialAmount: Long,
    title: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    label = { Text("名前") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter(Char::isDigit).trimStart('0').take(9) },
                    label = { Text("金額（空欄でその都度入力）") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), amount.toLongOrNull() ?: 0L) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
