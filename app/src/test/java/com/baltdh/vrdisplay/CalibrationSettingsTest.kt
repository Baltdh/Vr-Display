package com.baltdh.vrdisplay

import org.junit.Assert.assertEquals
import org.junit.Test

class CalibrationSettingsTest {
    @Test
    fun defaultsAreSafeForNewInstallations() {
        val settings = CalibrationSettings()

        assertEquals(0.5f, settings.ipd)
        assertEquals(1f, settings.zoom)
        assertEquals(0f, settings.horizontal)
        assertEquals(0f, settings.vertical)
        assertEquals(0, settings.rotation)
    }

    @Test
    fun normalizedClampsValuesToSupportedRanges() {
        val settings = CalibrationSettings(
            ipd = -0.4f,
            zoom = 4f,
            horizontal = -3f,
            vertical = 2f,
            rotation = 450
        ).normalized()

        assertEquals(0f, settings.ipd)
        assertEquals(2.5f, settings.zoom)
        assertEquals(-1f, settings.horizontal)
        assertEquals(1f, settings.vertical)
        assertEquals(90, settings.rotation)
    }

    @Test
    fun normalizedWrapsNegativeRotation() {
        assertEquals(270, CalibrationSettings(rotation = -90).normalized().rotation)
    }
}
