package com.kakeibox.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** ドーナツの色。Bitmap は昼夜の切り替えに追従しないので、どちらの背景でも見える中間色にする */
private const val DONUT_OK = 0xFF26A69A.toInt()
private const val DONUT_WARN = 0xFFFFB300.toInt()
private const val DONUT_OVER = 0xFFE53935.toInt()
private const val DONUT_TRACK = 0x66808080

/** 使用率に応じた弧の色（バーと同じ区切り：80%超で黄、超過で赤） */
internal fun donutColor(ratio: Float): Int = when {
    ratio >= 1f -> DONUT_OVER
    ratio >= 0.8f -> DONUT_WARN
    else -> DONUT_OK
}

/**
 * 使った割合を示すドーナツ（輪だけ）を描く。12時から時計回り、超過なら全周。
 * Glance には Canvas がないため、Bitmap にして Image で表示する。
 *
 * @param sizePx 一辺のピクセル数
 * @param ratio 使用率（0〜。1以上は全周）
 */
fun donutBitmap(sizePx: Int, ratio: Float): Bitmap {
    val size = sizePx.coerceAtLeast(8)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = size * 0.11f
    val inset = stroke / 2f
    val oval = RectF(inset, inset, size - inset, size - inset)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    paint.color = DONUT_TRACK
    canvas.drawArc(oval, 0f, 360f, false, paint)

    val sweep = 360f * ratio.coerceIn(0f, 1f)
    if (sweep > 0f) {
        paint.color = donutColor(ratio)
        // 全周のときは端の丸が重ならないよう、Cap を BUTT にする
        if (sweep >= 360f) paint.strokeCap = Paint.Cap.BUTT
        canvas.drawArc(oval, -90f, sweep, false, paint)
    }
    return bitmap
}
