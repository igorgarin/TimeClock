package com.example.timeclock

import kotlin.math.abs

/**
 * Frame index is derived from a wall-clock second boundary, while elapsed
 * intervals are measured with a monotonic clock. A wall-clock correction
 * re-aligns the monotonic anchor instead of accumulating timer-loop drift.
 */
class FrameClockCalculator(
    initialWallTimeMillis: Long,
    initialElapsedRealtimeNanos: Long
) {
    private var wallAnchorMillis = initialWallTimeMillis
    private var elapsedAnchorNanos = initialElapsedRealtimeNanos

    @Synchronized
    fun frameIndex(
        fps: Int,
        wallTimeMillis: Long,
        elapsedRealtimeNanos: Long
    ): Int {
        require(fps in SUPPORTED_FPS) { "Unsupported FPS: $fps" }

        val elapsedSinceAnchorMillis =
            (elapsedRealtimeNanos - elapsedAnchorNanos) / NANOS_PER_MILLI
        val monotonicWallEstimate = wallAnchorMillis + elapsedSinceAnchorMillis

        // Re-anchor after a system clock adjustment; ordinary scheduler jitter
        // does not change the anchor and therefore cannot accumulate as drift.
        if (abs(wallTimeMillis - monotonicWallEstimate) >= WALL_CLOCK_RESYNC_MS) {
            wallAnchorMillis = wallTimeMillis
            elapsedAnchorNanos = elapsedRealtimeNanos
        }

        val alignedMillis = if (
            abs(wallTimeMillis - monotonicWallEstimate) >= WALL_CLOCK_RESYNC_MS
        ) wallTimeMillis else monotonicWallEstimate

        val millisWithinSecond = Math.floorMod(alignedMillis, 1000L)
        return ((millisWithinSecond * fps) / 1000L).toInt().coerceIn(0, fps - 1)
    }

    companion object {
        val SUPPORTED_FPS = setOf(24, 25, 30, 50, 60, 100)
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val WALL_CLOCK_RESYNC_MS = 5L
    }
}

/**
 * A 60-slot cycle driven by display VSync. Each figure occupies 11 refreshes,
 * followed by one blank refresh. The sequence is independent of selected FPS.
 */
object ZenerCycle {
    private const val REFRESHES_PER_SECOND = 60L
    private const val REFRESHES_PER_FIGURE = 11L
    private const val SLOTS_PER_FIGURE = 12L
    private const val CYCLE_SLOTS = REFRESHES_PER_FIGURE * 5L + 5L
    private const val NANOS_PER_SECOND = 1_000_000_000L

    fun index(elapsedNanos: Long): Int? {
        val slot = Math.floorMod(elapsedNanos, NANOS_PER_SECOND) * REFRESHES_PER_SECOND /
            NANOS_PER_SECOND
        val slotInCycle = slot % CYCLE_SLOTS
        val figureIndex = (slotInCycle / SLOTS_PER_FIGURE).toInt()
        val refreshInFigure = slotInCycle % SLOTS_PER_FIGURE
        return figureIndex.takeIf { it < 5 && refreshInFigure < REFRESHES_PER_FIGURE }
    }
}

/** Twelve ring segments change continuously, five VSync slots per segment at 60 Hz. */
object ZenerRingCycle {
    private const val REFRESHES_PER_SECOND = 60L
    private const val SEGMENT_COUNT = 12L
    private const val REFRESHES_PER_SEGMENT = REFRESHES_PER_SECOND / SEGMENT_COUNT
    private const val NANOS_PER_SECOND = 1_000_000_000L

    fun index(elapsedNanos: Long): Int {
        val slot = Math.floorMod(elapsedNanos, NANOS_PER_SECOND) * REFRESHES_PER_SECOND /
            NANOS_PER_SECOND
        return (slot / REFRESHES_PER_SEGMENT).toInt()
    }
}
