package com.kakeibox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.components.CircleIconButton
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kakeibox.AppGraph
import com.kakeibox.MainActivity
import com.kakeibox.QuickAddActivity
import com.kakeibox.R
import com.kakeibox.data.DailyBudgetStatus
import com.kakeibox.data.computeDailyBudget
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.WarningColor
import com.kakeibox.ui.yen
import java.time.LocalDate
import java.time.YearMonth

/** 今日の予算の使用状況を表示するホーム画面ウィジェット */
class DailyBudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val setting = AppGraph.budgetStore(context).daily.value
        val spent = AppGraph.repository(context).dailyExpense(YearMonth.now())
        val status = computeDailyBudget(setting.amount, LocalDate.ofEpochDay(setting.since), LocalDate.now(), spent)
        provideContent {
            GlanceTheme { Content(status) }
        }
    }

    @Composable
    private fun Content(status: DailyBudgetStatus?) {
        val sub = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("今日の予算", style = sub, modifier = GlanceModifier.defaultWeight())
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "支出を追加",
                    onClick = actionStartActivity<QuickAddActivity>(),
                    modifier = GlanceModifier.size(36.dp),
                )
            }

            if (status == null) {
                Text(
                    "1日の予算が未設定です\nタップして設定",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                )
                return@Column
            }

            val over = status.remaining < 0
            Text(
                if (over) "${yen(-status.remaining)} オーバー" else "残り ${yen(status.remaining)}",
                style = TextStyle(
                    color = if (over) ColorProvider(ExpenseColor) else GlanceTheme.colors.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = status.ratio.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                color = when {
                    status.ratio >= 1f -> ColorProvider(ExpenseColor)
                    status.ratio >= 0.8f -> ColorProvider(WarningColor)
                    else -> GlanceTheme.colors.primary
                },
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text("使用 ${yen(status.spentToday)} / ${yen(status.todayBudget)}", style = sub)
        }
    }
}

class DailyBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyBudgetWidget()
}
