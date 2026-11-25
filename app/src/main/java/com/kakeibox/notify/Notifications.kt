package com.kakeibox.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kakeibox.MainActivity
import com.kakeibox.QuickAddActivity
import com.kakeibox.R

object Notifications {
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_BUDGET = "budget_alert"
    private const val ID_REMINDER = 1
    private const val ID_BUDGET_BASE = 1000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "入力リマインダー", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "毎日決まった時刻に記録を促します" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "使いすぎ通知", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "予算の80%・100%を超えたときに通知します" }
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showReminder(context: Context) {
        if (!canPost(context)) return
        val quickAdd = PendingIntent.getActivity(
            context, 0, Intent(context, QuickAddActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日の支出を記録しましたか？")
            .setContentText("タップしてすぐに記録できます")
            .setContentIntent(quickAdd)
            .addAction(R.drawable.ic_add, "記録する", quickAdd)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(ID_REMINDER, n)
    }

    fun showBudgetAlert(context: Context, index: Int, title: String, body: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(ID_BUDGET_BASE + index, n)
    }
}
