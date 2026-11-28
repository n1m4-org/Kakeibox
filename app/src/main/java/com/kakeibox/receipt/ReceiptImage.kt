package com.kakeibox.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** レシート画像の読み込みと、AI に送る大きさへの縮小 */
object ReceiptImage {
    /** Claude が扱いやすい長辺（これより大きい画像は縮小される） */
    private const val MAX_EDGE = 1568

    /** Uri の画像を、向きを補正して長辺 [MAX_EDGE] 以下で読み込む */
    fun load(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw ReceiptException("画像を開けませんでした")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ReceiptException("画像を読み込めませんでした")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw ReceiptException("画像を読み込めませんでした")

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val scale = minOf(1f, MAX_EDGE.toFloat() / max(decoded.width, decoded.height))
        if (rotation == 0 && scale >= 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (out !== decoded) decoded.recycle()
        return out
    }

    /** JPEG（品質85）の base64（改行なし） */
    fun toJpegBase64(bitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** 確認画面に出す小さな画像 */
    fun thumbnail(bitmap: Bitmap, edge: Int = 480): Bitmap {
        val scale = minOf(1f, edge.toFloat() / max(bitmap.width, bitmap.height))
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
    }
}
