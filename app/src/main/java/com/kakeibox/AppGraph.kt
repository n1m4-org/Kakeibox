package com.kakeibox

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kakeibox.data.AppDatabase
import com.kakeibox.data.BudgetStore
import com.kakeibox.data.SettingsStore
import com.kakeibox.data.TemplateDao
import com.kakeibox.data.TransactionRepository
import com.kakeibox.widget.DailyBudgetWidget
import com.kakeibox.widget.KakeiboxWidget

/** アプリ全体で共有する依存関係の組み立て */
object AppGraph {
    @Volatile private var repo: TransactionRepository? = null
    @Volatile private var budget: BudgetStore? = null
    @Volatile private var settings: SettingsStore? = null

    fun repository(context: Context): TransactionRepository =
        repo ?: synchronized(this) {
            repo ?: run {
                val app = context.applicationContext
                TransactionRepository(
                    dao = AppDatabase.get(app).transactionDao(),
                    onChanged = { refreshWidgets(app) },
                )
            }.also { repo = it }
        }

    fun templateDao(context: Context): TemplateDao = AppDatabase.get(context.applicationContext).templateDao()

    fun budgetStore(context: Context): BudgetStore =
        budget ?: synchronized(this) {
            budget ?: BudgetStore(context.applicationContext).also { budget = it }
        }

    fun settingsStore(context: Context): SettingsStore =
        settings ?: synchronized(this) {
            settings ?: SettingsStore(context.applicationContext).also { settings = it }
        }

    /** すべてのホーム画面ウィジェットを再描画する */
    suspend fun refreshWidgets(context: Context) {
        val app = context.applicationContext
        KakeiboxWidget().updateAll(app)
        DailyBudgetWidget().updateAll(app)
    }
}
