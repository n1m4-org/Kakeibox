package com.kakeibox.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kakeibox.AppGraph
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.Period
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.monthlyBudgetLabel
import com.kakeibox.ui.theme.KakeiboxTheme
import kotlinx.coroutines.launch

/** 「今日の予算」ウィジェットの配置時（と再設定時）に、表示する予算対象を選ぶ画面 */
class DailyBudgetWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // 戻るで閉じたときは配置をキャンセル
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val glanceManager = GlanceAppWidgetManager(this)
        var current by mutableStateOf<String?>(null)
        lifecycleScope.launch {
            val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
            current = getAppWidgetState(this@DailyBudgetWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)[DailyBudgetWidget.TARGET_KEY]
        }

        setContent {
            KakeiboxTheme {
                val settings by AppGraph.budgetStore(this).daily.collectAsStateWithLifecycle()
                ConfigContent(settings, current, AppGraph.settingsStore(this).currentPeriod()) { target ->
                    lifecycleScope.launch {
                        val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(this@DailyBudgetWidgetConfigActivity, glanceId) {
                            it[DailyBudgetWidget.TARGET_KEY] = target.key
                        }
                        DailyBudgetWidget().update(this@DailyBudgetWidgetConfigActivity, glanceId)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigContent(
    settings: Map<BudgetTarget, DailyBudgetSetting>,
    currentKey: String?,
    period: Period,
    onSelect: (BudgetTarget) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("ウィジェットに表示する予算") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item {
                Text(
                    "未設定の予算も選べます。金額はアプリの「1日の予算」から設定できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(BudgetTarget.entries, key = { it.key }) { target ->
                val setting = settings[target]
                ListItem(
                    modifier = Modifier.clickable { onSelect(target) },
                    leadingContent = { TargetBadge(target) },
                    headlineContent = { Text(target.label) },
                    supportingContent = {
                        Text(if (setting != null) monthlyBudgetLabel(setting.amount, period) else "未設定")
                    },
                    trailingContent = { RadioButton(selected = target.key == currentKey, onClick = { onSelect(target) }) },
                )
            }
        }
    }
}
