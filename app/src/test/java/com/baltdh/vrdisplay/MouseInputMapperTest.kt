package com.baltdh.vrdisplay

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MouseInputMapperTest {
    @Test fun primaryAndSecondaryButtonsMapToConfirmAndBack() {
        assertEquals(VrInputAction.CONFIRM, MouseInputMapper.fromButtons(MotionEvent.BUTTON_PRIMARY))
        assertEquals(VrInputAction.BACK, MouseInputMapper.fromButtons(MotionEvent.BUTTON_SECONDARY))
    }
    @Test fun scrollMapsToVerticalNavigation() {
        assertEquals(VrInputAction.UP, MouseInputMapper.fromScroll(1f))
        assertEquals(VrInputAction.DOWN, MouseInputMapper.fromScroll(-1f))
        assertNull(MouseInputMapper.fromScroll(0.05f))
    }
    @Test fun unrelatedButtonsAreIgnored() {
        assertNull(MouseInputMapper.fromButtons(MotionEvent.BUTTON_TERTIARY))
    }
}
