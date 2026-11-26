package com.kakeibox.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random

/** アクリルのカードの角丸 */
val AcrylicShape = RoundedCornerShape(20.dp)

/** 微細なノイズ（アクリルのざらつき）。一度だけ作って使い回す */
private val noiseBrush: ShaderBrush by lazy {
    val size = 96
    val rnd = Random(7)
    val px = IntArray(size * size) {
        val v = rnd.nextInt(256)
        (rnd.nextInt(60) shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bmp = Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/**
 * アクリル効果：背後の背景を強くぼかして透かし、半透明の色・ノイズ・光の縁を重ねる。
 * [KakeiboxBackground] の中で使う（外では半透明の色だけになる）。
 */
fun Modifier.acrylic(shape: Shape = AcrylicShape): Modifier = composed {
    val backdrop = LocalBackdrop.current
    val dark = isDarkScheme()
    val tint = if (dark) Color(0xFF1C2423).copy(alpha = 0.66f) else Color.White.copy(alpha = 0.5f)
    val edge = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.7f)
    var position by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { position = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val image = backdrop?.acrylic
            val root = backdrop?.rootSize
            if (image != null && root != null && root.width > 0) {
                drawImage(
                    image,
                    dstOffset = IntOffset(-position.x.roundToInt(), -position.y.roundToInt()),
                    dstSize = root,
                    filterQuality = FilterQuality.Low,
                )
            }
            drawRect(tint)
            drawRect(noiseBrush, alpha = if (dark) 0.35f else 0.5f)
        }
        .border(1.dp, edge, shape)
}

/** アクリルのカード */
@Composable
fun AcrylicCard(modifier: Modifier = Modifier, shape: Shape = AcrylicShape, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(modifier.acrylic(shape), content = content)
    }
}

/**
 * 画面全体のアクリルのパネル。中の ListItem などの面を透明にして、アクリルが透けて見えるようにする。
 */
@Composable
fun AcrylicPanel(modifier: Modifier = Modifier, shape: Shape = AcrylicShape, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(modifier.acrylic(shape)) { TransparentSurfaces { content() } }
    }
}

/** 中の面（surface・background）を透明にする */
@Composable
fun TransparentSurfaces(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = scheme.copy(surface = Color.Transparent, background = Color.Transparent),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        CompositionLocalProvider(content = content)
    }
}

/** 画面全体のパネルの外側の余白 */
fun Modifier.panelPadding(): Modifier = this.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)

/** アクリルのパネルの中に置くカード（うっすら色を付けた半透明） */
@Composable
fun innerCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))

/** ダークの配色か（面を透明にした中でも判定できるよう、文字色の明るさで見る） */
@Composable
@ReadOnlyComposable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
