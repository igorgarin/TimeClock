package com.example.timeclock

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameClockCalculatorTest {
    @Test
    fun frameIndexCyclesAtSecondBoundaryForSupportedRates() {
        for (fps in listOf(24, 25, 30, 50, 60, 100)) {
            val calculator = FrameClockCalculator(1_700_000_000_000L, 10_000_000_000L)
            val start = 1_700_000_000_000L
            val elapsed = 10_000_000_000L
            assertEquals(0, calculator.frameIndex(fps, start, elapsed))
            assertEquals(fps - 1, calculator.frameIndex(fps, start + 999, elapsed + 999_000_000L))
            assertEquals(0, calculator.frameIndex(fps, start + 1000, elapsed + 1_000_000_000L))
        }
    }

    @Test
    fun zenerCycleUsesElevenRefreshesThenOneBlankForEachFigure() {
        for (slot in 0 until 60) {
            val timestampNanos = (slot * 1_000_000_000L + 59L) / 60L
            val expectedFigure = (slot / 12).takeIf { slot % 12 < 11 }
            assertEquals(expectedFigure, ZenerCycle.index(timestampNanos))
        }

        assertEquals(0, ZenerCycle.index(1_000_000_000L))
    }

    @Test
    fun zenerRingCycleChangesEveryFiveRefreshesAcrossTwelveSegmentsWithoutBlankSlots() {
        for (slot in 0 until 60) {
            val timestampNanos = (slot * 1_000_000_000L + 59L) / 60L
            assertEquals(slot / 5, ZenerRingCycle.index(timestampNanos))
        }

        assertEquals(0, ZenerRingCycle.index(1_000_000_000L))
    }
}
