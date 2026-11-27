package com.baltdh.vrdisplay

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        val device = event.device?.name ?: "dispositivo desconhecido"
        lastInput = "$action • ${KeyEvent.keyCodeToString(event.keyCode)} • ${event.displayLabel} " +
            "• scan=${event.scanCode} • repeat=${event.repeatCount} " +
            "• device=$device • source=0x${event.source.toString(16)}"
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
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
            lastInput = "$action • x=${"%.1f".format(event.x)} y=${"%.1f".format(event.y)} " +
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
    val activity = localContext as? Activity
    val context = localContext.applicationContext
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
    var calibration by remember { mutableStateOf(CalibrationSettings()) }
    var calibrationLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        calibration = CalibrationSettingsStore.observe(context).first()
        calibrationLoaded = true
    }
    LaunchedEffect(calibration, calibrationLoaded) {
        if (calibrationLoaded) {
            delay(350)
            CalibrationSettingsStore.save(context, calibration)
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
        Text("Separação visual / IPD: " + (calibration.ipd * 100).toInt() + "%")
        Slider(
            value = calibration.ipd,
            onValueChange = { calibration = calibration.copy(ipd = it) }
        )
        Text("Zoom: " + "%.2fx".format(calibration.zoom))
        Slider(
            value = calibration.zoom,
            onValueChange = { calibration = calibration.copy(zoom = it) },
            valueRange = 0.7f..2.5f
        )
        Text("Deslocamento horizontal: " + "%.2f".format(calibration.horizontal))
        Slider(
            value = calibration.horizontal,
            onValueChange = { calibration = calibration.copy(horizontal = it) },
            valueRange = -1f..1f
        )
        Text("Deslocamento vertical: " + "%.2f".format(calibration.vertical))
        Slider(
            value = calibration.vertical,
            onValueChange = { calibration = calibration.copy(vertical = it) },
            valueRange = -1f..1f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { calibration = calibration.copy(rotation = (calibration.rotation + 90) % 360) }
            ) { Text("Rotação: " + calibration.rotation + "°") }
            TextButton(onClick = { calibration = CalibrationSettings() }) { Text("Redefinir") }
        }
        Text(
            "Os ajustes são salvos automaticamente neste aparelho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text("Diagnóstico de entrada", style = MaterialTheme.typography.titleLarge)
        Text(lastInput, style = MaterialTheme.typography.bodyMedium)
        Text("Pressione botões do controle Bluetooth, teclado ou mova o mouse OTG para identificar eventos e origem.")
        Spacer(Modifier.height(16.dp))
        ArCoreStatusPanel()
        Spacer(Modifier.height(20.dp))
        Text(
            "Aviso: o mesmo frame é exibido nos dois olhos (mono duplicado). ARCore e painéis de outros apps ainda não estão implementados.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
