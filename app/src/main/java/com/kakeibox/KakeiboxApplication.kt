package com.kakeibox

import android.app.Application
import com.kakeibox.data.RecurringRunner
import com.kakeibox.data.Today
import com.kakeibox.widget.MidnightRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.kakeibox.notify.Notifications
import com.kakeibox.notify.Reminder

class KakeiboxApplication : Application() {
    /** アプリのプロセスが続く間のコルーチン */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 0:00 や端末の日付変更で「今日」を切り替え、ウィジェットも描き直す
        Today.start(this, appScope, onDateChanged = { appScope.launch { AppGraph.refreshWidgets(this@KakeiboxApplication) } })
        MidnightRefresh.schedule(this)
        Notifications.createChannels(this)
        RecurringRunner.schedule(this)
        Reminder.apply(this)
    }
}
