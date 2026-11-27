package com.kakeibox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.Box
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

    /** 実際のサイズで配置（1段・カード・ドーナツ）を切り替える。[budgetWidgetLayout] */
    override val sizeMode: SizeMode = SizeMode.Exact

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
        val size = LocalSize.current
        val layout = budgetWidgetLayout(size)
        val compact = layout == BudgetWidgetLayout.ROW || layout == BudgetWidgetLayout.ROW_WIDE
        val tapAction = if (status == null) {
            actionStartActivity(MainActivity.openBudgetsIntent(context))
        } else {
            actionStartActivity<MainActivity>()
        }
        // ＋：カテゴリのウィジェットなら、そのカテゴリを選んだ状態でクイック入力を開く
        val addAction = actionStartActivity<QuickAddActivity>(
            (target as? BudgetTarget.Of)?.let { actionParametersOf(CATEGORY_PARAM to it.category.name) } ?: actionParametersOf(),
        )
        val icon: @Composable (Int) -> Unit = { iconSize ->
            Image(
                provider = ImageProvider(target.widgetIcon),
                contentDescription = target.label,
                colorFilter = ColorFilter.tint(ColorProvider(Color(target.color))),
                modifier = GlanceModifier.size(iconSize.dp),
            )
        }
        val addButton = @Composable {
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_add),
                contentDescription = "支出を追加",
                onClick = addAction,
                modifier = GlanceModifier.size(if (compact || layout == BudgetWidgetLayout.DONUT) 28.dp else 32.dp),
            )
        }
        val background = GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .clickable(tapAction)

        when (layout) {
            BudgetWidgetLayout.DONUT -> {
                // 2×2：中央にドーナツ（中にアイコン・残り・予算）、右上に＋
                val donut = (minOf(size.width, size.height) - 16.dp) * 0.8f
                Box(background.padding(8.dp), contentAlignment = Alignment.Center) {
                    Donut(context, donut, status) {
                        icon(16)
                        DonutCenter(status, donut)
                    }
                    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) { addButton() }
                }
                return
            }
            BudgetWidgetLayout.DONUT_LABELED -> {
                // 2×3：名前の行＋その下にドーナツ（中に残り・予算）
                Column(background.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Header(target, icon, addButton)
                    val donut = minOf(size.width - 24.dp, size.height - 16.dp - HEADER_HEIGHT - 8.dp)
                    Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                        Donut(context, donut, status) { DonutCenter(status, donut) }
                    }
                }
                return
            }
            BudgetWidgetLayout.STACK -> {
                // 4×2以上：名前の行・残り / 予算・予算1本分ずつのバー（2段目以降が超過分）
                Column(background.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Header(target, icon, addButton)
                    if (status == null) {
                        Text("未設定（タップで設定）", style = TextStyle(color = WidgetText.sub, fontSize = 13.sp))
                        return@Column
                    }
                    Row(verticalAlignment = Alignment.Bottom) { Amount(status, 20) }
                    Spacer(GlanceModifier.height(6.dp))
                    val barsHeight = size.height - 16.dp - HEADER_HEIGHT - 30.dp - 6.dp
                    val maxBars = ((barsHeight + STACK_GAP) / (STACK_BAR + STACK_GAP)).toInt().coerceIn(1, MAX_STACK_BARS)
                    // 今日の予算がマイナス（前日までの超過）のときは、1日の予算を1本分にする
                    val scale = if (status.todayBudget > 0) status.todayBudget else status.daily
                    StackedBarList(stackedBars(status.spentToday, scale, maxBars), status.ratio)
                }
                return
            }
            else -> Unit
        }

        Column(
            modifier = background.padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (compact) {
                // 縦1マス：アイコン・残り / 予算・＋ の1行と、細いバー。
                // 3マス幅以上はアイコンの横に名前を出し、金額は右寄せ。名前を優先し、入り切らなければ「/ 予算」を省く
                val wide = layout == BudgetWidgetLayout.ROW_WIDE || size.rowShowsLabel()
                val fit = if (wide) {
                    fitRow(
                        available = size.width.value - ROW_FIXED_WIDTH,
                        label = target.label,
                        amount = status?.let { signedYen(it.remaining) } ?: "未設定",
                        suffix = status?.let { budgetSuffix(it.todayBudget) } ?: "",
                    )
                } else {
                    null
                }
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    icon(18)
                    Spacer(GlanceModifier.width(6.dp))
                    if (fit != null) {
                        Text(
                            fit.label,
                            style = TextStyle(color = WidgetText.main, fontSize = ROW_LABEL_SP.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    if (status == null) {
                        Text(
                            "未設定",
                            style = TextStyle(color = WidgetText.sub, fontSize = 13.sp),
                            modifier = if (wide) GlanceModifier else GlanceModifier.defaultWeight(),
                        )
                    } else {
                        Row(
                            if (wide) GlanceModifier else GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.Bottom,
                        ) { Amount(status, ROW_AMOUNT_SP.toInt(), showSuffix = fit?.showSuffix ?: true) }
                    }
                    if (wide) Spacer(GlanceModifier.width(10.dp))
                    addButton()
                }
                if (status != null) {
                    Spacer(GlanceModifier.height(3.dp))
                    Bar(status, 4)
                }
                return@Column
            }

            // 横長の2マス以上：アイコン・名前・＋／残り / 予算／バー
            Header(target, icon, addButton)
            if (status == null) {
                Text("未設定（タップで設定）", style = TextStyle(color = WidgetText.sub, fontSize = 13.sp))
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom) { Amount(status, 20) }
            Spacer(GlanceModifier.height(4.dp))
            Bar(status, 6)
        }
    }

    /** 積み上げバー：1段目は使用率の色、2段目以降（超過分）は赤。収まらない分は最後の段に「×本数」 */
    @Composable
    private fun StackedBarList(bars: StackedBars, ratio: Float) {
        // Glance の Column は子が10個までなので、バーだけの Column にまとめ、間隔は余白で取る
        Column(GlanceModifier.fillMaxWidth()) {
            bars.fills.forEachIndexed { i, fill ->
                val last = i == bars.fills.lastIndex && bars.more > 0
                Row(
                    GlanceModifier.fillMaxWidth().padding(top = if (i > 0) STACK_GAP else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinearProgressIndicator(
                        progress = fill,
                        modifier = GlanceModifier.defaultWeight().height(STACK_BAR),
                        color = if (i == 0) WidgetText.bar(ratio) else WidgetText.over,
                        backgroundColor = WidgetText.track,
                    )
                    if (last) {
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            "×${bars.more}",
                            style = TextStyle(color = WidgetText.over, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    /** アイコン・名前・＋ の行 */
    @Composable
    private fun Header(target: BudgetTarget, icon: @Composable (Int) -> Unit, addButton: @Composable () -> Unit) {
        Row(GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            icon(18)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                target.label,
                style = TextStyle(color = WidgetText.main, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            addButton()
        }
    }

    /** ドーナツ（輪は Bitmap）と、その中央の内容。未設定なら輪は下地だけ */
    @Composable
    private fun Donut(context: Context, diameter: Dp, status: DailyBudgetStatus?, center: @Composable () -> Unit) {
        val d = diameter.coerceAtLeast(40.dp)
        val px = (d.value * context.resources.displayMetrics.density).toInt().coerceIn(40, MAX_DONUT_PX)
        val ratio = status?.ratio ?: 0f
        val bitmap = remember(px, ratio) { donutBitmap(px, ratio) }
        Box(GlanceModifier.size(d), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.size(d),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
        }
    }

    /** ドーナツの中：残り（大・超過は赤）と「/ 予算」。文字の大きさは直径に合わせる */
    @Composable
    private fun DonutCenter(status: DailyBudgetStatus?, diameter: Dp) {
        if (status == null) {
            Text("未設定", style = TextStyle(color = WidgetText.sub, fontSize = 12.sp))
            return
        }
        val amountSize = (diameter.value / 7f).coerceIn(12f, 22f)
        Text(
            signedYen(status.remaining),
            style = TextStyle(
                color = if (status.remaining < 0) WidgetText.over else WidgetText.main,
                fontSize = amountSize.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            budgetSuffix(status.todayBudget).trim(),
            style = TextStyle(color = WidgetText.sub, fontSize = (amountSize * 0.55f).coerceAtLeast(10f).sp),
            maxLines = 1,
        )
    }

    /** 残り（大・超過は赤）＋「 / 今日の予算」（小） */
    @Composable
    private fun Amount(status: DailyBudgetStatus, size: Int, showSuffix: Boolean = true) {
        Text(
            signedYen(status.remaining),
            style = TextStyle(
                color = if (status.remaining < 0) WidgetText.over else WidgetText.main,
                fontSize = size.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        if (showSuffix) {
            Text(budgetSuffix(status.todayBudget), style = TextStyle(color = WidgetText.sub, fontSize = ROW_SUFFIX_SP.sp), maxLines = 1)
        }
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

        /** 名前付きの1段で、名前と金額以外に使う幅（左右の余白24・アイコン18＋6・名前と金額の間8・＋の前10・＋28） */
        private const val ROW_FIXED_WIDTH = 94f

        /** 名前の行の高さ */
        private val HEADER_HEIGHT = 32.dp

        /** 積み上げバーの太さと間隔 */
        private val STACK_BAR = 8.dp
        private val STACK_GAP = 6.dp

        /** Glance の Column に入れられる子の上限 */
        private const val MAX_STACK_BARS = 10

        /** ドーナツの Bitmap の最大サイズ（ウィジェットに渡せる画像の容量に収める） */
        private const val MAX_DONUT_PX = 420

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
