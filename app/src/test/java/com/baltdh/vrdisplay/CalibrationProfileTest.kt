package com.baltdh.vrdisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationProfileTest {
    @Test
    fun addingProfileCopiesCurrentSettingsAndActivatesIt() {
        val initial = CalibrationProfileState.default(
            CalibrationSettings(zoom = 1.8f, horizontal = 0.25f)
        )

        val updated = initial.addProfile("VR Box")

        assertEquals(2, updated.profiles.size)
        assertEquals("VR Box", updated.activeProfile.name)
        assertEquals(1.8f, updated.activeProfile.settings.zoom, 0f)
        assertEquals(0.25f, updated.activeProfile.settings.horizontal, 0f)
        assertNotEquals(initial.activeProfileId, updated.activeProfileId)
    }

    @Test
    fun duplicateNamesAreRejectedIgnoringCase() {
        val initial = CalibrationProfileState.default()
        val updated = initial.addProfile("Cardboard").addProfile("cardboard")

        assertEquals(2, updated.profiles.size)
        assertEquals("Cardboard", updated.activeProfile.name)
    }

    @Test
    fun selectingAnotherProfileRestoresItsIndependentSettings() {
        val initial = CalibrationProfileState.default()
        val withSecond = initial.addProfile("Headset B")
            .withActiveSettings(CalibrationSettings(zoom = 2f))
        val secondId = withSecond.activeProfileId
        val withFirstSelected = withSecond.selectProfile("default")

        assertEquals(1f, withFirstSelected.activeProfile.settings.zoom, 0f)
        assertEquals(secondId, withFirstSelected.selectProfile(secondId).activeProfileId)
        assertTrue(withFirstSelected.profiles.size == 2)
    }

    @Test
    fun profileLimitIsEnforced() {
        var state = CalibrationProfileState.default()
        repeat(CalibrationProfileState.MAX_PROFILES - 1) { state = state.addProfile("Profile $it") }

        val capped = state.addProfile("Too many")

        assertEquals(CalibrationProfileState.MAX_PROFILES, capped.profiles.size)
    }
}
