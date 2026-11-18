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
        val SUPPORTED_FPS = setOf(24, 25, 30, 50, 60)
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val WALL_CLOCK_RESYNC_MS = 5L
    }
}

/**
 * 60 Hz cadence: 11 refreshes per figure, followed by one blank refresh.
 * Five groups of 12 refreshes make one 60-refresh / one-second cycle.
 */
object ZenerCycle {
    private const val DISPLAY_HZ = 60L
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val REFRESHES_PER_FIGURE = 11L
    private const val REFRESHES_PER_SLOT = 12L
    private const val FIGURE_COUNT = 5L
    private const val CYCLE_REFRESHES = REFRESHES_PER_SLOT * FIGURE_COUNT

    fun index(elapsedRealtimeNanos: Long): Int? {
        val refresh = Math.floorDiv(elapsedRealtimeNanos * DISPLAY_HZ, NANOS_PER_SECOND)
        val position = Math.floorMod(refresh, CYCLE_REFRESHES)
        if (position % REFRESHES_PER_SLOT == REFRESHES_PER_FIGURE) return null
        return (position / REFRESHES_PER_SLOT).toInt()
    }
}
