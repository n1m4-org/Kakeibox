package com.kakeibox.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("端末の設定に合わせる"),
    LIGHT("ライト"),
    DARK("ダーク"),
}

/** アプリの設定 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

/** 設定の保存（SharedPreferences）と購読 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load(prefs))
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        save(next)
        _settings.value = next
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.theme.name)
            .apply()
    }

    private companion object {
        const val KEY_THEME = "theme"

        fun load(p: SharedPreferences) = AppSettings(
            theme = p.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }
}
