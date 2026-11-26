package com.kakeibox.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.kakeibox.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** 背景画像のファイル */
fun backgroundFile(context: Context) = File(context.filesDir, "background.jpg")

/** 背景の見た目の設定 */
data class BackgroundSpec(val version: Long, val blur: Int, val brightness: Int, val dark: Boolean)

/** 描画済みの背景（ページ用と、アクリル用の強くぼかした小さい版） */
@Stable
class Backdrop {
    var page by mutableStateOf<ImageBitmap?>(null)
    var acrylic by mutableStateOf<ImageBitmap?>(null)
    /** 背景を描いている領域（＝画面）のサイズ */
    var rootSize by mutableStateOf(IntSize.Zero)
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

object BackdropRenderer {
    /** ページ背景を縮小する倍率（ぼかすときだけ） */
    private const val PAGE_SCALE = 4
    /** アクリル用の縮小倍率とぼかし半径（縮小後のピクセル） */
    private const val ACRYLIC_SCALE = 8
    private const val ACRYLIC_RADIUS = 5

    /** ぼかし 0〜100 → 縮小後のぼかし半径 */
    fun pageRadius(blur: Int): Int = (blur / 100f * 18).roundToInt()

    suspend fun render(context: Context, spec: BackgroundSpec, size: IntSize): Pair<ImageBitmap, ImageBitmap> =
        withContext(Dispatchers.Default) {
            val w = size.width.coerceAtLeast(1)
            val h = size.height.coerceAtLeast(1)
            val radius = pageRadius(spec.blur)
            // ぼかすときは小さく描いてからぼかす（軽く、拡大時になめらかになる）
            val scale = if (radius > 0) PAGE_SCALE else 1
            renderFrom(source(context, spec, max(1, w / scale), max(1, h / scale)), spec, size)
        }

    /** 元の画像（ページ用の大きさに切り抜き済み）から、ページ用とアクリル用を作る */
    suspend fun renderFrom(base: Bitmap, spec: BackgroundSpec, size: IntSize): Pair<ImageBitmap, ImageBitmap> {
        val radius = pageRadius(spec.blur)
        applyBrightness(base, spec.brightness)
        coroutineContext.ensureActive()
        val page = if (radius > 0) blurred(base, radius) else base
        coroutineContext.ensureActive()
        val small = Bitmap.createScaledBitmap(base, max(1, size.width / ACRYLIC_SCALE), max(1, size.height / ACRYLIC_SCALE), true)
        val acrylic = blurred(small, ACRYLIC_RADIUS + radius / 3)
        return page.asImageBitmap() to acrylic.asImageBitmap()
    }

    /** 背景画像（中央で切り抜き）か、パレットのグラデーション */
    private fun source(context: Context, spec: BackgroundSpec, w: Int, h: Int): Bitmap {
        if (spec.version != 0L) {
            loadCropped(backgroundFile(context), w, h)?.let { return it }
        }
        return gradient(w, h, spec.dark)
    }

    private fun loadCropped(file: File, w: Int, h: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        val src = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // 中央切り抜き（ContentScale.Crop と同じ）
        val s = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val cw = (w / s).roundToInt().coerceAtMost(src.width)
        val ch = (h / s).roundToInt().coerceAtMost(src.height)
        val left = (src.width - cw) / 2
        val top = (src.height - ch) / 2
        Canvas(out).drawBitmap(src, Rect(left, top, left + cw, top + ch), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** パレットの柔らかいグラデーション＋淡い円 */
    fun gradient(w: Int, h: Int, dark: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val (top, bottom) = if (dark) {
            Palette.Teal.darken(0.78f) to Palette.Rose.darken(0.8f)
        } else {
            Palette.Blush.lighten(0.2f) to Palette.Sage.lighten(0.35f)
        }
        paint.shader = LinearGradient(0f, 0f, w * 0.4f, h.toFloat(), top.toArgb(), bottom.toArgb(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        val blobAlpha = if (dark) 0.35f else 0.55f
        fun blob(cx: Float, cy: Float, r: Float, color: Color) {
            paint.shader = RadialGradient(
                cx * w, cy * h, r * max(w, h),
                color.copy(alpha = blobAlpha).toArgb(), color.copy(alpha = 0f).toArgb(), Shader.TileMode.CLAMP,
            )
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
        blob(0.85f, 0.12f, 0.45f, if (dark) Palette.Rose.darken(0.3f) else Palette.Petal)
        blob(0.1f, 0.55f, 0.5f, if (dark) Palette.Teal.darken(0.3f) else Palette.Sage)
        blob(0.75f, 0.9f, 0.4f, if (dark) Palette.Sage.darken(0.5f) else Palette.Rose.lighten(0.3f))
        return bmp
    }

    /** 明るさ：マイナスは黒、プラスは白を重ねる */
    private fun applyBrightness(bmp: Bitmap, brightness: Int) {
        if (brightness == 0) return
        val color = if (brightness < 0) Color.Black else Color.White
        Canvas(bmp).drawColor(color.copy(alpha = kotlin.math.abs(brightness) / 100f * 0.75f).toArgb())
    }

    private fun blurred(bmp: Bitmap, radius: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(blurPixels(px, w, h, radius), 0, w, 0, 0, w, h)
        return out
    }
}

/**
 * アプリの背景（画像またはパレットのグラデーション）を全画面に描き、
 * 子の [acrylic] に背景のぼかし画像を配る。
 * @param preset テスト用：描画済みの背景を直接渡す
 */
@Composable
fun KakeiboxBackground(preset: Backdrop? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isDarkScheme()
    val fallback = MaterialTheme.colorScheme.background
    val backdrop = preset ?: remember { Backdrop() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val size = with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        backdrop.rootSize = size
        if (preset == null) {
            val settings by AppGraph.settingsStore(context).settings.collectAsState()
            val spec = BackgroundSpec(settings.backgroundVersion, settings.backgroundBlur, settings.backgroundBrightness, dark)
            LaunchedEffect(spec, size) {
                // スライダー操作中は、指が止まってから描き直す
                if (backdrop.page != null) delay(80)
                val (page, acrylic) = BackdropRenderer.render(context, spec, size)
                backdrop.page = page
                backdrop.acrylic = acrylic
            }
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(fallback)
                backdrop.page?.let {
                    drawImage(it, dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()), filterQuality = FilterQuality.Low)
                }
            },
        ) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) { content() }
        }
    }
}
