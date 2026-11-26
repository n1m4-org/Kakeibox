package com.kakeibox.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** 次の 0:00 までのミリ秒（0:00 ちょうどなら翌日の 0:00 まで） */
fun millisUntilNextMidnight(now: LocalDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()

/**
 * 「今日」の日付。0:00 を過ぎると新しい日付を流す。
 * 画面やウィジェットは LocalDate.now() を直接読まず、これを購読して日替わりに追従する。
 */
object Today {
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    /** 今の日付を読み直す（日付が変わっていれば購読側に流れる） */
    fun refresh(now: LocalDate = LocalDate.now()) {
        _date.value = now
    }

    /**
     * 0:00 ごとの更新を始める。端末の日付・時刻・タイムゾーンの変更も受け取る。
     * スリープ中は待ち時間が延びることがあるため、画面の再表示時にも [refresh] を呼ぶ。
     */
    fun start(context: Context, scope: CoroutineScope, onDateChanged: () -> Unit = {}) {
        val app = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val before = _date.value
                refresh()
                if (_date.value != before) onDateChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            while (true) {
                // 0:00 ちょうどの誤差を避けるため少し遅らせる
                delay(millisUntilNextMidnight(LocalDateTime.now()) + 1_000)
                val before = _date.value
                refresh()
                if (_date.value != before) onDateChanged()
            }
        }
    }
}
