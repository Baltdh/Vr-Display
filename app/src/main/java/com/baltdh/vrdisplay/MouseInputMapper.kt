package com.baltdh.vrdisplay

import android.view.MotionEvent

object MouseInputMapper {
    fun fromButtons(buttonState: Int): VrInputAction? = when {
        buttonState and MotionEvent.BUTTON_PRIMARY != 0 -> VrInputAction.CONFIRM
        buttonState and MotionEvent.BUTTON_SECONDARY != 0 -> VrInputAction.BACK
        else -> null
    }

    fun fromScroll(vertical: Float): VrInputAction? = when {
        vertical > 0.1f -> VrInputAction.UP
        vertical < -0.1f -> VrInputAction.DOWN
        else -> null
    }
}
