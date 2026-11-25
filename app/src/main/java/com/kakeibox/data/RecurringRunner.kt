package com.kakeibox.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kakeibox.AppGraph
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** 定期の収支を、記録日が来た分だけ明細に登録する */
object RecurringRunner {
    private val mutex = Mutex()

    /** @return 登録した件数 */
    suspend fun run(context: Context, today: LocalDate = LocalDate.now()): Int = mutex.withLock {
        val dao = AppGraph.recurringDao(context)
        val repo = AppGraph.repository(context)
        var count = 0
        dao.all().forEach { r ->
            val dates = r.dueDates(today)
            dates.forEach { repo.save(r.toTransaction(it)); count++ }
            if (r.enabled) dao.upsert(r.copy(processedUntil = today.toEpochDay()))
        }
        count
    }

    /** 1日1回の自動実行を登録する（アプリ起動時に呼ぶ） */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("recurring", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class RecurringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RecurringRunner.run(applicationContext)
        return Result.success()
    }
}
