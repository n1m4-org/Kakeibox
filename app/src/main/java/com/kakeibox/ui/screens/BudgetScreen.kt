package com.kakeibox.ui.screens

import com.kakeibox.ui.theme.AcrylicPanel
import com.kakeibox.ui.theme.panelPadding
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.BudgetKind
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Period
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.BudgetDialog
import com.kakeibox.ui.components.TargetBadge
import com.kakeibox.ui.monthlyBudgetLabel
import com.kakeibox.ui.categoryConfig
import com.kakeibox.data.TxType
import com.kakeibox.ui.yen

const val CARRYOVER_RULE =
    "使わなかった分は翌日に繰り越され、使いすぎた分は翌日の予算から差し引かれます（新しい月の開始日にリセット）。" +
        "金額を変更すると、繰り越しは今日から数え直します。"

private fun kindDescription(kind: BudgetKind) = when (kind) {
    BudgetKind.DAILY -> CARRYOVER_RULE
    BudgetKind.WEEKLY -> "月曜〜日曜の1週間で使える金額です。"
    BudgetKind.MONTHLY -> "設定した月の開始日から1か月で使える金額です。"
}

/** 全体・カテゴリごとの予算（1日／1週間／1か月）の設定画面 */
@Composable
fun BudgetScreen(vm: MainViewModel, initialKind: BudgetKind, onBack: () -> Unit) {
    val daily by vm.dailyBudget.collectAsStateWithLifecycle()
    val weekly by vm.weeklyBudget.collectAsStateWithLifecycle()
    val monthly by vm.monthlyBudget.collectAsStateWithLifecycle()
    val period by vm.currentPeriod.collectAsStateWithLifecycle()
    BudgetContent(
        amounts = mapOf(
            BudgetKind.DAILY to daily.mapValues { it.value.amount },
            BudgetKind.WEEKLY to weekly,
            BudgetKind.MONTHLY to monthly,
        ),
        period = period,
        initialKind = initialKind,
        onBack = onBack,
        onSave = vm::setBudget,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetContent(
    amounts: Map<BudgetKind, Map<BudgetTarget, Long>>,
    period: Period,
    initialKind: BudgetKind,
    onBack: () -> Unit,
    onSave: (BudgetKind, BudgetTarget, Long) -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var editing by remember { mutableStateOf<BudgetTarget?>(null) }
    val current = amounts[kind].orEmpty()
    // 表示中のカテゴリと、非表示でも予算が設定されているカテゴリ
    val cfg = categoryConfig()
    val categoryTargets = cfg.ordered(TxType.EXPENSE)
        .filter { !cfg.isHidden(it) || amounts.values.any { m -> m.containsKey(BudgetTarget.Of(it)) } }
        .map { BudgetTarget.Of(it) }

    fun summary(amount: Long): String = when (kind) {
        BudgetKind.DAILY -> monthlyBudgetLabel(amount, period)
        BudgetKind.WEEKLY -> "1週間 ${yen(amount)}"
        BudgetKind.MONTHLY -> "1か月 ${yen(amount)}（1日あたり ${yen(amount / period.days)}）"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予算の設定") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
    ) { padding ->
        AcrylicPanel(Modifier.padding(padding).panelPadding()) {
            LazyColumn(Modifier.fillMaxSize()) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BudgetKind.entries.forEachIndexed { i, k ->
                        SegmentedButton(
                            selected = kind == k,
                            onClick = { kind = k },
                            shape = SegmentedButtonDefaults.itemShape(i, BudgetKind.entries.size),
                        ) { Text(k.label) }
                    }
                }
            }
            item {
                Text(
                    kindDescription(kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { SectionHeader("全体（すべての支出）") }
            item { TargetRow(BudgetTarget.All, current[BudgetTarget.All]?.let(::summary)) { editing = BudgetTarget.All } }
            item { SectionHeader("カテゴリ別") }
            items(categoryTargets, key = { it.key }) { t ->
                TargetRow(t, current[t]?.let(::summary)) { editing = t }
            }
            val sum = categoryTargets.sumOf { current[it] ?: 0L }
            if (sum > 0) {
                item {
                    HorizontalDivider()
                    Text(
                        "カテゴリ合計　${summary(sum)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
        }
    }

    editing?.let { target ->
        BudgetDialog(
            title = "${target.label}の${kind.label}の予算",
            description = kindDescription(kind),
            current = current[target] ?: 0L,
            preview = { summary(it) },
            onDismiss = { editing = null },
            onSave = { onSave(kind, target, it); editing = null },
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
private fun TargetRow(target: BudgetTarget, summary: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { TargetBadge(target) },
        headlineContent = { Text(target.label) },
        supportingContent = {
            Text(
                summary ?: "未設定",
                color = if (summary != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Icon(Icons.Outlined.Edit, "${target.label}の予算を設定") },
    )
}
