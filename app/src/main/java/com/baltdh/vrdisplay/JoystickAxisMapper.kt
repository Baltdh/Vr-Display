package com.baltdh.vrdisplay

import kotlin.math.abs

/** Converts joystick stick/HAT positions into one-shot directional actions. */
class JoystickAxisMapper(private val deadZone: Float = DEFAULT_DEAD_ZONE) {
    init {
        require(deadZone in 0.1f..0.9f) { "deadZone must be between 0.1 and 0.9" }
    }

    private var activeAction: VrInputAction? = null

    fun update(x: Float, y: Float, hatX: Float, hatY: Float): VrInputAction? {
        val candidate = directionFromHat(hatX, hatY) ?: directionFromStick(x, y)
        if (candidate == activeAction) return null
        activeAction = candidate
        return candidate
    }

    fun reset() { activeAction = null }

    private fun directionFromHat(x: Float, y: Float): VrInputAction? {
        if (abs(x) < HAT_THRESHOLD && abs(y) < HAT_THRESHOLD) return null
        return if (abs(x) >= abs(y)) {
            if (x < 0f) VrInputAction.LEFT else VrInputAction.RIGHT
        } else {
            if (y < 0f) VrInputAction.UP else VrInputAction.DOWN
        }
    }

    private fun directionFromStick(x: Float, y: Float): VrInputAction? {
        if (abs(x) < deadZone && abs(y) < deadZone) return null
        return if (abs(x) >= abs(y)) {
            if (x < 0f) VrInputAction.LEFT else VrInputAction.RIGHT
        } else {
            if (y < 0f) VrInputAction.UP else VrInputAction.DOWN
        }
    }

    companion object {
        const val DEFAULT_DEAD_ZONE = 0.45f
        private const val HAT_THRESHOLD = 0.5f
    }
}
