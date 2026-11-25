package com.kakeibox

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kakeibox.data.AppDatabase
import com.kakeibox.data.TransactionRepository
import com.kakeibox.widget.KakeiboxWidget

/** アプリ全体で共有する依存関係の組み立て */
object AppGraph {
    @Volatile private var repo: TransactionRepository? = null

    fun repository(context: Context): TransactionRepository =
        repo ?: synchronized(this) {
            repo ?: run {
                val app = context.applicationContext
                TransactionRepository(
                    dao = AppDatabase.get(app).transactionDao(),
                    onChanged = { KakeiboxWidget().updateAll(app) },
                )
            }.also { repo = it }
        }
}
