package com.kakeibox.data

/** 通知の判定対象（予算1つ分） */
data class AlertCandidate(
    val kind: BudgetKind,
    val target: BudgetTarget,
    /** 期間を表すキー（日予算は日付、週は週の開始日、月は期間の開始日） */
    val periodKey: String,
    val budget: Long,
    val spent: Long,
) {
    val ratio: Float get() = if (budget > 0) spent.toFloat() / budget else 1f
}

/** 送る通知 */
data class BudgetAlert(val candidate: AlertCandidate, val threshold: Int) {
    /** 二重通知を防ぐためのキー */
    val key: String get() = alertKey(candidate, threshold)
}

private fun alertKey(c: AlertCandidate, threshold: Int) = "${c.kind}:${c.target.key}:${c.periodKey}:$threshold"

/** 通知する閾値（%） */
val ALERT_THRESHOLDS = listOf(80, 100)

/**
 * まだ通知していない閾値を超えた予算を返す。同じ予算で80%と100%を同時に超えた場合は100%だけ。
 */
fun alertsToSend(candidates: List<AlertCandidate>, alreadySent: Set<String>): List<BudgetAlert> =
    candidates.mapNotNull { c ->
        val reached = ALERT_THRESHOLDS.filter { c.ratio * 100 >= it }.maxOrNull() ?: return@mapNotNull null
        if (alertKey(c, reached) in alreadySent) return@mapNotNull null
        // 100%通知済みなら80%は出さない
        if (ALERT_THRESHOLDS.any { it > reached && alertKey(c, it) in alreadySent }) return@mapNotNull null
        BudgetAlert(c, reached)
    }

/** 通知の本文 */
fun BudgetAlert.message(): Pair<String, String> {
    val c = candidate
    val unit = when (c.kind) {
        BudgetKind.DAILY -> "今日"
        BudgetKind.WEEKLY -> "今週"
        BudgetKind.MONTHLY -> "今月"
    }
    val title = if (threshold >= 100) "${unit}の${c.target.label}の予算をオーバーしました" else "${unit}の${c.target.label}の予算が残りわずかです"
    val remaining = c.budget - c.spent
    val body = "使用 ¥%,d / 予算 ¥%,d（%d%%）".format(c.spent, c.budget, (c.ratio * 100).toInt()) +
        if (remaining >= 0) "・残り ¥%,d".format(remaining) else "・¥%,d オーバー".format(-remaining)
    return title to body
}
