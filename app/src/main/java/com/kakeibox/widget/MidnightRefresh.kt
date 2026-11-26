package com.kakeibox.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kakeibox.AppGraph
import com.kakeibox.data.Today
import com.kakeibox.data.millisUntilNextMidnight
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 0:00（の1分後）にウィジェットを描き直し、日替わり（今日の予算・繰り越し・週・月）を反映する。
 * アプリが起動していなくても動くよう WorkManager で予約し、実行のたびに翌日分を予約し直す。
 * 端末の省電力機能により数分〜十数分遅れることがある。
 */
object MidnightRefresh {
    private const val WORK = "midnight_refresh"
    private const val MARGIN_MS = 60_000L

    fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
            .setInitialDelay(millisUntilNextMidnight(now) + MARGIN_MS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }
}

class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Today.refresh()
        AppGraph.refreshWidgets(applicationContext)
        MidnightRefresh.schedule(applicationContext)
        return Result.success()
    }
}
