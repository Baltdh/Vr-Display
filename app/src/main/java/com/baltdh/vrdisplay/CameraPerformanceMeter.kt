package com.baltdh.vrdisplay

data class CameraPerformanceSnapshot(
    val framesPerSecond: Float,
    val averageProcessingMs: Float,
    val width: Int,
    val height: Int
)

/**
 * Produces a low-frequency performance snapshot from frames processed by CameraX.
 * Call from the single analyzer thread; no synchronization is required.
 */
class CameraPerformanceMeter(
    private val windowDurationMs: Long = DEFAULT_WINDOW_DURATION_MS
) {
    init {
        require(windowDurationMs > 0L) { "windowDurationMs must be positive" }
    }

    private var windowStartedAt: Long? = null
    private var frameCount = 0
    private var totalProcessingMs = 0.0
    private var latestWidth = 0
    private var latestHeight = 0

    fun recordFrame(
        timestampMs: Long,
        processingMs: Float,
        width: Int,
        height: Int
    ): CameraPerformanceSnapshot? {
        require(timestampMs >= 0L) { "timestampMs must not be negative" }
        require(width > 0 && height > 0) { "frame dimensions must be positive" }

        val safeProcessingMs = processingMs.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val startedAt = windowStartedAt
        if (startedAt == null) {
            windowStartedAt = timestampMs
        }
        frameCount += 1
        totalProcessingMs += safeProcessingMs
        latestWidth = width
        latestHeight = height

        val elapsedMs = timestampMs - (windowStartedAt ?: timestampMs)
        if (elapsedMs < windowDurationMs || elapsedMs <= 0L) return null

        val snapshot = CameraPerformanceSnapshot(
            framesPerSecond = frameCount * 1000f / elapsedMs,
            averageProcessingMs = (totalProcessingMs / frameCount).toFloat(),
            width = latestWidth,
            height = latestHeight
        )
        windowStartedAt = timestampMs
        frameCount = 0
        totalProcessingMs = 0.0
        return snapshot
    }

    companion object {
        const val DEFAULT_WINDOW_DURATION_MS = 1_000L
    }
}
