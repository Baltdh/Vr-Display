package com.baltdh.vrdisplay

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private var lastInput by mutableStateOf("Nenhum evento recebido")
    private var lastMotionUpdateAt = 0L
    private val joystickAxisMapper = JoystickAxisMapper()
    var inputMappings by mutableStateOf(InputMappingSettings())
        private set
    var pendingMappingAction by mutableStateOf<VrInputAction?>(null)
        private set
    var mappedActionEvent by mutableStateOf<Pair<VrInputAction, Long>?>(null)
        private set
    private var mappedEventSequence = 0L
    private var capturedKeyCode: Int? = null

    fun armInputMapping(action: VrInputAction) {
        pendingMappingAction = action
    }

    fun cancelInputMapping() {
        pendingMappingAction = null
    }

    fun restoreInputMappings() {
        inputMappings = InputMappingSettings()
    }

    fun loadInputMappings(settings: InputMappingSettings) {
        inputMappings = settings
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val device = event.device?.name ?: "dispositivo desconhecido"
        val pending = pendingMappingAction
        if (event.action == KeyEvent.ACTION_DOWN && pending != null) {
            inputMappings = inputMappings.withKey(pending, event.keyCode)
            pendingMappingAction = null
            capturedKeyCode = event.keyCode
            lastInput = "Mapeado • ${pending.label} = ${inputKeyLabel(event.keyCode)} • device=$device"
            return true
        }
        if (capturedKeyCode == event.keyCode) {
            if (event.action == KeyEvent.ACTION_UP) capturedKeyCode = null
            return true
        }
        val mapped = inputMappings.actionFor(event.keyCode)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && mapped != null) {
            mappedEventSequence += 1
            mappedActionEvent = mapped to mappedEventSequence
        }
        val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        val mappedText = mapped?.let { " • ação=${it.label}" } ?: ""
        lastInput = "$action • ${KeyEvent.keyCodeToString(event.keyCode)} • ${event.displayLabel} " +
            "• scan=${event.scanCode} • repeat=${event.repeatCount} " +
            "• device=$device • source=0x${event.source.toString(16)}$mappedText"
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        if (isJoystick && event.actionMasked == MotionEvent.ACTION_MOVE) {
            val mapped = joystickAxisMapper.update(
                x = event.getAxisValue(MotionEvent.AXIS_X),
                y = event.getAxisValue(MotionEvent.AXIS_Y),
                hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            )
            if (mapped != null) {
                mappedEventSequence += 1
                mappedActionEvent = mapped to mappedEventSequence
            }
        }
        val now = SystemClock.elapsedRealtime()
        val isContinuousMotion = event.actionMasked == MotionEvent.ACTION_MOVE ||
            event.actionMasked == MotionEvent.ACTION_HOVER_MOVE
        if (!isContinuousMotion || now - lastMotionUpdateAt >= 100L) {
            lastMotionUpdateAt = now
            val action = when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE -> "HOVER"
                MotionEvent.ACTION_MOVE -> "MOVE"
                MotionEvent.ACTION_SCROLL -> "SCROLL"
                MotionEvent.ACTION_BUTTON_PRESS -> "BUTTON_PRESS"
                MotionEvent.ACTION_BUTTON_RELEASE -> "BUTTON_RELEASE"
                else -> "ACTION_${event.actionMasked}"
            }
            val device = event.device?.name ?: "dispositivo desconhecido"
            val axes = if (isJoystick) {
                " • stickX=${"%.2f".format(event.getAxisValue(MotionEvent.AXIS_X))}" +
                    " stickY=${"%.2f".format(event.getAxisValue(MotionEvent.AXIS_Y))}" +
                    " hatX=${"%.1f".format(event.getAxisValue(MotionEvent.AXIS_HAT_X))}" +
                    " hatY=${"%.1f".format(event.getAxisValue(MotionEvent.AXIS_HAT_Y))}"
            } else ""
            lastInput = "$action • x=${"%.1f".format(event.x)} y=${"%.1f".format(event.y)} " + axes +
                "• scrollX=${event.getAxisValue(MotionEvent.AXIS_HSCROLL)} " +
                "scrollY=${event.getAxisValue(MotionEvent.AXIS_VSCROLL)} " +
                "• buttons=${event.buttonState} • device=$device " +
                "• source=0x${event.source.toString(16)}"
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { VrDisplayHome(lastInput = lastInput) } }
    }
}

@Composable
private fun VrDisplayHome(lastInput: String) {
    val localContext = LocalContext.current
    val activity = localContext as? MainActivity
    val context = localContext.applicationContext
    val inputMappings = activity?.inputMappings ?: InputMappingSettings()
    val pendingMappingAction = activity?.pendingMappingAction
    var inputMappingsLoaded by remember { mutableStateOf(false) }
    var cameraVisible by remember { mutableStateOf(false) }
    var immersiveMode by remember { mutableStateOf(false) }
    var sideBySide by remember { mutableStateOf(true) }
    var frontCamera by remember { mutableStateOf(false) }

    SideEffect {
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !immersiveMode)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (immersiveMode) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    var profileState by remember { mutableStateOf(CalibrationProfileState.default()) }
    var profilesLoaded by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showCreateProfile by remember { mutableStateOf(false) }
    var profileNameDraft by remember { mutableStateOf("") }
    val calibration = profileState.activeProfile.settings

    LaunchedEffect(context) {
        profileState = CalibrationSettingsStore.observe(context).first()
        profilesLoaded = true
        val savedMappings = InputMappingStore.observe(context).first()
        activity?.loadInputMappings(savedMappings)
        inputMappingsLoaded = true
    }
    LaunchedEffect(inputMappings, inputMappingsLoaded) {
        if (inputMappingsLoaded) {
            delay(350)
            InputMappingStore.save(context, inputMappings)
        }
    }
    LaunchedEffect(activity?.mappedActionEvent) {
        val action = activity?.mappedActionEvent?.first
        when (action) {
            VrInputAction.BACK -> {
                if (immersiveMode) {
                    cameraVisible = false
                    immersiveMode = false
                }
            }
            VrInputAction.CONFIRM -> sideBySide = !sideBySide
            VrInputAction.UP -> profileState = profileState.withActiveSettings(
                calibration.copy(vertical = (calibration.vertical + 0.03f).coerceIn(-1f, 1f))
            )
            VrInputAction.DOWN -> profileState = profileState.withActiveSettings(
                calibration.copy(vertical = (calibration.vertical - 0.03f).coerceIn(-1f, 1f))
            )
            VrInputAction.LEFT -> profileState = profileState.withActiveSettings(
                calibration.copy(horizontal = (calibration.horizontal - 0.03f).coerceIn(-1f, 1f))
            )
            VrInputAction.RIGHT -> profileState = profileState.withActiveSettings(
                calibration.copy(horizontal = (calibration.horizontal + 0.03f).coerceIn(-1f, 1f))
            )
            null -> Unit
        }
    }
    LaunchedEffect(profileState, profilesLoaded) {
        if (profilesLoaded) {
            delay(350)
            CalibrationSettingsStore.save(context, profileState)
        }
    }

    BackHandler(enabled = immersiveMode) {
        cameraVisible = false
        immersiveMode = false
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraVisible = granted
        immersiveMode = granted
    }

    if (cameraVisible && immersiveMode) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreviewPanel(
                sideBySide, frontCamera, calibration.ipd, calibration.zoom,
                calibration.horizontal, calibration.vertical, calibration.rotation,
                immersive = true
            ) {
                cameraVisible = false
                immersiveMode = false
            }
            TextButton(
                onClick = {
                    cameraVisible = false
                    immersiveMode = false
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) { Text("Sair do VR") }
        }
    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Vr-Display", style = MaterialTheme.typography.headlineMedium)
        Text("VR Box / Cardboard • modo experimental", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { cameraPermission.launch(android.Manifest.permission.CAMERA) }) { Text("Iniciar visualização VR") }
            OutlinedButton(onClick = { sideBySide = !sideBySide }) { Text(if (sideBySide) "Modo SBS" else "Tela única") }
            OutlinedButton(onClick = { frontCamera = !frontCamera }) { Text(if (frontCamera) "Frontal" else "Traseira") }
        }
        if (cameraVisible) {
            Spacer(Modifier.height(12.dp))
            CameraPreviewPanel(
                sideBySide, frontCamera, calibration.ipd, calibration.zoom,
                calibration.horizontal, calibration.vertical, calibration.rotation
            ) { cameraVisible = false }
        }
        Spacer(Modifier.height(18.dp))
        Text("Calibração óptica", style = MaterialTheme.typography.titleLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(onClick = { profileMenuExpanded = true }) {
                    Text("Perfil: " + profileState.activeProfile.name)
                }
                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false }
                ) {
                    profileState.profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name) },
                            onClick = {
                                profileState = profileState.selectProfile(profile.id)
                                profileMenuExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedButton(
                enabled = profileState.profiles.size < CalibrationProfileState.MAX_PROFILES,
                onClick = {
                    profileNameDraft = ""
                    showCreateProfile = true
                }
            ) { Text("Novo perfil") }
        }
        Text("Separação visual / IPD: " + (calibration.ipd * 100).toInt() + "%")
        Slider(
            value = calibration.ipd,
            onValueChange = { profileState = profileState.withActiveSettings(calibration.copy(ipd = it)) }
        )
        Text("Zoom: " + "%.2fx".format(calibration.zoom))
        Slider(
            value = calibration.zoom,
            onValueChange = { profileState = profileState.withActiveSettings(calibration.copy(zoom = it)) },
            valueRange = 0.7f..2.5f
        )
        Text("Deslocamento horizontal: " + "%.2f".format(calibration.horizontal))
        Slider(
            value = calibration.horizontal,
            onValueChange = { profileState = profileState.withActiveSettings(calibration.copy(horizontal = it)) },
            valueRange = -1f..1f
        )
        Text("Deslocamento vertical: " + "%.2f".format(calibration.vertical))
        Slider(
            value = calibration.vertical,
            onValueChange = { profileState = profileState.withActiveSettings(calibration.copy(vertical = it)) },
            valueRange = -1f..1f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { profileState = profileState.withActiveSettings(calibration.copy(rotation = (calibration.rotation + 90) % 360)) }
            ) { Text("Rotação: " + calibration.rotation + "°") }
            TextButton(onClick = { profileState = profileState.withActiveSettings(CalibrationSettings()) }) { Text("Redefinir perfil") }
        }
        Text(
            "Cada perfil mantém seus próprios ajustes e é salvo automaticamente neste aparelho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text("Mapeamento de controle", style = MaterialTheme.typography.titleLarge)
        Text(
            pendingMappingAction?.let { "Aguardando tecla para: ${it.label}. Pressione um botão no controle ou teclado." }
                ?: "Toque em uma ação e pressione o botão que deseja atribuir.",
            color = if (pendingMappingAction != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall
        )
        VrInputAction.entries.forEach { action ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(action.label, style = MaterialTheme.typography.bodyMedium)
                    Text(inputKeyLabel(inputMappings.keyFor(action)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
                OutlinedButton(onClick = { activity?.armInputMapping(action) }) {
                    Text(if (pendingMappingAction == action) "Aguardando..." else "Atribuir")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { activity?.restoreInputMappings() }) { Text("Restaurar padrão") }
            if (pendingMappingAction != null) {
                TextButton(onClick = { activity?.cancelInputMapping() }) { Text("Cancelar") }
            }
        }
        Text("Diagnóstico de entrada", style = MaterialTheme.typography.titleLarge)
        Text(lastInput, style = MaterialTheme.typography.bodyMedium)
        Text("Joystick: stick analógico e HAT acionam as direções com zona morta de 0,45. Mouse OTG permanece no diagnóstico.")
        Spacer(Modifier.height(16.dp))
        ArCoreStatusPanel()
        Spacer(Modifier.height(20.dp))
        Text(
            "Aviso: o mesmo frame é exibido nos dois olhos (mono duplicado). ARCore e painéis de outros apps ainda não estão implementados.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall
        )
        if (showCreateProfile) {
            val cleanName = profileNameDraft.trim()
            val canCreate = cleanName.isNotEmpty() &&
                profileState.profiles.none { it.name.equals(cleanName, ignoreCase = true) } &&
                profileState.profiles.size < CalibrationProfileState.MAX_PROFILES
            AlertDialog(
                onDismissRequest = { showCreateProfile = false },
                title = { Text("Novo perfil de headset") },
                text = {
                    OutlinedTextField(
                        value = profileNameDraft,
                        onValueChange = { profileNameDraft = it },
                        label = { Text("Nome do perfil") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = canCreate,
                        onClick = {
                            profileState = profileState.addProfile(cleanName)
                            showCreateProfile = false
                            profileNameDraft = ""
                        }
                    ) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProfile = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
