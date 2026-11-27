package com.baltdh.vrdisplay

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.inputMappingDataStore by preferencesDataStore(name = "vr_display_input_mappings")

enum class VrInputAction(val id: String, val label: String) {
    CONFIRM("confirm", "Confirmar"),
    BACK("back", "Voltar"),
    UP("up", "Cima"),
    DOWN("down", "Baixo"),
    LEFT("left", "Esquerda"),
    RIGHT("right", "Direita")
}

data class InputMappingSettings(
    val confirm: Int = KeyEvent.KEYCODE_DPAD_CENTER,
    val back: Int = KeyEvent.KEYCODE_BACK,
    val up: Int = KeyEvent.KEYCODE_DPAD_UP,
    val down: Int = KeyEvent.KEYCODE_DPAD_DOWN,
    val left: Int = KeyEvent.KEYCODE_DPAD_LEFT,
    val right: Int = KeyEvent.KEYCODE_DPAD_RIGHT
) {
    fun keyFor(action: VrInputAction): Int = when (action) {
        VrInputAction.CONFIRM -> confirm
        VrInputAction.BACK -> back
        VrInputAction.UP -> up
        VrInputAction.DOWN -> down
        VrInputAction.LEFT -> left
        VrInputAction.RIGHT -> right
    }

    fun actionFor(keyCode: Int): VrInputAction? =
        VrInputAction.entries.firstOrNull { keyFor(it) == keyCode }

    fun withKey(action: VrInputAction, keyCode: Int): InputMappingSettings {
        val previousKey = keyFor(action)
        var updated = copy(
            confirm = if (action == VrInputAction.CONFIRM) keyCode else confirm,
            back = if (action == VrInputAction.BACK) keyCode else back,
            up = if (action == VrInputAction.UP) keyCode else up,
            down = if (action == VrInputAction.DOWN) keyCode else down,
            left = if (action == VrInputAction.LEFT) keyCode else left,
            right = if (action == VrInputAction.RIGHT) keyCode else right
        )
        val collision = VrInputAction.entries.firstOrNull {
            it != action && updated.keyFor(it) == keyCode
        }
        if (collision != null) {
            updated = updated.copy(
                confirm = if (collision == VrInputAction.CONFIRM) previousKey else updated.confirm,
                back = if (collision == VrInputAction.BACK) previousKey else updated.back,
                up = if (collision == VrInputAction.UP) previousKey else updated.up,
                down = if (collision == VrInputAction.DOWN) previousKey else updated.down,
                left = if (collision == VrInputAction.LEFT) previousKey else updated.left,
                right = if (collision == VrInputAction.RIGHT) previousKey else updated.right
            )
        }
        return updated
    }
}

object InputMappingStore {
    private fun key(action: VrInputAction) = intPreferencesKey("input_" + action.id)

    fun observe(context: Context): Flow<InputMappingSettings> =
        context.inputMappingDataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun save(context: Context, settings: InputMappingSettings) {
        context.inputMappingDataStore.edit { prefs ->
            VrInputAction.entries.forEach { action ->
                prefs[key(action)] = settings.keyFor(action)
            }
        }
    }

    private fun Preferences.toSettings() = InputMappingSettings(
        confirm = this[key(VrInputAction.CONFIRM)] ?: KeyEvent.KEYCODE_DPAD_CENTER,
        back = this[key(VrInputAction.BACK)] ?: KeyEvent.KEYCODE_BACK,
        up = this[key(VrInputAction.UP)] ?: KeyEvent.KEYCODE_DPAD_UP,
        down = this[key(VrInputAction.DOWN)] ?: KeyEvent.KEYCODE_DPAD_DOWN,
        left = this[key(VrInputAction.LEFT)] ?: KeyEvent.KEYCODE_DPAD_LEFT,
        right = this[key(VrInputAction.RIGHT)] ?: KeyEvent.KEYCODE_DPAD_RIGHT
    )
}

fun inputKeyLabel(keyCode: Int): String =
    KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
