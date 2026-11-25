package com.kakeibox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kakeibox.data.CategoryTotal
import com.kakeibox.ui.yen

/** ドーナツ型の円グラフ。中央に合計を表示。 */
@Composable
fun DonutChart(items: List<CategoryTotal>, total: Long, size: Dp = 220.dp) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.18f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            if (items.isEmpty()) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            var start = -90f
            items.forEach { item ->
                val sweep = item.ratio * 360f
                drawArc(Color(item.category.color), start, sweep, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("合計", style = MaterialTheme.typography.labelMedium)
            Text(yen(total), style = MaterialTheme.typography.titleLarge)
        }
    }
}
