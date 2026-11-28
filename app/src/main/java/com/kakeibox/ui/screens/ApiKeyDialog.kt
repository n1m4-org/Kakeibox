package com.kakeibox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** レシート読み取り（AI）の API キーの登録・削除 */
@Composable
fun ApiKeyDialog(masked: String?, onSave: (String) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var key by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("レシート読み取り（AI）") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "レシートの画像と端末で読み取った文字を Anthropic の Claude に送り、" +
                        "日付・店名・カテゴリごとの金額にしてもらいます。\n" +
                        "Anthropic のコンソールで作成した API キーを入力してください。" +
                        "利用料は API キーの持ち主に請求されます（1枚あたり数円程度）。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (masked != null) Text("登録済み: $masked", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text(if (masked == null) "API キー" else "新しい API キー") },
                    placeholder = { Text("sk-ant-…") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "キーは端末内で暗号化して保存し、バックアップには含めません。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(key) }, enabled = key.length >= 20) { Text("保存") } },
        dismissButton = {
            if (masked != null) TextButton(onClick = onClear) { Text("削除") }
            else TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
