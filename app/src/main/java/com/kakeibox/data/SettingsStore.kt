package com.kakeibox.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class ThemeMode(val label: String) {
    SYSTEM("端末の設定に合わせる"),
    LIGHT("ライト"),
    DARK("ダーク"),
}

/** アプリの設定 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** 月の開始日（締め日の翌日）。1ならカレンダーどおり */
    val periodStartDay: Int = 1,
    /** クイック入力などで使う既定の支払い方法 */
    val defaultPayment: PaymentMethod = PaymentMethod.CASH,
    /** 給料日（0=表示しない、31=月末） */
    val payday: Int = 0,
    /** 給料日が土日なら直前の金曜にする */
    val paydayShiftWeekend: Boolean = true,
    /** 入力リマインダー */
    val reminderEnabled: Boolean = false,
    /** リマインダーの時刻（0:00からの分） */
    val reminderMinutes: Int = 21 * 60,
    /** 使いすぎ通知 */
    val budgetAlertEnabled: Boolean = false,
    /** アプリロック */
    val appLock: Boolean = false,
)

/** 設定の保存（SharedPreferences）と購読 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load(prefs))
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** 今日を含む期間 */
    fun currentPeriod(today: LocalDate = LocalDate.now()): Period = Period.of(today, _settings.value.periodStartDay)

    /** 保存内容を読み直す（バックアップの復元後） */
    fun reload() { _settings.value = load(prefs) }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        save(next)
        _settings.value = next
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.theme.name)
            .putInt(KEY_PERIOD_START, s.periodStartDay)
            .putString(KEY_DEFAULT_PAYMENT, s.defaultPayment.name)
            .putInt(KEY_PAYDAY, s.payday)
            .putBoolean(KEY_PAYDAY_SHIFT, s.paydayShiftWeekend)
            .putBoolean(KEY_REMINDER, s.reminderEnabled)
            .putInt(KEY_REMINDER_MINUTES, s.reminderMinutes)
            .putBoolean(KEY_BUDGET_ALERT, s.budgetAlertEnabled)
            .putBoolean(KEY_APP_LOCK, s.appLock)
            .apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_PERIOD_START = "period_start_day"
        const val KEY_DEFAULT_PAYMENT = "default_payment"
        const val KEY_PAYDAY = "payday"
        const val KEY_PAYDAY_SHIFT = "payday_shift_weekend"
        const val KEY_REMINDER = "reminder"
        const val KEY_REMINDER_MINUTES = "reminder_minutes"
        const val KEY_BUDGET_ALERT = "budget_alert"
        const val KEY_APP_LOCK = "app_lock"

        fun load(p: SharedPreferences) = AppSettings(
            theme = p.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            periodStartDay = p.getInt(KEY_PERIOD_START, 1).coerceIn(1, Period.MAX_START_DAY),
            defaultPayment = PaymentMethod.fromName(p.getString(KEY_DEFAULT_PAYMENT, null)),
            payday = p.getInt(KEY_PAYDAY, 0).coerceIn(0, 31),
            paydayShiftWeekend = p.getBoolean(KEY_PAYDAY_SHIFT, true),
            reminderEnabled = p.getBoolean(KEY_REMINDER, false),
            reminderMinutes = p.getInt(KEY_REMINDER_MINUTES, 21 * 60).coerceIn(0, 24 * 60 - 1),
            budgetAlertEnabled = p.getBoolean(KEY_BUDGET_ALERT, false),
            appLock = p.getBoolean(KEY_APP_LOCK, false),
        )
    }
}
