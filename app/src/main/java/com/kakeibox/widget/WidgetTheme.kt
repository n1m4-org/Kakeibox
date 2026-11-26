package com.kakeibox.widget

import androidx.glance.material3.ColorProviders
import com.kakeibox.ui.theme.DarkColors
import com.kakeibox.ui.theme.LightColors

/** ウィジェットもアプリと同じパレットの配色にする（端末のライト／ダークに追従） */
val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)
