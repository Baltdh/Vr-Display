package com.baltdh.vrdisplay

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vrDisplayDataStore by preferencesDataStore(name = "vr_display_settings")

data class CalibrationSettings(
    val ipd: Float = 0.5f,
    val zoom: Float = 1f,
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val rotation: Int = 0
) {
    fun normalized(): CalibrationSettings = copy(
        ipd = ipd.coerceIn(0f, 1f),
        zoom = zoom.coerceIn(0.7f, 2.5f),
        horizontal = horizontal.coerceIn(-1f, 1f),
        vertical = vertical.coerceIn(-1f, 1f),
        rotation = ((rotation % 360) + 360) % 360
    )
}

object CalibrationSettingsStore {
    private val ipdKey = floatPreferencesKey("calibration_ipd")
    private val zoomKey = floatPreferencesKey("calibration_zoom")
    private val horizontalKey = floatPreferencesKey("calibration_horizontal")
    private val verticalKey = floatPreferencesKey("calibration_vertical")
    private val rotationKey = intPreferencesKey("calibration_rotation")

    fun observe(context: Context): Flow<CalibrationSettings> =
        context.vrDisplayDataStore.data.map { preferences ->
            preferences.toCalibrationSettings().normalized()
        }

    suspend fun save(context: Context, settings: CalibrationSettings) {
        val safe = settings.normalized()
        context.vrDisplayDataStore.edit { preferences ->
            preferences[ipdKey] = safe.ipd
            preferences[zoomKey] = safe.zoom
            preferences[horizontalKey] = safe.horizontal
            preferences[verticalKey] = safe.vertical
            preferences[rotationKey] = safe.rotation
        }
    }

    private fun Preferences.toCalibrationSettings() = CalibrationSettings(
        ipd = this[ipdKey] ?: 0.5f,
        zoom = this[zoomKey] ?: 1f,
        horizontal = this[horizontalKey] ?: 0f,
        vertical = this[verticalKey] ?: 0f,
        rotation = this[rotationKey] ?: 0
    )
}
