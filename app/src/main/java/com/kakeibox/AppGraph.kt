package com.kakeibox

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kakeibox.data.AppDatabase
import com.kakeibox.data.BudgetStore
import com.kakeibox.data.CategoryStore
import com.kakeibox.data.RecurringDao
import com.kakeibox.data.SettingsStore
import com.kakeibox.notify.BudgetAlerts
import com.kakeibox.data.TemplateDao
import com.kakeibox.data.TransactionRepository
import com.kakeibox.widget.BreakdownWidget
import com.kakeibox.widget.DailyBudgetWidget
import com.kakeibox.widget.KakeiboxWidget

/** アプリ全体で共有する依存関係の組み立て */
object AppGraph {
    @Volatile private var repo: TransactionRepository? = null
    @Volatile private var budget: BudgetStore? = null
    @Volatile private var settings: SettingsStore? = null
    @Volatile private var apiKeys: com.kakeibox.receipt.ApiKeyStore? = null

    /** レシート読み取り（AI）の API キー */
    fun apiKeyStore(context: Context): com.kakeibox.receipt.ApiKeyStore =
        apiKeys ?: synchronized(this) {
            apiKeys ?: com.kakeibox.receipt.ApiKeyStore(context.applicationContext).also { apiKeys = it }
        }

    fun repository(context: Context): TransactionRepository =
        repo ?: synchronized(this) {
            repo ?: run {
                val app = context.applicationContext
                TransactionRepository(
                    dao = AppDatabase.get(app).transactionDao(),
                    onChanged = {
                        refreshWidgets(app)
                        BudgetAlerts.check(app)
                    },
                )
            }.also { repo = it }
        }

    fun templateDao(context: Context): TemplateDao = AppDatabase.get(context.applicationContext).templateDao()

    fun recurringDao(context: Context): RecurringDao = AppDatabase.get(context.applicationContext).recurringDao()

    @Volatile private var categories: CategoryStore? = null

    /** カテゴリの設定（予算などより先に読み込む） */
    fun categoryStore(context: Context): CategoryStore =
        categories ?: synchronized(this) {
            categories ?: CategoryStore(context.applicationContext).also { categories = it }
        }

    fun budgetStore(context: Context): BudgetStore =
        budget ?: synchronized(this) {
            categoryStore(context)
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
        BreakdownWidget().updateAll(app)
    }
}
