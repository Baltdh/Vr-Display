package com.baltdh.vrdisplay

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputMappingSettingsTest {
    @Test
    fun defaultsMatchCommonDpadAndConfirmKeys() {
        val settings = InputMappingSettings()
        assertEquals(VrInputAction.CONFIRM, settings.actionFor(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(VrInputAction.BACK, settings.actionFor(KeyEvent.KEYCODE_BACK))
        assertEquals(VrInputAction.UP, settings.actionFor(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(VrInputAction.DOWN, settings.actionFor(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(VrInputAction.LEFT, settings.actionFor(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(VrInputAction.RIGHT, settings.actionFor(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    @Test
    fun remappingToUnusedKeyKeepsOtherBindings() {
        val remapped = InputMappingSettings()
            .withKey(VrInputAction.CONFIRM, KeyEvent.KEYCODE_BUTTON_A)

        assertEquals(KeyEvent.KEYCODE_BUTTON_A, remapped.keyFor(VrInputAction.CONFIRM))
        assertEquals(VrInputAction.CONFIRM, remapped.actionFor(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(VrInputAction.UP, remapped.actionFor(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test
    fun assigningAnAlreadyUsedKeySwapsRatherThanDuplicates() {
        val remapped = InputMappingSettings()
            .withKey(VrInputAction.CONFIRM, KeyEvent.KEYCODE_DPAD_UP)

        assertEquals(KeyEvent.KEYCODE_DPAD_UP, remapped.keyFor(VrInputAction.CONFIRM))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, remapped.keyFor(VrInputAction.UP))
        assertEquals(VrInputAction.CONFIRM, remapped.actionFor(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(VrInputAction.UP, remapped.actionFor(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun analogDeadZoneIsNormalizedToSupportedRange() {
        assertEquals(0.1f, InputMappingSettings(analogDeadZone = -2f).normalized().analogDeadZone)
        assertEquals(0.9f, InputMappingSettings(analogDeadZone = 2f).normalized().analogDeadZone)
        assertEquals(JoystickAxisMapper.DEFAULT_DEAD_ZONE, InputMappingSettings(analogDeadZone = Float.NaN).normalized().analogDeadZone)
    }

    @Test
    fun unmappedKeyReturnsNull() {
        assertNull(InputMappingSettings().actionFor(KeyEvent.KEYCODE_UNKNOWN))
    }
}
