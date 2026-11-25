package com.kakeibox

import com.kakeibox.data.AlertCandidate
import com.kakeibox.data.BudgetKind
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.alertsToSend
import com.kakeibox.data.message
import com.kakeibox.notify.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class NotifyTest {
    private fun c(spent: Long, budget: Long = 1_000, target: BudgetTarget = BudgetTarget.All) =
        AlertCandidate(BudgetKind.DAILY, target, "2026-11-25", budget, spent)

    @Test
    fun belowThresholdNoAlert() = assertEquals(emptyList<Any>(), alertsToSend(listOf(c(799)), emptySet()))

    @Test
    fun crossing80() {
        val a = alertsToSend(listOf(c(800)), emptySet()).single()
        assertEquals(80, a.threshold)
        assertEquals("DAILY:ALL:2026-11-25:80", a.key)
    }

    @Test
    fun jumpingStraightTo100SendsOnly100() = assertEquals(listOf(100), alertsToSend(listOf(c(1_200)), emptySet()).map { it.threshold })

    @Test
    fun notRepeated() {
        assertEquals(emptyList<Any>(), alertsToSend(listOf(c(900)), setOf("DAILY:ALL:2026-11-25:80")))
        assertEquals(listOf(100), alertsToSend(listOf(c(1_000)), setOf("DAILY:ALL:2026-11-25:80")).map { it.threshold })
        assertEquals(emptyList<Any>(), alertsToSend(listOf(c(1_500)), setOf("DAILY:ALL:2026-11-25:100")))
    }

    @Test
    fun separateTargetsAreIndependent() {
        val list = alertsToSend(listOf(c(900), c(900, target = BudgetTarget.Of(Category.FOOD))), setOf("DAILY:ALL:2026-11-25:80"))
        assertEquals(listOf(BudgetTarget.Of(Category.FOOD)), list.map { it.candidate.target })
    }

    @Test
    fun messageMentionsTargetAndOverAmount() {
        val (title, body) = alertsToSend(listOf(c(1_200, target = BudgetTarget.Of(Category.FOOD))), emptySet()).single().message()
        assertEquals("今日の食費の予算をオーバーしました", title)
        assertTrue(body, body.contains("¥200 オーバー"))
    }

    @Test
    fun reminderDelay() {
        assertEquals(Duration.ofMinutes(30), Reminder.delayUntil(LocalDateTime.of(2026, 11, 25, 20, 30), 21 * 60))
        // 過ぎていたら翌日
        assertEquals(Duration.ofHours(23), Reminder.delayUntil(LocalDateTime.of(2026, 11, 25, 22, 0), 21 * 60))
    }
}
