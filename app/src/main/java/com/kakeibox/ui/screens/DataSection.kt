package com.kakeibox.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kakeibox.data.BackupData
import com.kakeibox.data.Csv
import com.kakeibox.data.DataTransfer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 設定画面の「データ」の操作 */
class DataActions(
    val exportCsv: () -> Unit = {},
    val importCsv: () -> Unit = {},
    val backup: () -> Unit = {},
    val restore: () -> Unit = {},
)

/** ファイルの選択（SAF）と確認ダイアログを持つ DataActions を作る */
@Composable
fun rememberDataActions(): DataActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var csvPreview by remember { mutableStateOf<Csv.ImportResult?>(null) }
    var backupPreview by remember { mutableStateOf<BackupData?>(null) }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { DataTransfer.exportCsv(context, uri) }
                .onSuccess { toast("${it}件をCSVに書き出しました") }
                .onFailure { toast("書き出せませんでした：${it.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { DataTransfer.readCsv(context, uri) }
                .onSuccess { csvPreview = it }
                .onFailure { toast("読み込めませんでした：${it.message}") }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { DataTransfer.writeBackup(context, uri) }
                .onSuccess { toast("バックアップを保存しました（明細${it.transactions.size}件）") }
                .onFailure { toast("保存できませんでした：${it.message}") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { DataTransfer.readBackup(context, uri) }
                .onSuccess { backupPreview = it }
                .onFailure { toast(it.message ?: "読み込めませんでした") }
        }
    }

    csvPreview?.let { r ->
        AlertDialog(
            onDismissRequest = { csvPreview = null },
            title = { Text("CSVの取り込み") },
            text = {
                Text(
                    "${r.transactions.size}件の記録を追加します。" +
                        (if (r.skippedLines.isNotEmpty()) "\n読み込めない行（${r.skippedLines.take(10).joinToString("、")}行目" +
                            (if (r.skippedLines.size > 10) " ほか" else "") + "）はスキップします。" else "") +
                        "\n※ 既存の記録はそのまま残ります（同じファイルを2回取り込むと重複します）。"
                )
            },
            confirmButton = {
                TextButton(enabled = r.transactions.isNotEmpty(), onClick = {
                    csvPreview = null
                    scope.launch {
                        DataTransfer.importTransactions(context, r.transactions)
                        toast("${r.transactions.size}件を取り込みました")
                    }
                }) { Text("取り込む") }
            },
            dismissButton = { TextButton(onClick = { csvPreview = null }) { Text("キャンセル") } },
        )
    }
    backupPreview?.let { b ->
        AlertDialog(
            onDismissRequest = { backupPreview = null },
            title = { Text("バックアップから復元") },
            text = {
                Text(
                    "明細${b.transactions.size}件・テンプレート${b.templates.size}件・定期の収支${b.recurring.size}件と設定を復元します。" +
                        "\n現在のデータと設定はすべて置き換えられ、元に戻せません。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    backupPreview = null
                    scope.launch {
                        runCatching { DataTransfer.restore(context, b) }
                            .onSuccess { toast("復元しました") }
                            .onFailure { toast("復元できませんでした：${it.message}") }
                    }
                }) { Text("置き換えて復元") }
            },
            dismissButton = { TextButton(onClick = { backupPreview = null }) { Text("キャンセル") } },
        )
    }

    return DataActions(
        exportCsv = { exportLauncher.launch("kakeibox_$today.csv") },
        importCsv = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
        backup = { backupLauncher.launch("kakeibox_backup_$today.json") },
        restore = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*")) },
    )
}
