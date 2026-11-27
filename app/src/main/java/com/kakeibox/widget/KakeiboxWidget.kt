package com.kakeibox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.layout.Row
import androidx.glance.layout.size
import com.kakeibox.R
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kakeibox.AppGraph
import com.kakeibox.MainActivity
import com.kakeibox.QuickAddActivity
import com.kakeibox.ui.yen
import com.kakeibox.data.Period
import com.kakeibox.data.Today
import java.time.LocalDate

/** 今月の支出合計と「支出を追加」ボタンを表示するホーム画面ウィジェット */
class KakeiboxWidget : GlanceAppWidget() {
    /** 縦1マスと2マス以上で配置を切り替える */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WIDGET_SMALL, WIDGET_TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = AppGraph.repository(context)
        val settingsStore = AppGraph.settingsStore(context)
        val initial = repo.periodExpense(settingsStore.currentPeriod())
        provideContent {
            // セッション中の更新（記録の追加・月替わり・締め日変更）にも追従するため、合計はFlowで購読する
            val settings by settingsStore.settings.collectAsState()
            val today by Today.date.collectAsState()
            val period = Period.of(today, settings.periodStartDay)
            val flow = remember(period) { repo.observePeriodExpense(period) }
            val total by flow.collectAsState(initial)
            GlanceTheme { Content(period, total) }
        }
    }

    @Composable
    private fun Content(period: Period, total: Long) {
        val compact = LocalSize.current.isCompact()
        val monthLabel = if (period.startDay == 1) "${period.start.monthValue}月" else "今月"
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            if (compact) {
                // 縦1マス：「11月 ¥3,679」と＋
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$monthLabel ", style = TextStyle(color = WidgetText.sub, fontSize = 12.sp), maxLines = 1)
                    Text(
                        yen(total),
                        style = TextStyle(color = WidgetText.main, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_add),
                        contentDescription = "支出を追加",
                        onClick = actionStartActivity<QuickAddActivity>(),
                        modifier = GlanceModifier.size(28.dp),
                    )
                }
                return@Column
            }
            Text("${monthLabel}の支出", style = TextStyle(color = WidgetText.sub, fontSize = 12.sp))
            Text(yen(total), style = TextStyle(color = WidgetText.main, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = "＋ 支出を追加",
                onClick = actionStartActivity<QuickAddActivity>(),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

class KakeiboxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KakeiboxWidget()
}
