package com.kakeibox

import android.app.Application
import com.kakeibox.data.RecurringRunner
import com.kakeibox.notify.Notifications
import com.kakeibox.notify.Reminder

class KakeiboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        RecurringRunner.schedule(this)
        Reminder.apply(this)
    }
}
