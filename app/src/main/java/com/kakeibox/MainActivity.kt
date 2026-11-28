package com.kakeibox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.kakeibox.security.AppLock
import com.kakeibox.security.LockScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.lifecycleScope
import com.kakeibox.data.RecurringRunner
import com.kakeibox.data.Today
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.UpdateDialog
import com.kakeibox.update.ReleaseInfo
import com.kakeibox.ui.quickadd.QuickAddContent
import com.kakeibox.ui.screens.AppearanceScreen
import com.kakeibox.ui.screens.BudgetScreen
import com.kakeibox.ui.screens.CategoriesScreen
import com.kakeibox.ui.screens.CalendarScreen
import com.kakeibox.data.BudgetKind
import com.kakeibox.ui.screens.EditScreen
import com.kakeibox.ui.screens.HomeScreen
import com.kakeibox.ui.screens.ReceiptScreen
import com.kakeibox.ui.screens.ReportScreen
import com.kakeibox.ui.screens.SettingsNavigation
import com.kakeibox.ui.screens.RecurringEditScreen
import com.kakeibox.ui.screens.RecurringScreen
import com.kakeibox.ui.screens.SearchScreen
import com.kakeibox.ui.screens.SettingsScreen
import com.kakeibox.ui.screens.TemplatesScreen
import com.kakeibox.ui.theme.KakeiboxTheme
import com.kakeibox.ui.theme.KakeiboxBackground
import com.kakeibox.ui.theme.TransparentSurfaces
import com.kakeibox.ui.theme.acrylic
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kakeibox.ui.yen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var lockMessage by mutableStateOf<String?>(null)
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ウィジェットから特定の画面（入力・予算設定）を指定して起動された場合はその画面から始める
        val startRoute = if (savedInstanceState == null) intent.getStringExtra(EXTRA_START_ROUTE) else null
        setContent {
            KakeiboxTheme {
                Box {
                    KakeiboxApp(startRoute = startRoute)
                    // ロック中は上に重ねて隠す（画面の状態は保ったまま）
                    if (locked) LockScreen(lockMessage, onUnlock = ::unlock, onBack = { moveTaskToBack(true) })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val enabled = AppGraph.settingsStore(this).settings.value.appLock
        if (AppLock.shouldLock(enabled)) locked = true else if (!enabled) locked = false
    }

    override fun onResume() {
        super.onResume()
        // スリープ中に日付が変わっていた場合に備えて「今日」を読み直す
        Today.refresh()
        // 記録日が来た定期の収支を登録（WorkManagerの実行待ちを補う）
        lifecycleScope.launch { RecurringRunner.run(applicationContext) }
        if (locked) unlock()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) AppLock.onBackground()
    }

    private fun unlock() {
        if (prompting) return
        prompting = true
        AppLock.authenticate(
            this, "Kakeibox のロックを解除",
            onSuccess = { prompting = false; AppLock.markUnlocked(); locked = false; lockMessage = null },
            onFailure = { prompting = false; lockMessage = it },
        )
    }


    companion object {
        const val EXTRA_START_ROUTE = "start_route"

        private fun startIntent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_START_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fun openEditIntent(context: Context): Intent = startIntent(context, Routes.edit())
        fun openBudgetsIntent(context: Context): Intent = startIntent(context, Routes.budgets(BudgetKind.DAILY))
        fun openReceiptIntent(context: Context): Intent = startIntent(context, Routes.RECEIPT)
    }
}

internal object Routes {
    const val HOME = "home"
    const val REPORT = "report"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val BUDGETS = "budgets?kind={kind}"
    fun budgets(kind: BudgetKind) = "budgets?kind=${kind.name}"
    const val TEMPLATES = "templates"
    const val SEARCH = "search"
    const val APPEARANCE = "appearance"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"
    const val RECURRING_EDIT = "recurring/edit?id={id}"
    fun recurringEdit(id: Long?) = if (id == null) "recurring/edit" else "recurring/edit?id=$id"
    const val RECEIPT = "receipt"
    const val EDIT = "edit?id={id}"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakeiboxApp(startRoute: String? = null, vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()

    LaunchedEffect(Unit) {
        if (startRoute != null) nav.navigate(startRoute)
    }

    // アプリ全体の背景（画像またはパレットのグラデーション）。各画面はこの上にアクリルで重なる
    KakeiboxBackground {
    NavHost(nav, startDestination = "tabs") {
        composable("tabs") {
            val inner = rememberNavController()
            val innerEntry by inner.currentBackStackEntryAsState()
            val tab = innerEntry?.destination?.route ?: Routes.HOME
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            var showQuickAdd by rememberSaveable { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            // 起動時のアップデート確認（1日1回）。新しい版があればスナックバーで知らせる
            var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
            var showUpdate by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                vm.autoCheckUpdate()?.let { info ->
                    updateInfo = info
                    val result = snackbar.showSnackbar(
                        message = "新しいバージョン ${info.versionName} があります",
                        actionLabel = "更新",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) showUpdate = true
                }
            }
            if (showUpdate) updateInfo?.let { UpdateDialog(it) { showUpdate = false } }

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
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.acrylic(RectangleShape),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                    ) {
                        listOf(
                            Triple(Routes.HOME, "明細", Icons.AutoMirrored.Outlined.ReceiptLong),
                            Triple(Routes.CALENDAR, "カレンダー", Icons.Outlined.CalendarMonth),
                            Triple(Routes.REPORT, "レポート", Icons.Outlined.PieChart),
                            Triple(Routes.SETTINGS, "設定", Icons.Outlined.Settings),
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallFloatingActionButton(onClick = { nav.navigate(Routes.RECEIPT) }) {
                                Icon(Icons.Outlined.DocumentScanner, "レシートを読み取る")
                            }
                            Spacer(Modifier.height(12.dp))
                            FloatingActionButton(onClick = { showQuickAdd = true }) {
                                Icon(Icons.Outlined.Add, "支出を追加")
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(inner, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen(
                            vm, padding,
                            onEdit = { nav.navigate(Routes.edit(it)) },
                            onOpenBudgets = { nav.navigate(Routes.budgets(it)) },
                            onSearch = { nav.navigate(Routes.SEARCH) },
                        )
                    }
                    composable(Routes.CALENDAR) { CalendarScreen(vm, padding, onOpen = { nav.navigate(Routes.edit(it)) }) }
                    composable(Routes.REPORT) { ReportScreen(vm, padding) }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(vm, padding, SettingsNavigation(
                            openBudgets = { nav.navigate(Routes.budgets(BudgetKind.DAILY)) },
                            openTemplates = { nav.navigate(Routes.TEMPLATES) },
                            openRecurring = { nav.navigate(Routes.RECURRING) },
                            openAppearance = { nav.navigate(Routes.APPEARANCE) },
                            openCategories = { nav.navigate(Routes.CATEGORIES) },
                        ))
                    }
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
                    val appSettings by vm.settings.collectAsStateWithLifecycle()
                    val templates by vm.templates.collectAsStateWithLifecycle()
                    QuickAddContent(
                        defaultPayment = appSettings.defaultPayment,
                        onSave = { amount, category, payment ->
                            vm.quickAdd(amount, category, payment)
                            close()
                        },
                        templates = templates,
                        onTemplate = { t, entered ->
                            vm.addFromTemplate(t, entered)
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
            TransparentSurfaces { EditScreen(vm, id, onDone = { nav.popBackStack() }) }
        }
        composable(Routes.RECEIPT) {
            TransparentSurfaces { ReceiptScreen(onBack = { nav.popBackStack() }) }
        }
        composable(Routes.CATEGORIES) {
            TransparentSurfaces { CategoriesScreen(vm, onBack = { nav.popBackStack() }) }
        }
        composable(Routes.APPEARANCE) {
            TransparentSurfaces { AppearanceScreen(vm, onBack = { nav.popBackStack() }) }
        }
        composable(Routes.SEARCH) {
            TransparentSurfaces { SearchScreen(vm, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.edit(it)) }) }
        }
        composable(Routes.TEMPLATES) {
            TransparentSurfaces { TemplatesScreen(vm, onBack = { nav.popBackStack() }, onCreate = { nav.navigate(Routes.edit()) }) }
        }
        composable(Routes.RECURRING) {
            TransparentSurfaces { RecurringScreen(vm, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.recurringEdit(it)) }) }
        }
        composable(
            Routes.RECURRING_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it >= 0 }
            TransparentSurfaces { RecurringEditScreen(vm, id, onDone = { nav.popBackStack() }) }
        }
        composable(
            Routes.BUDGETS,
            arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = BudgetKind.DAILY.name }),
        ) { entry ->
            val kind = BudgetKind.valueOf(entry.arguments?.getString("kind") ?: BudgetKind.DAILY.name)
            TransparentSurfaces { BudgetScreen(vm, kind, onBack = { nav.popBackStack() }) }
        }
    }
    }
}
