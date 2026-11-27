package com.kakeibox.data

import android.content.Context

/** カテゴリの設定を SharedPreferences に保存し、[Categories] に反映する */
class CategoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        reload()
    }

    /** 保存内容を読み直す（起動時・バックアップの復元後） */
    fun reload() = Categories.set(CategoryConfig.fromJson(prefs.getString(KEY, null)))

    fun update(transform: (CategoryConfig) -> CategoryConfig) {
        val next = transform(Categories.config.value)
        prefs.edit().putString(KEY, next.toJson()).apply()
        Categories.set(next)
    }

    companion object {
        const val PREFS = "kakeibox_categories"
        private const val KEY = "config"
    }
}
