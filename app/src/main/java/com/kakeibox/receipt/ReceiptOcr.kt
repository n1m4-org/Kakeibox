package com.kakeibox.receipt

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** 端末での文字認識（ML Kit・日本語）。モデルは Google Play 開発者サービスが用意する */
object ReceiptOcr {
    /**
     * 画像の文字を行ごとに返す。
     * モデルの準備ができていないなど、使えないときは null（画像だけで AI に読んでもらう）。
     */
    suspend fun recognize(bitmap: Bitmap): String? {
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } finally {
            recognizer.close()
        }
    }
}
