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
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                "${if (period.startDay == 1) "${period.start.monthValue}月" else "今月"}の支出",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
            Text(
                yen(total),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
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
