package com.baltdh.vrdisplay

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.vrDisplayDataStore by preferencesDataStore(name = "vr_display_settings")

data class CalibrationSettings(
    val ipd: Float = 0.5f,
    val zoom: Float = 1f,
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val rotation: Int = 0
) {
    fun normalized(): CalibrationSettings = copy(
        ipd = if (ipd.isFinite()) ipd.coerceIn(0f, 1f) else 0.5f,
        zoom = if (zoom.isFinite()) zoom.coerceIn(0.7f, 2.5f) else 1f,
        horizontal = if (horizontal.isFinite()) horizontal.coerceIn(-1f, 1f) else 0f,
        vertical = if (vertical.isFinite()) vertical.coerceIn(-1f, 1f) else 0f,
        rotation = ((rotation % 360) + 360) % 360
    )
}

data class CalibrationProfile(
    val id: String,
    val name: String,
    val settings: CalibrationSettings
)

data class CalibrationProfileState(
    val activeProfileId: String,
    val profiles: List<CalibrationProfile>
) {
    val activeProfile: CalibrationProfile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()

    fun withActiveSettings(settings: CalibrationSettings): CalibrationProfileState =
        copy(profiles = profiles.map { profile ->
            if (profile.id == activeProfile.id) profile.copy(settings = settings.normalized()) else profile
        })

    fun selectProfile(id: String): CalibrationProfileState =
        if (profiles.any { it.id == id }) copy(activeProfileId = id) else this

    fun addProfile(name: String): CalibrationProfileState {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || profiles.size >= MAX_PROFILES ||
            profiles.any { it.name.equals(cleanName, ignoreCase = true) }
        ) return this

        val profile = CalibrationProfile(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            settings = activeProfile.settings.normalized()
        )
        return copy(activeProfileId = profile.id, profiles = profiles + profile)
    }

    fun normalized(): CalibrationProfileState {
        val safeProfiles = profiles
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }
            .take(MAX_PROFILES)
            .map { it.copy(name = it.name.trim(), settings = it.settings.normalized()) }
            .ifEmpty { default().profiles }
        val safeActiveId = safeProfiles.firstOrNull { it.id == activeProfileId }?.id
            ?: safeProfiles.first().id
        return CalibrationProfileState(safeActiveId, safeProfiles)
    }

    companion object {
        const val MAX_PROFILES = 20

        fun default(settings: CalibrationSettings = CalibrationSettings()) =
            CalibrationProfileState(
                activeProfileId = "default",
                profiles = listOf(
                    CalibrationProfile("default", "Padrão", settings.normalized())
                )
            )
    }
}

object CalibrationSettingsStore {
    private val ipdKey = floatPreferencesKey("calibration_ipd")
    private val zoomKey = floatPreferencesKey("calibration_zoom")
    private val horizontalKey = floatPreferencesKey("calibration_horizontal")
    private val verticalKey = floatPreferencesKey("calibration_vertical")
    private val rotationKey = intPreferencesKey("calibration_rotation")
    private val profilesKey = stringPreferencesKey("calibration_profiles_v1")
    private val activeProfileKey = stringPreferencesKey("active_calibration_profile")

    fun observe(context: Context): Flow<CalibrationProfileState> =
        context.vrDisplayDataStore.data.map { preferences ->
            preferences.toProfileState().normalized()
        }

    suspend fun save(context: Context, state: CalibrationProfileState) {
        val safe = state.normalized()
        val activeSettings = safe.activeProfile.settings
        context.vrDisplayDataStore.edit { preferences ->
            preferences[profilesKey] = encodeProfiles(safe.profiles)
            preferences[activeProfileKey] = safe.activeProfileId

            // Keep the original keys synchronized for safe migration from older builds.
            preferences[ipdKey] = activeSettings.ipd
            preferences[zoomKey] = activeSettings.zoom
            preferences[horizontalKey] = activeSettings.horizontal
            preferences[verticalKey] = activeSettings.vertical
            preferences[rotationKey] = activeSettings.rotation
        }
    }

    private fun Preferences.toProfileState(): CalibrationProfileState {
        val rawProfiles = this[profilesKey]
        if (rawProfiles.isNullOrBlank()) {
            val legacySettings = CalibrationSettings(
                ipd = this[ipdKey] ?: 0.5f,
                zoom = this[zoomKey] ?: 1f,
                horizontal = this[horizontalKey] ?: 0f,
                vertical = this[verticalKey] ?: 0f,
                rotation = this[rotationKey] ?: 0
            )
            return CalibrationProfileState.default(legacySettings)
        }

        val decoded = decodeProfiles(rawProfiles)
        val activeId = this[activeProfileKey] ?: decoded.firstOrNull()?.id.orEmpty()
        return CalibrationProfileState(activeId, decoded).normalized()
    }

    private fun encodeProfiles(profiles: List<CalibrationProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            val settings = profile.settings.normalized()
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("ipd", settings.ipd.toDouble())
                    .put("zoom", settings.zoom.toDouble())
                    .put("horizontal", settings.horizontal.toDouble())
                    .put("vertical", settings.vertical.toDouble())
                    .put("rotation", settings.rotation)
            )
        }
        return array.toString()
    }

    private fun decodeProfiles(raw: String): List<CalibrationProfile> = try {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id", "")
                val name = item.optString("name", "")
                if (id.isBlank() || name.isBlank()) continue
                val settings = CalibrationSettings(
                    ipd = item.optDouble("ipd", 0.5).toFloat(),
                    zoom = item.optDouble("zoom", 1.0).toFloat(),
                    horizontal = item.optDouble("horizontal", 0.0).toFloat(),
                    vertical = item.optDouble("vertical", 0.0).toFloat(),
                    rotation = item.optInt("rotation", 0)
                ).normalized()
                add(CalibrationProfile(id, name, settings))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
