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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
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
import com.kakeibox.data.Today
import com.kakeibox.data.computeDailyBudget
import com.kakeibox.data.spentByDay
import com.kakeibox.ui.color
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

    /** 縦1マスと2マス以上で配置を切り替える */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WIDGET_SMALL, WIDGET_TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = AppGraph.repository(context)
        val budgetStore = AppGraph.budgetStore(context)
        val settingsStore = AppGraph.settingsStore(context)
        val initialSpent = repo.dailyExpense(settingsStore.currentPeriod())

        provideContent {
            // セッション中の更新（記録・予算・対象の変更、日付の切り替わり）にも追従するよう、すべて購読する
            val target = BudgetTarget.fromKey(currentState(TARGET_KEY))
            val appSettings by settingsStore.settings.collectAsState()
            val today by Today.date.collectAsState()
            val period = Period.of(today, appSettings.periodStartDay)
            val spentFlow = remember(period) { repo.observeDailyExpense(period) }
            val spent by spentFlow.collectAsState(initialSpent)
            val settings by budgetStore.daily.collectAsState()

            val status = settings[target]?.let {
                computeDailyBudget(it.amount, LocalDate.ofEpochDay(it.since), today, spent.spentByDay(target), period.start)
            }
            GlanceTheme { Content(context, target, status) }
        }
    }

    @Composable
    private fun Content(context: Context, target: BudgetTarget, status: DailyBudgetStatus?) {
        val compact = LocalSize.current.isCompact()
        val tapAction = if (status == null) {
            actionStartActivity(MainActivity.openBudgetsIntent(context))
        } else {
            actionStartActivity<MainActivity>()
        }
        // ＋：カテゴリのウィジェットなら、そのカテゴリを選んだ状態でクイック入力を開く
        val addAction = actionStartActivity<QuickAddActivity>(
            (target as? BudgetTarget.Of)?.let { actionParametersOf(CATEGORY_PARAM to it.category.name) } ?: actionParametersOf(),
        )
        val icon = @Composable {
            Image(
                provider = ImageProvider(target.widgetIcon),
                contentDescription = target.label,
                colorFilter = ColorFilter.tint(ColorProvider(Color(target.color))),
                modifier = GlanceModifier.size(18.dp),
            )
        }
        val addButton = @Composable {
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_add),
                contentDescription = "支出を追加",
                onClick = addAction,
                modifier = GlanceModifier.size(if (compact) 28.dp else 32.dp),
            )
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp)
                .clickable(tapAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (compact) {
                // 縦1マス：アイコン・残り / 予算・＋ の1行と、細いバー
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(GlanceModifier.width(6.dp))
                    if (status == null) {
                        Text("未設定", style = TextStyle(color = WidgetText.sub, fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
                    } else {
                        Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Bottom) { Amount(status, 16) }
                    }
                    addButton()
                }
                if (status != null) {
                    Spacer(GlanceModifier.height(3.dp))
                    Bar(status, 4)
                }
                return@Column
            }

            // 2マス以上：アイコン・名前・＋／残り / 予算／バー
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    target.label,
                    style = TextStyle(color = WidgetText.main, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                )
                addButton()
            }
            if (status == null) {
                Text("未設定（タップで設定）", style = TextStyle(color = WidgetText.sub, fontSize = 13.sp))
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom) { Amount(status, 20) }
            Spacer(GlanceModifier.height(4.dp))
            Bar(status, 6)
        }
    }

    /** 残り（大・超過は赤）＋「 / 今日の予算」（小） */
    @Composable
    private fun Amount(status: DailyBudgetStatus, size: Int) {
        Text(
            signedYen(status.remaining),
            style = TextStyle(
                color = if (status.remaining < 0) WidgetText.over else WidgetText.main,
                fontSize = size.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(budgetSuffix(status.todayBudget), style = TextStyle(color = WidgetText.sub, fontSize = 11.sp), maxLines = 1)
    }

    @Composable
    private fun Bar(status: DailyBudgetStatus, height: Int) {
        LinearProgressIndicator(
            progress = status.ratio.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(height.dp),
            color = WidgetText.bar(status.ratio),
            backgroundColor = WidgetText.track,
        )
    }

    companion object {
        /** ウィジェットごとに保存する予算対象のキー */
        val TARGET_KEY: Preferences.Key<String> = stringPreferencesKey("budget_target")

        /** Glance は ActionParameters を Intent の extra として渡す */
        private val CATEGORY_PARAM = ActionParameters.Key<String>(QuickAddActivity.EXTRA_CATEGORY)
    }
}

class DailyBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyBudgetWidget()
}

/** 残りの後ろに付ける「 / 2,400」（今日の予算がマイナスなら「 / -300」） */
internal fun budgetSuffix(budget: Long): String =
    " / ${if (budget < 0) "-" else ""}${String.format(java.util.Locale.JAPAN, "%,d", kotlin.math.abs(budget))}"
