package com.kakeibox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.quickadd.QuickAddContent
import com.kakeibox.ui.screens.EditScreen
import com.kakeibox.ui.screens.HomeScreen
import com.kakeibox.ui.screens.ReportScreen
import com.kakeibox.ui.theme.KakeiboxTheme
import com.kakeibox.ui.yen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ウィジェットのクイック入力から「詳細入力」を選んだ場合は入力画面から始める
        val openEdit = savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_EDIT, false)
        setContent { KakeiboxTheme { KakeiboxApp(openEditOnStart = openEdit) } }
    }

    companion object {
        private const val EXTRA_OPEN_EDIT = "open_edit"

        fun openEditIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_EDIT, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

private object Routes {
    const val HOME = "home"
    const val REPORT = "report"
    const val EDIT = "edit?id={id}"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakeiboxApp(openEditOnStart: Boolean = false, vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()

    LaunchedEffect(Unit) {
        if (openEditOnStart) nav.navigate(Routes.edit())
    }

    NavHost(nav, startDestination = "tabs") {
        composable("tabs") {
            val inner = rememberNavController()
            val innerEntry by inner.currentBackStackEntryAsState()
            val tab = innerEntry?.destination?.route ?: Routes.HOME
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            var showQuickAdd by rememberSaveable { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            // クイック入力の保存結果をSnackbarで通知（取り消し可）
            LaunchedEffect(Unit) {
                vm.quickAdded.collect { tx ->
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = "${tx.categoryEnum.label} ${yen(tx.amount)} を記録しました",
                        actionLabel = "取り消す",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.delete(tx)
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar {
                        listOf(
                            Triple(Routes.HOME, "明細", Icons.AutoMirrored.Outlined.ReceiptLong),
                            Triple(Routes.REPORT, "レポート", Icons.Outlined.PieChart),
                        ).forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = tab == r,
                                onClick = {
                                    inner.navigate(r) {
                                        popUpTo(inner.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(icon, null) },
                                label = { Text(label) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tab == Routes.HOME) {
                        FloatingActionButton(onClick = { showQuickAdd = true }) {
                            Icon(Icons.Outlined.Add, "支出を追加")
                        }
                    }
                },
            ) { padding ->
                NavHost(inner, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen(vm, padding, onEdit = { nav.navigate(Routes.edit(it)) }) }
                    composable(Routes.REPORT) { ReportScreen(vm, padding) }
                }
            }

            if (showQuickAdd) {
                fun close(then: () -> Unit = {}) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showQuickAdd = false
                        then()
                    }
                }
                ModalBottomSheet(onDismissRequest = { showQuickAdd = false }, sheetState = sheetState) {
                    QuickAddContent(
                        onSave = { amount, category ->
                            vm.quickAdd(amount, category)
                            close()
                        },
                        onDetail = { close { nav.navigate(Routes.edit()) } },
                    )
                }
            }
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it >= 0 }
            EditScreen(vm, id, onDone = { nav.popBackStack() })
        }
    }
}
