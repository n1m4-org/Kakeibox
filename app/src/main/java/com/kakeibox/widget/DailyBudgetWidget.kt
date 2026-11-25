package com.kakeibox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kakeibox.AppGraph
import com.kakeibox.MainActivity
import com.kakeibox.QuickAddActivity
import com.kakeibox.R
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.DailyBudgetStatus
import com.kakeibox.data.Period
import com.kakeibox.data.computeDailyBudget
import com.kakeibox.data.spentByDay
import com.kakeibox.ui.color
import com.kakeibox.ui.monthlyBudgetLabel
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.WarningColor
import com.kakeibox.ui.signedYen
import com.kakeibox.ui.yen
import java.time.LocalDate

/**
 * 1つの予算対象（全体 or カテゴリ）について、今日の予算の使用状況を表示するウィジェット。
 * 対象は配置時に [DailyBudgetWidgetConfigActivity] で選び、ウィジェットごとの state に保存する。
 * 対象が保存されていないウィジェットは「全体」を表示する。
 */
class DailyBudgetWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = AppGraph.repository(context)
        val budgetStore = AppGraph.budgetStore(context)
        val settingsStore = AppGraph.settingsStore(context)
        val initialSpent = repo.dailyExpense(settingsStore.currentPeriod())

        provideContent {
            // セッション中の更新（記録・予算・対象の変更、日付の切り替わり）にも追従するよう、すべて購読する
            val target = BudgetTarget.fromKey(currentState(TARGET_KEY))
            val appSettings by settingsStore.settings.collectAsState()
            val today = LocalDate.now()
            val period = Period.of(today, appSettings.periodStartDay)
            val spentFlow = remember(period) { repo.observeDailyExpense(period) }
            val spent by spentFlow.collectAsState(initialSpent)
            val settings by budgetStore.daily.collectAsState()

            val status = settings[target]?.let {
                computeDailyBudget(it.amount, LocalDate.ofEpochDay(it.since), today, spent.spentByDay(target), period.start)
            }
            GlanceTheme { Content(context, target, status, period) }
        }
    }

    @Composable
    private fun Content(context: Context, target: BudgetTarget, status: DailyBudgetStatus?, period: Period) {
        val sub = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        val tapAction = if (status == null) {
            actionStartActivity(MainActivity.openBudgetsIntent(context))
        } else {
            actionStartActivity<MainActivity>()
        }
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(tapAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(target.widgetIcon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(Color(target.color))),
                    modifier = GlanceModifier.size(20.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    "${target.label}の予算",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                )
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
            Text("使用 ${yen(status.spentToday)} / ${signedYen(status.todayBudget)}", style = sub, maxLines = 1)
            Text(monthlyBudgetLabel(status.daily, period), style = sub, maxLines = 1)
        }
    }

    companion object {
        /** ウィジェットごとに保存する予算対象のキー */
        val TARGET_KEY: Preferences.Key<String> = stringPreferencesKey("budget_target")
    }
}

class DailyBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyBudgetWidget()
}

/** ウィジェット用のアイコン（アプリ内と同じ線画アイコンのベクター版） */
val BudgetTarget.widgetIcon: Int
    get() = when (this) {
        BudgetTarget.All -> R.drawable.ic_budget_all
        is BudgetTarget.Of -> when (category) {
            Category.FOOD -> R.drawable.ic_budget_food
            Category.DAILY -> R.drawable.ic_budget_daily
            Category.TRANSPORT -> R.drawable.ic_budget_transport
            Category.HOUSING -> R.drawable.ic_budget_housing
            Category.UTILITY -> R.drawable.ic_budget_utility
            Category.COMMUNICATION -> R.drawable.ic_budget_communication
            Category.SOCIAL -> R.drawable.ic_budget_social
            Category.HOBBY -> R.drawable.ic_budget_hobby
            Category.MEDICAL -> R.drawable.ic_budget_medical
            Category.CLOTHES -> R.drawable.ic_budget_clothes
            else -> R.drawable.ic_budget_other_expense
        }
    }
