package com.kakeibox.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.AppSettings
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Period
import com.kakeibox.ui.icon
import com.kakeibox.data.ThemeMode
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.ChoiceDialog
import com.kakeibox.ui.components.SettingsItem
import com.kakeibox.ui.components.SettingsSection
import com.kakeibox.ui.components.SettingsSwitch

/** 設定画面から開く画面 */
class SettingsNavigation(
    val openBudgets: () -> Unit,
    val openTemplates: () -> Unit,
    val openRecurring: () -> Unit,
)

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues, navigation: SettingsNavigation) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    SettingsContent(settings, contentPadding, navigation, onUpdate = vm::updateSettings)
}

private enum class SettingsDialog { THEME, PERIOD_START, PAYMENT, PAYDAY }

@Composable
internal fun SettingsContent(
    settings: AppSettings,
    contentPadding: PaddingValues,
    navigation: SettingsNavigation,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { SettingsSection("予算") }
        item {
            SettingsItem("予算の設定", Icons.Outlined.Savings, "全体・カテゴリごとの1日／1週間／1か月の予算", onClick = navigation.openBudgets)
        }

        item { SettingsSection("入力") }
        item {
            SettingsItem("既定の支払い方法", settings.defaultPayment.icon, settings.defaultPayment.label) {
                dialog = SettingsDialog.PAYMENT
            }
        }
        item {
            SettingsItem("テンプレート", Icons.Outlined.Bookmarks, "よく使う記録をワンタップで", onClick = navigation.openTemplates)
        }
        item {
            SettingsItem("定期の収支", Icons.Outlined.EventRepeat, "家賃・サブスク・給料などを毎月自動で記録", onClick = navigation.openRecurring)
        }

        item { SettingsSection("期間") }
        item {
            SettingsItem(
                "月の開始日（締め日）",
                Icons.Outlined.DateRange,
                periodStartLabel(settings.periodStartDay),
            ) { dialog = SettingsDialog.PERIOD_START }
        }

        item {
            SettingsItem("給料日", Icons.Outlined.Payments, paydayLabel(settings.payday) + if (settings.payday != 0) "（明細にカウントダウンを表示）" else "") {
                dialog = SettingsDialog.PAYDAY
            }
        }
        if (settings.payday != 0) {
            item {
                SettingsSwitch(
                    "土日の場合は前の金曜日",
                    Icons.Outlined.Weekend,
                    settings.paydayShiftWeekend,
                    onCheckedChange = { v -> onUpdate { it.copy(paydayShiftWeekend = v) } },
                )
            }
        }

        item { SettingsSection("表示") }
        item {
            SettingsItem("テーマ", Icons.Outlined.DarkMode, settings.theme.label) { dialog = SettingsDialog.THEME }
        }
    }

    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = "テーマ",
            options = ThemeMode.entries,
            selected = settings.theme,
            label = { it.label },
            onDismiss = { dialog = null },
            onSelect = { mode -> onUpdate { it.copy(theme = mode) }; dialog = null },
        )
        SettingsDialog.PERIOD_START -> ChoiceDialog(
            title = "月の開始日",
            options = (1..Period.MAX_START_DAY).toList(),
            selected = settings.periodStartDay,
            label = ::periodStartLabel,
            onDismiss = { dialog = null },
            onSelect = { day -> onUpdate { it.copy(periodStartDay = day) }; dialog = null },
        )
        SettingsDialog.PAYMENT -> ChoiceDialog(
            title = "既定の支払い方法",
            options = PaymentMethod.entries,
            selected = settings.defaultPayment,
            label = { it.label },
            onDismiss = { dialog = null },
            onSelect = { p -> onUpdate { it.copy(defaultPayment = p) }; dialog = null },
        )
        SettingsDialog.PAYDAY -> ChoiceDialog(
            title = "給料日",
            options = (0..31).toList(),
            selected = settings.payday,
            label = ::paydayLabel,
            onDismiss = { dialog = null },
            onSelect = { d -> onUpdate { it.copy(payday = d) }; dialog = null },
        )
        null -> Unit
    }
}

/** 「毎月1日（カレンダーどおり）」「毎月25日〜翌月24日」 */
internal fun periodStartLabel(day: Int): String =
    if (day == 1) "毎月1日（カレンダーどおり）" else "毎月${day}日〜翌月${day - 1}日"

internal fun paydayLabel(day: Int): String = when (day) {
    0 -> "設定しない"
    31 -> "毎月末"
    else -> "毎月${day}日"
}
