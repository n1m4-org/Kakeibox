package com.kakeibox.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakeibox.AppGraph
import com.kakeibox.data.Category
import com.kakeibox.data.TxType
import com.kakeibox.receipt.EditLine
import com.kakeibox.receipt.ReceiptInbox
import com.kakeibox.receipt.ReceiptState
import com.kakeibox.receipt.ReceiptViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.components.PaymentChips
import com.kakeibox.ui.fullLabel
import com.kakeibox.ui.theme.AcrylicPanel
import com.kakeibox.ui.theme.panelPadding
import com.kakeibox.ui.yen
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

private const val MILLIS_PER_DAY = 86_400_000L

/** レシートの画像を読み取り、カテゴリごとの明細にする画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(onBack: () -> Unit, rvm: ReceiptViewModel = viewModel()) {
    val state by rvm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyStore = remember { AppGraph.apiKeyStore(context) }
    val hasKey by keyStore.hasKey.collectAsStateWithLifecycle()
    var editKey by rememberSaveable { mutableStateOf(false) }
    if (editKey) {
        ApiKeyDialog(
            masked = if (hasKey) keyStore.masked() else null,
            onSave = { keyStore.set(it); editKey = false },
            onClear = { keyStore.clear(); editKey = false },
            onDismiss = { editKey = false },
        )
    }

    // 他のアプリから共有された画像
    val shared by ReceiptInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(shared) { if (shared.isNotEmpty()) rvm.start(ReceiptInbox.take()) }

    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri
        if (ok && uri != null) rvm.start(listOf(Uri.parse(uri)))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        rvm.start(uris)
    }
    val pickers = Pickers(
        camera = {
            val uri = newCaptureUri(context)
            captureUri = uri.toString()
            camera.launch(uri)
        },
        gallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("レシートを読み取る") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
    ) { padding ->
        AcrylicPanel(Modifier.padding(padding).imePadding().panelPadding()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val s = state) {
                    ReceiptState.Pick -> PickContent(hasKey, pickers, onRegisterKey = { editKey = true })
                    is ReceiptState.Working -> WorkingContent(s)
                    is ReceiptState.Failed -> FailedContent(s, onRetry = rvm::retry, onSkip = rvm::skip, onReset = rvm::reset)
                    is ReceiptState.Review -> ReviewContent(s, onEdit = rvm::edit, onSave = rvm::save, onSkip = rvm::skip, onRetry = rvm::retry)
                    is ReceiptState.Done -> DoneContent(s, onMore = rvm::reset, onBack = onBack)
                }
            }
        }
    }
}

private class Pickers(val camera: () -> Unit, val gallery: () -> Unit)

/** 撮影した画像の保存先（アプリのキャッシュ。共有用の FileProvider 経由でカメラアプリに渡す） */
private fun newCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

@Composable
private fun PickContent(hasKey: Boolean, pickers: Pickers, onRegisterKey: () -> Unit) {
    Text(
        "レシートを撮影するか、写真を選んでください。日付・店名・カテゴリごとの金額を読み取り、確認してから保存します。",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!hasKey) {
        Text(
            "読み取りには Anthropic の API キーが必要です（あとから 設定 → レシート読み取り（AI）の API キー で変更できます）。",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRegisterKey, modifier = Modifier.fillMaxWidth()) { Text("API キーを登録する") }
        return
    }
    Button(onClick = pickers.camera, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Icon(Icons.Outlined.PhotoCamera, null)
        Spacer(Modifier.width(8.dp))
        Text("カメラで撮影")
    }
    OutlinedButton(onClick = pickers.gallery, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Icon(Icons.Outlined.PhotoLibrary, null)
        Spacer(Modifier.width(8.dp))
        Text("ギャラリーから選ぶ（複数可）")
    }
    Text(
        "ほかのアプリ（ギャラリーなど）の「共有」から Kakeibox を選んでも読み取れます。\n" +
            "画像と読み取った文字は Anthropic に送信されます。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Thumb(thumb: android.graphics.Bitmap?) {
    if (thumb == null) return
    Image(
        thumb.asImageBitmap(),
        contentDescription = "レシート",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun WorkingContent(s: ReceiptState.Working) {
    if (s.position.isNotEmpty()) Text(s.position, style = MaterialTheme.typography.labelLarge)
    Thumb(s.thumb)
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
        Spacer(Modifier.width(12.dp))
        Text(s.step)
    }
}

@Composable
private fun FailedContent(s: ReceiptState.Failed, onRetry: () -> Unit, onSkip: () -> Unit, onReset: () -> Unit) {
    if (s.position.isNotEmpty()) Text(s.position, style = MaterialTheme.typography.labelLarge)
    Thumb(s.thumb)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text(s.message, color = MaterialTheme.colorScheme.error)
    }
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("もう一度読み取る") }
    if (s.position.isNotEmpty()) OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("この画像を飛ばす") }
    TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("別の画像を選ぶ") }
}

@Composable
private fun DoneContent(s: ReceiptState.Done, onMore: () -> Unit, onBack: () -> Unit) {
    Text(if (s.saved > 0) "${s.saved}件の明細を保存しました" else "保存した明細はありません", style = MaterialTheme.typography.titleMedium)
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("明細に戻る") }
    OutlinedButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) { Text("続けて読み取る") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewContent(
    s: ReceiptState.Review,
    onEdit: ((ReceiptState.Review) -> ReceiptState.Review) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
) {
    var showDate by rememberSaveable { mutableStateOf(false) }

    if (s.position.isNotEmpty()) Text("レシート ${s.position}", style = MaterialTheme.typography.labelLarge)
    Thumb(s.thumb)

    OutlinedTextField(
        value = s.store,
        onValueChange = { v -> onEdit { it.copy(store = v.take(60)) } },
        label = { Text("店名（メモに入ります）") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CalendarMonth, null)
        Text("  " + s.date.fullLabel())
    }
    PaymentChips(TxType.EXPENSE, s.payment, onSelect = { p -> onEdit { it.copy(payment = p) } })

    Text("カテゴリごとの金額", style = MaterialTheme.typography.labelLarge)
    s.lines.forEachIndexed { i, line ->
        LineRow(
            line,
            onChange = { new -> onEdit { r -> r.copy(lines = r.lines.toMutableList().also { it[i] = new }) } },
            onRemove = if (s.lines.size > 1) {
                { onEdit { r -> r.copy(lines = r.lines.toMutableList().also { it.removeAt(i) }) } }
            } else null,
        )
    }
    TextButton(onClick = { onEdit { it.copy(lines = it.lines + EditLine(Category.OTHER_EXPENSE.key, "")) } }) {
        Icon(Icons.Outlined.Add, null)
        Text(" 行を追加")
    }

    Text(
        "合計 ${yen(s.sum)}" + if (s.total > 0) "（レシート ${yen(s.total)}）" else "",
        style = MaterialTheme.typography.titleMedium,
    )
    if (s.mismatch) {
        Text(
            "レシートの合計と ${yen(kotlin.math.abs(s.total - s.sum))} 違います。金額を確認してください",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Button(onClick = onSave, enabled = s.savable, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("${s.lines.count { it.amount > 0 }}件を保存")
    }
    if (s.position.isNotEmpty()) {
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("このレシートは保存しない") }
    }
    TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("読み取り直す") }

    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = s.date.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        onEdit { it.copy(date = d) }
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("キャンセル") } },
        ) { DatePicker(picker) }
    }
}

/** 1行：カテゴリ（タップで選択）・金額・品名・削除 */
@Composable
private fun LineRow(line: EditLine, onChange: (EditLine) -> Unit, onRemove: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    val category = Category.fromName(line.categoryKey, TxType.EXPENSE)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { menu = true }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(category, size = 32.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(category.label, modifier = Modifier.width(76.dp), maxLines = 1)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    Category.of(TxType.EXPENSE).forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.label) },
                            leadingIcon = { CategoryBadge(c, size = 24.dp) },
                            onClick = { onChange(line.copy(categoryKey = c.key)); menu = false },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = line.amountText,
                onValueChange = { v -> onChange(line.copy(amountText = v.filter(Char::isDigit).trimStart('0').take(9))) },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "この行を削除") }
        }
        if (line.items.isNotEmpty()) {
            Text(
                line.items.joinToString("・"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 42.dp),
                maxLines = 2,
            )
        }
    }
}
