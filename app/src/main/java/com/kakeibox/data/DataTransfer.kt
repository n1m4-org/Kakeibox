package com.kakeibox.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.kakeibox.AppGraph
import com.kakeibox.notify.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

/** CSVの書き出し・取り込みと、バックアップの保存・復元（ファイルはSAFで選ぶ） */
object DataTransfer {
    /** バックアップに含める SharedPreferences（予算・設定） */
    private val PREF_FILES = listOf("kakeibox", "kakeibox_settings", CategoryStore.PREFS)

    suspend fun exportCsv(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val txs = AppGraph.repository(context).all()
        write(context, uri, Csv.export(txs))
        txs.size
    }

    suspend fun readCsv(context: Context, uri: Uri): Csv.ImportResult = withContext(Dispatchers.IO) {
        Csv.import(readText(context, uri))
    }

    suspend fun importTransactions(context: Context, txs: List<Transaction>) =
        AppGraph.repository(context).insertAll(txs.map { it.copy(id = 0) })

    suspend fun writeBackup(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val data = BackupData(
            transactions = db.transactionDao().all(),
            templates = db.templateDao().all(),
            recurring = db.recurringDao().all(),
            prefs = PREF_FILES.associateWith { context.getSharedPreferences(it, Context.MODE_PRIVATE).all },
        )
        write(context, uri, data.toJson())
        data
    }

    /** @throws IllegalArgumentException バックアップとして読めない場合 */
    suspend fun readBackup(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        BackupData.fromJson(readText(context, uri))
    }

    /** 現在のデータと設定をすべて置き換える */
    suspend fun restore(context: Context, data: BackupData) {
        val app = context.applicationContext
        withContext(Dispatchers.IO) {
            val db = AppDatabase.get(app)
            db.withTransaction {
                db.transactionDao().deleteAll()
                db.templateDao().deleteAll()
                db.recurringDao().deleteAll()
                db.transactionDao().insertAll(data.transactions)
                db.templateDao().insertAll(data.templates)
                db.recurringDao().insertAll(data.recurring)
            }
            data.prefs.filterKeys { it in PREF_FILES }.forEach { (file, values) ->
                val editor = app.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear()
                values.forEach { (k, v) ->
                    when (v) {
                        is Boolean -> editor.putBoolean(k, v)
                        is Int -> editor.putInt(k, v)
                        is Long -> editor.putLong(k, v)
                        is Float -> editor.putFloat(k, v)
                        is String -> editor.putString(k, v)
                        is Set<*> -> editor.putStringSet(k, v.map { it.toString() }.toSet())
                    }
                }
                editor.commit()
            }
        }
        AppGraph.categoryStore(app).reload()
        AppGraph.budgetStore(app).reload()
        AppGraph.settingsStore(app).reload()
        Reminder.apply(app)
        AppGraph.refreshWidgets(app)
    }

    private fun write(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** UTF-8 で読めなければ Shift_JIS（日本語版Excelで保存したCSV）として読む */
    private fun readText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val utf8 = String(bytes, Charsets.UTF_8)
        if (!utf8.contains('�')) return utf8
        return runCatching { String(bytes, Charset.forName("Shift_JIS")) }.getOrDefault(utf8)
    }
}
