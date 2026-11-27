package com.kakeibox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kakeibox.data.Categories
import com.kakeibox.data.CategoryConfig

/** カテゴリの設定を購読する（追加・並べ替え・非表示がすぐ画面に反映される） */
@Composable
fun categoryConfig(): CategoryConfig {
    val config by Categories.config.collectAsState()
    return config
}
