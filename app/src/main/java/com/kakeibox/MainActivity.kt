package com.kakeibox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.screens.EditScreen
import com.kakeibox.ui.screens.HomeScreen
import com.kakeibox.ui.screens.ReportScreen
import com.kakeibox.ui.theme.KakeiboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KakeiboxTheme { KakeiboxApp() } }
    }
}

private object Routes {
    const val HOME = "home"
    const val REPORT = "report"
    const val EDIT = "edit?id={id}"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@Composable
fun KakeiboxApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()

    NavHost(nav, startDestination = "tabs") {
        composable("tabs") {
            val inner = rememberNavController()
            val innerEntry by inner.currentBackStackEntryAsState()
            val tab = innerEntry?.destination?.route ?: Routes.HOME
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        listOf(
                            Triple(Routes.HOME, "明細", Icons.AutoMirrored.Filled.ReceiptLong),
                            Triple(Routes.REPORT, "レポート", Icons.Default.PieChart),
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
                        FloatingActionButton(onClick = { nav.navigate(Routes.edit()) }) {
                            Icon(Icons.Default.Add, "記録を追加")
                        }
                    }
                },
            ) { padding ->
                NavHost(inner, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen(vm, padding, onEdit = { nav.navigate(Routes.edit(it)) }) }
                    composable(Routes.REPORT) { ReportScreen(vm, padding) }
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
