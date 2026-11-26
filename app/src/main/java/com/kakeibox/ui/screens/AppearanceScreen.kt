package com.kakeibox.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.AppSettings
import com.kakeibox.data.BudgetTarget
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetProgress
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.theme.AcrylicCard
import com.kakeibox.ui.theme.BackgroundImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 背景（画像・ぼかし・明るさ）の設定。後ろのアプリの背景がそのままプレビューになる */
@Composable
fun AppearanceScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching { BackgroundImage.import(context, uri) }.getOrDefault(false)
            if (ok) vm.updateSettings { it.copy(backgroundVersion = System.currentTimeMillis()) }
            else Toast.makeText(context, "画像を読み込めませんでした", Toast.LENGTH_LONG).show()
        }
    }
    AppearanceContent(
        settings = settings,
        onBack = onBack,
        onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onReset = {
            BackgroundImage.delete(context)
            vm.updateSettings { it.copy(backgroundVersion = 0) }
        },
        onUpdate = vm::updateSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onReset: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("背景") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 見本のカード（実際のアクリルの見え方）
            AcrylicCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TargetBadge(BudgetTarget.All, size = 28.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("見本", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("残り ¥1,500", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    BudgetProgress(0.4f)
                }
            }
            // 背景が見えるように少し空ける
            Spacer(Modifier.height(140.dp))

            AcrylicCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("画像", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPick) {
                            Icon(Icons.Outlined.Image, null)
                            Text(" 画像を選ぶ")
                        }
                        OutlinedButton(onClick = onReset, enabled = settings.backgroundVersion != 0L) {
                            Icon(Icons.Outlined.RestartAlt, null)
                            Text(" 既定に戻す")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    SliderRow(
                        label = "ぼかし",
                        value = settings.backgroundBlur.toFloat(),
                        range = 0f..100f,
                        valueText = "${settings.backgroundBlur}",
                        onChange = { v -> onUpdate { it.copy(backgroundBlur = v.roundToInt()) } },
                    )
                    SliderRow(
                        label = "明るさ",
                        value = settings.backgroundBrightness.toFloat(),
                        range = -100f..100f,
                        valueText = settings.backgroundBrightness.let { if (it > 0) "+$it" else "$it" },
                        onChange = { v -> onUpdate { it.copy(backgroundBrightness = v.roundToInt()) } },
                        start = { Icon(Icons.Outlined.BrightnessLow, "暗く") },
                        end = { Icon(Icons.Outlined.BrightnessHigh, "明るく") },
                    )
                    TextButton(
                        onClick = { onUpdate { it.copy(backgroundBlur = AppSettings().backgroundBlur, backgroundBrightness = 0) } },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("ぼかし・明るさを戻す") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            start?.invoke()
            Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
            end?.invoke()
        }
    }
}
