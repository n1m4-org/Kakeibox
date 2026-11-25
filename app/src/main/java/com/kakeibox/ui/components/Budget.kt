package com.kakeibox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.WarningColor

/** 予算の使用率バー（80%で黄、100%以上で赤） */
@Composable
fun BudgetProgress(ratio: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp),
        color = when {
            ratio >= 1f -> ExpenseColor
            ratio >= 0.8f -> WarningColor
            else -> MaterialTheme.colorScheme.primary
        },
    )
}

/**
 * 予算額の入力ダイアログ。
 * @param preview 入力中の金額から補足表示（今月の予算など）を作る。0のときは呼ばれない。
 */
@Composable
fun BudgetDialog(
    title: String,
    current: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    description: String? = null,
    preview: ((Long) -> String)? = null,
) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    val value = text.toLongOrNull() ?: 0L
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
                    onValueChange = { v -> text = v.filter(Char::isDigit).trimStart('0').take(9) },
                    label = { Text("金額（円）・空欄で解除") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (preview != null && value > 0) {
                    Text(preview(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
