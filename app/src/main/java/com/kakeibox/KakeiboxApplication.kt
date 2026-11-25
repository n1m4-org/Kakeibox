package com.kakeibox

import android.app.Application
import com.kakeibox.data.RecurringRunner

class KakeiboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecurringRunner.schedule(this)
    }
}
