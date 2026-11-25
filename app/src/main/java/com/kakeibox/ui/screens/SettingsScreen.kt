package com.kakeibox.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.kakeibox.security.AppLock
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
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
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kakeibox.notify.Notifications
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
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Notifications.canPost(context)) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        pending?.invoke()
        pending = null
    }
    SettingsContent(
        settings, contentPadding, navigation,
        onUpdate = vm::updateSettings,
        notificationsAllowed = allowed,
        dataActions = rememberDataActions(),
        onAppLockChange = { on ->
            val activity = context as? FragmentActivity
            when {
                activity == null -> Unit
                on && !AppLock.canAuthenticate(context) ->
                    Toast.makeText(context, "端末に画面ロック（PIN・パターン・指紋など）を設定してください", Toast.LENGTH_LONG).show()
                else -> AppLock.authenticate(
                    activity,
                    if (on) "アプリロックをオンにする" else "アプリロックをオフにする",
                    onSuccess = {
                        AppLock.markUnlocked()
                        vm.updateSettings { it.copy(appLock = on) }
                    },
                )
            }
        },
        withNotificationPermission = { action ->
            if (Notifications.canPost(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                action()
            } else {
                pending = action
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}

private enum class SettingsDialog { THEME, PERIOD_START, PAYMENT, PAYDAY, REMINDER_TIME }

@Composable
internal fun SettingsContent(
    settings: AppSettings,
    contentPadding: PaddingValues,
    navigation: SettingsNavigation,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    notificationsAllowed: Boolean = true,
    /** 通知の許可を確認してから action を実行する */
    withNotificationPermission: (action: () -> Unit) -> Unit = { it() },
    dataActions: DataActions = DataActions(),
    onAppLockChange: (Boolean) -> Unit = {},
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

        item { SettingsSection("通知") }
        if (!notificationsAllowed && (settings.reminderEnabled || settings.budgetAlertEnabled)) {
            item {
                Text(
                    "通知が許可されていません。端末の設定アプリから Kakeibox の通知を許可してください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        item {
            SettingsSwitch(
                "入力リマインダー",
                Icons.Outlined.NotificationsActive,
                settings.reminderEnabled,
                summary = "毎日 ${minutesLabel(settings.reminderMinutes)} に通知（その日の記録がないときだけ）",
                onCheckedChange = { on ->
                    if (on) withNotificationPermission { onUpdate { it.copy(reminderEnabled = true) } }
                    else onUpdate { it.copy(reminderEnabled = false) }
                },
            )
        }
        if (settings.reminderEnabled) {
            item {
                SettingsItem("通知の時刻", Icons.Outlined.Schedule, minutesLabel(settings.reminderMinutes)) {
                    dialog = SettingsDialog.REMINDER_TIME
                }
            }
        }
        item {
            SettingsSwitch(
                "使いすぎ通知",
                Icons.Outlined.NotificationImportant,
                settings.budgetAlertEnabled,
                summary = "1日・週・月の予算の80%と100%を超えたときに通知",
                onCheckedChange = { on ->
                    if (on) withNotificationPermission { onUpdate { it.copy(budgetAlertEnabled = true) } }
                    else onUpdate { it.copy(budgetAlertEnabled = false) }
                },
            )
        }

        item { SettingsSection("データ") }
        item { SettingsItem("CSVに書き出す", Icons.Outlined.FileDownload, "すべての明細（Excelで開けます）", onClick = dataActions.exportCsv) }
        item { SettingsItem("CSVから取り込む", Icons.Outlined.FileUpload, "日付,種別,カテゴリ,金額,支払い方法,メモ の形式", onClick = dataActions.importCsv) }
        item { SettingsItem("バックアップを保存", Icons.Outlined.Backup, "明細・テンプレート・定期の収支・予算・設定をファイルに保存", onClick = dataActions.backup) }
        item { SettingsItem("バックアップから復元", Icons.Outlined.Restore, "機種変更のときなどに。現在のデータは置き換わります", onClick = dataActions.restore) }

        item { SettingsSection("セキュリティ") }
        item {
            SettingsSwitch(
                "アプリロック",
                Icons.Outlined.Lock,
                settings.appLock,
                summary = "起動時と、1分以上たってから戻ったときに指紋・顔認証または端末のPINで解除",
                onCheckedChange = onAppLockChange,
            )
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
        SettingsDialog.REMINDER_TIME -> TimeDialog(
            initialMinutes = settings.reminderMinutes,
            onDismiss = { dialog = null },
            onConfirm = { m -> onUpdate { it.copy(reminderMinutes = m) }; dialog = null },
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

internal fun minutesLabel(m: Int): String = "%d:%02d".format(m / 60, m % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通知の時刻") },
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
