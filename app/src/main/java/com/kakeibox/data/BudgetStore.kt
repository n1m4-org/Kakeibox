package com.kakeibox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 月の予算(円)。0は未設定。 */
class BudgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("kakeibox", Context.MODE_PRIVATE)
    private val _budget = MutableStateFlow(prefs.getLong(KEY, 0L))
    val budget: StateFlow<Long> = _budget.asStateFlow()

    fun set(value: Long) {
        prefs.edit().putLong(KEY, value).apply()
        _budget.value = value
    }

    private companion object { const val KEY = "monthly_budget" }
}
