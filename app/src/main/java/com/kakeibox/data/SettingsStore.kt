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
)

/** 設定の保存（SharedPreferences）と購読 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load(prefs))
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** 今日を含む期間 */
    fun currentPeriod(today: LocalDate = LocalDate.now()): Period = Period.of(today, _settings.value.periodStartDay)

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
            .apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_PERIOD_START = "period_start_day"

        fun load(p: SharedPreferences) = AppSettings(
            theme = p.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            periodStartDay = p.getInt(KEY_PERIOD_START, 1).coerceIn(1, Period.MAX_START_DAY),
        )
    }
}
