package com.baltdh.vrdisplay

import kotlin.math.abs

/**
 * Converts joystick stick/HAT positions into one-shot directional actions.
 *
 * Separate activation and release thresholds prevent noisy analog samples from
 * repeatedly arming the same direction near the configured dead zone.
 */
class JoystickAxisMapper(private val deadZone: Float = DEFAULT_DEAD_ZONE) {
    init {
        require(deadZone in 0.1f..0.9f) { "deadZone must be between 0.1 and 0.9" }
    }

    private var activeAction: VrInputAction? = null

    fun update(
        x: Float,
        y: Float,
        hatX: Float,
        hatY: Float,
        deadZone: Float = this.deadZone
    ): VrInputAction? {
        val safeDeadZone = if (deadZone.isFinite()) {
            deadZone.coerceIn(0.1f, 0.9f)
        } else {
            this.deadZone
        }
        val safeX = x.normalizedAxis()
        val safeY = y.normalizedAxis()
        val safeHatX = hatX.normalizedAxis()
        val safeHatY = hatY.normalizedAxis()

        val candidate = directionFromAxes(safeHatX, safeHatY, HAT_ACTIVATION_THRESHOLD)
            ?: directionFromAxes(safeX, safeY, safeDeadZone)

        if (candidate == activeAction) return null

        if (candidate == null && activeAction != null) {
            val retainedByHat = directionFromAxes(
                safeHatX,
                safeHatY,
                HAT_RELEASE_THRESHOLD
            ) == activeAction
            val retainedByStick = directionFromAxes(
                safeX,
                safeY,
                safeDeadZone * STICK_RELEASE_RATIO
            ) == activeAction

            if (retainedByHat || retainedByStick) return null
        }

        activeAction = candidate
        return candidate
    }

    fun reset() {
        activeAction = null
    }

    private fun Float.normalizedAxis(): Float =
        if (isFinite()) coerceIn(-1f, 1f) else 0f

    private fun directionFromAxes(x: Float, y: Float, threshold: Float): VrInputAction? {
        if (abs(x) < threshold && abs(y) < threshold) return null
        return if (abs(x) >= abs(y)) {
            if (x < 0f) VrInputAction.LEFT else VrInputAction.RIGHT
        } else {
            if (y < 0f) VrInputAction.UP else VrInputAction.DOWN
        }
    }

    companion object {
        const val DEFAULT_DEAD_ZONE = 0.45f
        private const val HAT_ACTIVATION_THRESHOLD = 0.5f
        private const val HAT_RELEASE_THRESHOLD = 0.35f
        private const val STICK_RELEASE_RATIO = 0.75f
    }
}
