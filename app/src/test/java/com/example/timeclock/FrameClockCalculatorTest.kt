package com.example.timeclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameClockCalculatorTest {
    @Test
    fun frameIndexCyclesAtSecondBoundaryForSupportedRates() {
        for (fps in listOf(24, 25, 30, 50, 60)) {
            val calculator = FrameClockCalculator(1_700_000_000_000L, 10_000_000_000L)
            val start = 1_700_000_000_000L
            val elapsed = 10_000_000_000L
            assertEquals(0, calculator.frameIndex(fps, start, elapsed))
            assertEquals(fps - 1, calculator.frameIndex(fps, start + 999, elapsed + 999_000_000L))
            assertEquals(0, calculator.frameIndex(fps, start + 1000, elapsed + 1_000_000_000L))
        }
    }

    @Test
    fun zenerCycleUsesElevenRefreshesThenOneBlankAtSixtyHertz() {
        val refreshNanos = 1_000_000_000L / 60L
        for (figure in 0..4) {
            val first = figure * 12L
            repeat(11) { offset ->
                assertEquals(figure, ZenerCycle.index((first + offset) * refreshNanos))
            }
            assertNull(ZenerCycle.index((first + 11L) * refreshNanos))
        }
        assertEquals(0, ZenerCycle.index(60L * refreshNanos))
    }
}
