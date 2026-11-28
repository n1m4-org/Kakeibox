package com.kakeibox

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kakeibox.receipt.ReceiptInbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 他のアプリの「共有」で送られたレシート画像を受け取る。
 * 共有元から借りた読み取り権限はこの画面にしか付かないため、画像をアプリのキャッシュへ写してから
 * 本体（レシート読み取り画面）に渡す。
 */
class ReceiptShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = sharedUris(intent)
        if (uris.isEmpty()) {
            finish()
            return
        }
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) { uris.take(10).mapNotNull(::copyToCache) }
            if (copied.isEmpty()) {
                Toast.makeText(applicationContext, "画像を受け取れませんでした", Toast.LENGTH_SHORT).show()
            } else {
                ReceiptInbox.offer(copied)
                startActivity(MainActivity.openReceiptIntent(this@ReceiptShareActivity))
            }
            finish()
        }
    }

    private fun sharedUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM),
        )
        Intent.ACTION_SEND_MULTIPLE -> (
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            ).orEmpty()
        else -> emptyList()
    }

    private fun copyToCache(uri: Uri): Uri? = runCatching {
        val dir = File(cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "shared_${System.nanoTime()}.img")
        contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
        Uri.fromFile(file)
    }.getOrNull()
}
