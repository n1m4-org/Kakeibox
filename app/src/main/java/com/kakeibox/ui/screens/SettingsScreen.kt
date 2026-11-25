package com.kakeibox.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.AppSettings
import com.kakeibox.data.ThemeMode
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.ChoiceDialog
import com.kakeibox.ui.components.SettingsItem
import com.kakeibox.ui.components.SettingsSection

/** 設定画面から開く画面 */
class SettingsNavigation(
    val openBudgets: () -> Unit,
)

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues, navigation: SettingsNavigation) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    SettingsContent(settings, contentPadding, navigation, onUpdate = vm::updateSettings)
}

private enum class SettingsDialog { THEME }

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
            SettingsItem("予算の設定", Icons.Outlined.Savings, "全体・カテゴリごとの1日の予算", onClick = navigation.openBudgets)
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
        null -> Unit
    }
}
