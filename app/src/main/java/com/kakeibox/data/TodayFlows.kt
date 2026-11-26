package com.kakeibox.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 今日を含む期間（日付・締め日の変更に追従） */
fun currentPeriodFlow(today: Flow<LocalDate>, startDay: Flow<Int>): Flow<Period> =
    combine(today, startDay) { d, s -> Period.of(d, s) }.distinctUntilChanged()

/**
 * 今日の予算（対象ごと）。日付が変わると、前日までの余り・超過を繰り越して再計算する。
 * @param spentIn 期間内の日別・カテゴリ別支出（DBの購読）
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun todayStatusesFlow(
    today: Flow<LocalDate>,
    startDay: Flow<Int>,
    settings: Flow<Map<BudgetTarget, DailyBudgetSetting>>,
    spentIn: (Period) -> Flow<List<DayCategoryTotal>>,
): Flow<List<TargetStatus>> =
    combine(today, startDay) { d, s -> d to Period.of(d, s) }
        .distinctUntilChanged()
        .flatMapLatest { (d, p) ->
            combine(spentIn(p), settings) { spent, st -> computeTodayStatuses(st, spent, d, p.start) }
        }

/** 今週（月〜日）の週予算。日付が変わって週が替わると対象の週も替わる。 */
@OptIn(ExperimentalCoroutinesApi::class)
fun weeklyStatusesFlow(
    today: Flow<LocalDate>,
    budgets: Flow<Map<BudgetTarget, Long>>,
    spentIn: (Week) -> Flow<List<DayCategoryTotal>>,
): Flow<List<PlainBudgetStatus>> =
    today.map { Week.of(it) }
        .distinctUntilChanged()
        .flatMapLatest { w -> combine(spentIn(w), budgets) { spent, b -> plainBudgetStatuses(b, spent) } }
