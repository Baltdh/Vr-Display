package com.baltdh.vrdisplay

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private var lastKey by mutableStateOf("Nenhum evento recebido")

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        lastKey = "DOWN: keyCode=" + keyCode + " | " + (event?.displayLabel ?: "?") + " | scan=" + (event?.scanCode ?: 0)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        lastKey = "UP: keyCode=" + keyCode + " | " + (event?.displayLabel ?: "?") + " | scan=" + (event?.scanCode ?: 0)
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { VrDisplayHome(lastKey = lastKey) } }
    }
}

@Composable
private fun VrDisplayHome(lastKey: String) {
    val context = LocalContext.current.applicationContext
    var cameraVisible by remember { mutableStateOf(false) }
    var sideBySide by remember { mutableStateOf(true) }
    var frontCamera by remember { mutableStateOf(false) }
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

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraVisible = granted }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Vr-Display", style = MaterialTheme.typography.headlineMedium)
        Text("VR Box / Cardboard • modo experimental", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { cameraPermission.launch(android.Manifest.permission.CAMERA) }) { Text("Abrir câmera VR") }
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
        Text("Diagnóstico do controle", style = MaterialTheme.typography.titleLarge)
        Text(lastKey, style = MaterialTheme.typography.bodyMedium)
        Text("Pressione botões no controle Bluetooth ou teclado para visualizar eventos.")
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
