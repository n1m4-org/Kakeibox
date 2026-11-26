package com.kakeibox.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.withContext
import kotlin.math.max

/** 背景画像の取り込み（縮小・向きの補正をしてアプリ内に保存） */
object BackgroundImage {
    private const val MAX_SIDE = 1600

    /** @return 保存できたら true */
    suspend fun import(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        importFrom({ context.contentResolver.openInputStream(uri) }, backgroundFile(context))
    }

    /**
     * 画像を読み込み、向きを補正して長辺 [MAX_SIDE] 以下に縮小し、JPEG で [out] に保存する。
     * @param open 画像のストリームを開く（大きさ・本体・向きの読み込みで3回呼ぶ）
     */
    fun importFrom(open: () -> InputStream?, out: File): Boolean {
        // 大きさだけ読む（inJustDecodeBounds の decodeStream は常に null を返すので戻り値は見ない）
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = open() ?: return false
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val oriented = rotate(decoded, orientation(open))
        val scale = MAX_SIDE.toFloat() / max(oriented.width, oriented.height)
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(oriented, (oriented.width * scale).toInt(), (oriented.height * scale).toInt(), true)
        } else oriented
        out.outputStream().use { sized.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return true
    }

    fun delete(context: Context) {
        backgroundFile(context).delete()
    }

    /** 写真の向き（EXIF） */
    private fun orientation(open: () -> InputStream?): Int = runCatching {
        open()?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun rotate(bmp: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }
}
