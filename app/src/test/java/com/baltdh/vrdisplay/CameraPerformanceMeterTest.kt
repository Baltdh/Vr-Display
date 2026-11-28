package com.baltdh.vrdisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraPerformanceMeterTest {
    @Test
    fun publishesAverageFpsAndProcessingTimeAfterWindow() {
        val meter = CameraPerformanceMeter(windowDurationMs = 1_000L)

        for (timestamp in 1_000L..1_900L step 100L) {
            assertNull(meter.recordFrame(timestamp, 4f, 640, 480))
        }

        val snapshot = meter.recordFrame(2_000L, 6f, 640, 480)

        requireNotNull(snapshot)
        assertEquals(10f, snapshot.framesPerSecond, 0.001f)
        assertEquals(4.1818f, snapshot.averageProcessingMs, 0.001f)
        assertEquals(640, snapshot.width)
        assertEquals(480, snapshot.height)
    }

    @Test
    fun startsANewMeasurementWindowAfterPublishing() {
        val meter = CameraPerformanceMeter(windowDurationMs = 100L)
        assertNull(meter.recordFrame(10L, 2f, 640, 480))
        val first = meter.recordFrame(110L, 4f, 640, 480)
        assertEquals(10f, requireNotNull(first).framesPerSecond, 0.001f)

        assertNull(meter.recordFrame(120L, 8f, 1280, 720))
        val second = meter.recordFrame(210L, 10f, 1280, 720)
        assertEquals(10f, requireNotNull(second).framesPerSecond, 0.001f)
        assertEquals(9f, second.averageProcessingMs, 0.001f)
        assertEquals(1280, second.width)
        assertEquals(720, second.height)
    }

    @Test
    fun invalidProcessingDurationIsCountedAsZero() {
        val meter = CameraPerformanceMeter(windowDurationMs = 1L)
        assertNull(meter.recordFrame(1L, Float.NaN, 320, 240))
        val snapshot = meter.recordFrame(2L, Float.POSITIVE_INFINITY, 320, 240)
        assertEquals(1_000f, requireNotNull(snapshot).framesPerSecond, 0.001f)
        assertEquals(0f, snapshot.averageProcessingMs, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveWindow() {
        CameraPerformanceMeter(windowDurationMs = 0L)
    }
}

    
    @Test
    fun repeatedTimestampsAreHandledWithoutDivisionByZero() {
        val meter = CameraPerformanceMeter(windowDurationMs = 1L)
        assertNull(meter.recordFrame(100L, 3f, 640, 480))
        assertNull(meter.recordFrame(100L, 3f, 640, 480))
        val snapshot = meter.recordFrame(101L, 3f, 640, 480)
        assertEquals(2_000f, requireNotNull(snapshot).framesPerSecond, 0.001f)
    }
