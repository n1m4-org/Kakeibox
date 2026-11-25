package com.kakeibox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.Period
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetDialog
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.monthlyBudgetLabel

const val CARRYOVER_RULE =
    "使わなかった分は翌日に繰り越され、使いすぎた分は翌日の予算から差し引かれます（新しい月の開始日にリセット）。" +
        "金額を変更すると、繰り越しは今日から数え直します。"

/** 全体・カテゴリごとの1日予算の設定画面 */
@Composable
fun DailyBudgetScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.dailyBudget.collectAsStateWithLifecycle()
    val period by vm.currentPeriod.collectAsStateWithLifecycle()
    DailyBudgetContent(settings, period, onBack, onSave = vm::setDailyBudget)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyBudgetContent(
    settings: Map<BudgetTarget, DailyBudgetSetting>,
    period: Period,
    onBack: () -> Unit,
    onSave: (BudgetTarget, Long) -> Unit,
) {
    var editing by remember { mutableStateOf<BudgetTarget?>(null) }
    val categoryTargets = BudgetTarget.entries.filterIsInstance<BudgetTarget.Of>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("1日の予算") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item {
                Text(
                    CARRYOVER_RULE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { SectionHeader("全体（すべての支出）") }
            item { TargetRow(BudgetTarget.All, settings[BudgetTarget.All], period) { editing = BudgetTarget.All } }
            item { SectionHeader("カテゴリ別") }
            items(categoryTargets, key = { it.key }) { t ->
                TargetRow(t, settings[t], period) { editing = t }
            }
            val dailySum = categoryTargets.sumOf { settings[it]?.amount ?: 0L }
            if (dailySum > 0) {
                item {
                    HorizontalDivider()
                    Text(
                        "カテゴリ合計　${monthlyBudgetLabel(dailySum, period)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
    }

    editing?.let { target ->
        BudgetDialog(
            title = "${target.label}の1日の予算",
            description = CARRYOVER_RULE,
            current = settings[target]?.amount ?: 0L,
            preview = { monthlyBudgetLabel(it, period) },
            onDismiss = { editing = null },
            onSave = { onSave(target, it); editing = null },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun TargetRow(target: BudgetTarget, setting: DailyBudgetSetting?, period: Period, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { TargetBadge(target) },
        headlineContent = { Text(target.label) },
        supportingContent = {
            Text(
                if (setting != null) monthlyBudgetLabel(setting.amount, period) else "未設定",
                color = if (setting != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Icon(Icons.Outlined.Edit, "${target.label}の1日の予算を設定") },
    )
}
