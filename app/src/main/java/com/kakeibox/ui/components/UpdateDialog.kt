package com.kakeibox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kakeibox.BuildConfig
import com.kakeibox.update.ReleaseInfo
import com.kakeibox.update.Updater
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 新しいバージョンの内容と［更新］。ダウンロード中は進捗を表示し、終わったら Android の更新画面を開く */
@Composable
fun UpdateDialog(info: ReleaseInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    // 開発用（デバッグ）の版は署名が違うため上書きできない
    val debugBuild = BuildConfig.DEBUG

    AlertDialog(
        onDismissRequest = { job?.cancel(); onDismiss() },
        title = { Text("新しいバージョン ${info.versionName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("今のバージョン：${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                if (info.notes.isNotBlank()) {
                    Text(
                        info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    )
                }
                if (debugBuild) {
                    Text(
                        "今入っているのは開発用の版のため、そのままでは上書きできません。" +
                            "設定の「バックアップを保存」→ アンインストール → 新しい版をインストール →「バックアップから復元」の順で切り替えてください。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                progress?.let { p ->
                    Text("ダウンロード中… ${(p * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            if (debugBuild) {
                TextButton(onClick = { Updater.openReleasesPage(context); onDismiss() }) { Text("ダウンロードページを開く") }
            } else {
                TextButton(
                    enabled = progress == null,
                    onClick = {
                        if (!Updater.canInstall(context)) {
                            error = "「このアプリからのインストールを許可」をオンにしてから、もう一度［更新］を押してください。"
                            Updater.openInstallPermissionSettings(context)
                            return@TextButton
                        }
                        error = null
                        progress = 0f
                        job = scope.launch {
                            runCatching { Updater.download(context, info) { progress = it } }
                                .onSuccess { file -> progress = null; Updater.install(context, file); onDismiss() }
                                .onFailure { e -> progress = null; error = "ダウンロードできませんでした（${e.message}）" }
                        }
                    },
                ) { Text("更新") }
            }
        },
        dismissButton = { TextButton(onClick = { job?.cancel(); onDismiss() }) { Text("あとで") } },
    )
}
