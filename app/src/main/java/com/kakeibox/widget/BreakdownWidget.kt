package com.kakeibox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kakeibox.AppGraph
import com.kakeibox.MainActivity
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.Period
import com.kakeibox.data.Today
import com.kakeibox.data.categoryRanking
import com.kakeibox.ui.yen
import java.time.LocalDate

/** 今月のカテゴリ別支出ランキング（上位5件）を表示するウィジェット */
class BreakdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = AppGraph.repository(context)
        val settingsStore = AppGraph.settingsStore(context)
        val initial = repo.dailyExpense(settingsStore.currentPeriod())
        provideContent {
            val settings by settingsStore.settings.collectAsState()
            val today by Today.date.collectAsState()
            val period = Period.of(today, settings.periodStartDay)
            val flow = remember(period) { repo.observeDailyExpense(period) }
            val spent by flow.collectAsState(initial)
            val total = spent.sumOf { it.total }
            GlanceTheme(colors = WidgetColors) { Content(period, total, categoryRanking(spent, TOP)) }
        }
    }

    @Composable
    private fun Content(period: Period, total: Long, ranking: List<Pair<Category, Long>>) {
        val sub = TextStyle(color = WidgetText.sub, fontSize = 11.sp)
        Column(
            GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (period.startDay == 1) "${period.start.monthValue}月" else "今月"}の支出内訳",
                    style = TextStyle(color = WidgetText.main, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(yen(total), style = TextStyle(color = WidgetText.main, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(6.dp))
            if (ranking.isEmpty()) {
                Text("まだ支出の記録がありません", style = sub)
                return@Column
            }
            ranking.forEach { (c, amount) ->
                val color = ColorProvider(Color(c.color))
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(BudgetTarget.Of(c).widgetIcon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color),
                        modifier = GlanceModifier.size(16.dp),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Row(GlanceModifier.fillMaxWidth()) {
                            Text(c.label, style = TextStyle(color = WidgetText.main, fontSize = 12.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
                            Text("${yen(amount)}・${amount * 100 / total}%", style = sub)
                        }
                        LinearProgressIndicator(
                            progress = amount.toFloat() / total,
                            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                            color = color,
                            backgroundColor = WidgetText.track,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val TOP = 5
    }
}

class BreakdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BreakdownWidget()
}
