package com.baltdh.vrdisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JoystickAxisMapperTest {
    @Test fun stickMapsFourDirections() {
        val mapper = JoystickAxisMapper()
        assertEquals(VrInputAction.RIGHT, mapper.update(0.8f, 0f, 0f, 0f))
        mapper.update(0f, 0f, 0f, 0f)
        assertEquals(VrInputAction.LEFT, mapper.update(-0.8f, 0f, 0f, 0f))
        mapper.update(0f, 0f, 0f, 0f)
        assertEquals(VrInputAction.UP, mapper.update(0f, -0.8f, 0f, 0f))
        mapper.update(0f, 0f, 0f, 0f)
        assertEquals(VrInputAction.DOWN, mapper.update(0f, 0.8f, 0f, 0f))
    }

    @Test fun deadZoneDoesNotGenerateMovement() {
        assertNull(JoystickAxisMapper().update(0.2f, -0.2f, 0f, 0f))
    }

    @Test fun sameDirectionDoesNotRepeatUntilNeutral() {
        val mapper = JoystickAxisMapper()
        assertEquals(VrInputAction.RIGHT, mapper.update(0.9f, 0f, 0f, 0f))
        assertNull(mapper.update(0.95f, 0f, 0f, 0f))
        assertNull(mapper.update(0f, 0f, 0f, 0f))
        assertEquals(VrInputAction.RIGHT, mapper.update(0.9f, 0f, 0f, 0f))
    }

    @Test fun customDeadZoneChangesStickSensitivity() {
        val mapper = JoystickAxisMapper()
        assertNull(mapper.update(0.3f, 0f, 0f, 0f, deadZone = 0.5f))
        assertEquals(VrInputAction.RIGHT, mapper.update(0.3f, 0f, 0f, 0f, deadZone = 0.2f))
    }

    @Test fun hatTakesPriorityOverStick() {
        assertEquals(VrInputAction.LEFT, JoystickAxisMapper().update(0.9f, 0f, -1f, 0f))
    }

    @Test fun switchingDirectionEmitsNewActionImmediately() {
        val mapper = JoystickAxisMapper()
        assertEquals(VrInputAction.RIGHT, mapper.update(0.9f, 0f, 0f, 0f))
        assertEquals(VrInputAction.UP, mapper.update(0f, -0.9f, 0f, 0f))
    }

    @Test fun stickNoiseInsideReleaseBandDoesNotRetriggerDirection() {
        val mapper = JoystickAxisMapper()
        assertEquals(VrInputAction.RIGHT, mapper.update(0.46f, 0f, 0f, 0f))

        assertNull(mapper.update(0.40f, 0f, 0f, 0f))
        assertNull(mapper.update(0.34f, 0f, 0f, 0f))
        assertNull(mapper.update(0.33f, 0f, 0f, 0f))
        assertEquals(VrInputAction.RIGHT, mapper.update(0.46f, 0f, 0f, 0f))
    }

    @Test fun hatUsesSeparateReleaseThreshold() {
        val mapper = JoystickAxisMapper()
        assertEquals(VrInputAction.LEFT, mapper.update(0f, 0f, -0.6f, 0f))

        assertNull(mapper.update(0f, 0f, -0.4f, 0f))
        assertNull(mapper.update(0f, 0f, -0.34f, 0f))
        assertEquals(VrInputAction.LEFT, mapper.update(0f, 0f, -0.6f, 0f))
    }

    @Test fun nonFiniteAxisSamplesAreIgnored() {
        val mapper = JoystickAxisMapper()
        assertNull(
            mapper.update(
                x = Float.NaN,
                y = Float.POSITIVE_INFINITY,
                hatX = Float.NaN,
                hatY = Float.NEGATIVE_INFINITY
            )
        )
        assertEquals(VrInputAction.RIGHT, mapper.update(0.8f, 0f, 0f, 0f))
    }
}
