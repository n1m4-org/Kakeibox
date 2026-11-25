package com.kakeibox.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kakeibox.AppGraph
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** 毎日の入力リマインダー */
object Reminder {
    private const val WORK = "reminder"

    /** 次に通知する時刻までの待ち時間 */
    fun delayUntil(now: LocalDateTime, minutesOfDay: Int): Duration {
        val time = LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /** 設定に合わせて登録・解除する */
    fun apply(context: Context) {
        val s = AppGraph.settingsStore(context).settings.value
        val wm = WorkManager.getInstance(context)
        if (!s.reminderEnabled) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val delay = delayUntil(LocalDateTime.now(), s.reminderMinutes)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val s = AppGraph.settingsStore(applicationContext).settings.value
        if (!s.reminderEnabled) return Result.success()
        // 今日すでに記録があれば通知しない
        val today = LocalDate.now()
        val recorded = AppGraph.repository(applicationContext).countOn(today) > 0
        if (!recorded) Notifications.showReminder(applicationContext)
        return Result.success()
    }
}
