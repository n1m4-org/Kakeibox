package com.kakeibox

import com.kakeibox.ui.theme.blurPixels
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun radiusZeroReturnsCopy() {
        val src = intArrayOf(black, white, black, white)
        val out = blurPixels(src, 2, 2, 0)
        assertArrayEquals(src, out)
        assertTrue(out !== src)
    }

    @Test
    fun uniformImageStaysUniform() {
        val c = 0xFF51A696.toInt()
        val out = blurPixels(IntArray(15 * 9) { c }, 15, 9, 4)
        assertTrue(out.all { it == c })
    }

    @Test
    fun pointSpreadsToNeighborsAndKeepsEdgesInRange() {
        val w = 11; val h = 11
        val src = IntArray(w * h) { black }
        src[5 * w + 5] = white
        val out = blurPixels(src, w, h, 2, passes = 1)
        val center = out[5 * w + 5] and 0xFF
        val neighbor = out[5 * w + 6] and 0xFF
        val far = out[0] and 0xFF
        assertTrue("center=$center neighbor=$neighbor", center in 1..254 && neighbor > 0)
        assertEquals(0, far)
        // アルファは不透明のまま
        assertTrue(out.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun radiusLargerThanImageDoesNotCrash() {
        val out = blurPixels(IntArray(3 * 2) { white }, 3, 2, 50)
        assertTrue(out.all { it == white })
    }
}
