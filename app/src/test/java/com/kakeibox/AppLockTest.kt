package com.kakeibox

import com.kakeibox.security.AppLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockTest {
    @Before fun setUp() = AppLock.reset()

    @Test
    fun disabledNeverLocks() = assertFalse(AppLock.shouldLock(false, now = 0))

    @Test
    fun locksOnFirstStart() = assertTrue(AppLock.shouldLock(true, now = 0))

    @Test
    fun staysUnlockedForShortBackground() {
        AppLock.markUnlocked()
        assertFalse(AppLock.shouldLock(true, now = 1_000))
        AppLock.onBackground(now = 10_000)
        assertFalse(AppLock.shouldLock(true, now = 69_999))
    }

    @Test
    fun locksAfterOneMinuteInBackground() {
        AppLock.markUnlocked()
        AppLock.onBackground(now = 10_000)
        assertTrue(AppLock.shouldLock(true, now = 70_000))
    }
}
