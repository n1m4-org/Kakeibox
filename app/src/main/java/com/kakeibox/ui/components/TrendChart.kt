package com.kakeibox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kakeibox.data.Period
import com.kakeibox.data.PeriodTotal
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 軸ラベル（開始日1なら「11月」、それ以外は「11/25」） */
fun Period.axisLabel(): String = if (startDay == 1) "${start.monthValue}月" else "${start.monthValue}/${start.dayOfMonth}"

/**
 * 収入・支出の棒グラフ＋収支の折れ線。タップで期間の値を表示する。
 * @param initialSelected 初期選択（既定は最後＝最新）
 */
@Composable
fun TrendChart(totals: List<PeriodTotal>, modifier: Modifier = Modifier, initialSelected: Int = totals.lastIndex) {
    if (totals.isEmpty()) return
    var selected by remember(totals.size) { mutableIntStateOf(initialSelected.coerceIn(0, totals.lastIndex)) }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlight = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier) {
        val sel = totals[selected.coerceIn(0, totals.lastIndex)]
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sel.period.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("収入 ${yen(sel.income)}", style = MaterialTheme.typography.labelMedium, color = IncomeColor)
            Text("支出 ${yen(sel.expense)}", style = MaterialTheme.typography.labelMedium, color = ExpenseColor)
        }
        Text(
            "収支 ${signedYen(sel.balance)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (sel.balance < 0) ExpenseColor else lineColor,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.End,
        )

        val top = totals.maxOf { max(max(it.income, it.expense), it.balance) }.coerceAtLeast(1)
        val bottom = min(0L, totals.minOf { it.balance })
        Canvas(
            Modifier.fillMaxWidth().height(180.dp).pointerInput(totals.size) {
                detectTapGestures { pos ->
                    selected = (pos.x / (size.width.toFloat() / totals.size)).toInt().coerceIn(0, totals.lastIndex)
                }
            },
        ) {
            val groupW = size.width / totals.size
            val range = (top - bottom).toFloat()
            fun y(v: Long) = size.height * (top - v) / range
            val zeroY = y(0)

            // 選択中の期間の背景
            drawRoundRect(highlight, Offset(groupW * selected, 0f), Size(groupW, size.height), CornerRadius(8f))
            drawLine(gridColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 2f)

            val barW = groupW * 0.28f
            totals.forEachIndexed { i, t ->
                val cx = groupW * i + groupW / 2
                if (t.income > 0) {
                    drawRoundRect(IncomeColor, Offset(cx - barW - 1f, y(t.income)), Size(barW, zeroY - y(t.income)), CornerRadius(4f))
                }
                if (t.expense > 0) {
                    drawRoundRect(ExpenseColor, Offset(cx + 1f, y(t.expense)), Size(barW, zeroY - y(t.expense)), CornerRadius(4f))
                }
            }
            // 収支の折れ線
            val path = Path()
            totals.forEachIndexed { i, t ->
                val p = Offset(groupW * i + groupW / 2, y(t.balance))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 4f))
            totals.forEachIndexed { i, t ->
                drawCircle(lineColor, radius = if (i == selected) 8f else 5f, center = Offset(groupW * i + groupW / 2, y(t.balance)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            totals.forEachIndexed { i, t ->
                Text(
                    t.period.axisLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("■ 収入", color = IncomeColor, style = MaterialTheme.typography.labelSmall)
            Text("■ 支出", color = ExpenseColor, style = MaterialTheme.typography.labelSmall)
            Text("● 収支", color = lineColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** 「+¥1,200（+15%）」形式の前期比 */
fun changeLabel(diff: Long, rate: Float?): String {
    val sign = if (diff > 0) "+" else if (diff < 0) "-" else "±"
    val pct = rate?.let { "（${if (it > 0) "+" else ""}${"%.0f".format(it * 100)}%）" } ?: ""
    return "$sign${yen(abs(diff))}$pct"
}
