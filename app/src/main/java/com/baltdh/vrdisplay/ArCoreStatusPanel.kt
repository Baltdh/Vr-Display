package com.baltdh.vrdisplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ar.core.ArCoreApk

@Composable
fun ArCoreStatusPanel() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Verificando disponibilidade do ARCore…") }
    var available by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ArCoreApk.getInstance().checkAvailabilityAsync(context.applicationContext) { result ->
            available = result.isSupported
            status = when {
                result.isSupported -> "ARCore disponível neste dispositivo."
                result.isTransient -> "Verificação temporariamente indisponível. Tente novamente."
                else -> "ARCore não é compatível ou não está disponível neste dispositivo."
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Preparação ARCore", style = MaterialTheme.typography.titleLarge)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Button(enabled = available, onClick = {
            try {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    val result = ArCoreApk.getInstance().requestInstall(activity, true)
                    status = "Resultado da solicitação: " + result
                } else status = "Não foi possível obter a Activity."
            } catch (t: Throwable) {
                status = t.message ?: "Falha ao solicitar instalação do ARCore."
            }
        }) { Text("Solicitar instalação / atualização") }
        Text(
            "Este painel apenas verifica e solicita a instalação. Sessão AR, tracking e Depth ainda não foram implementados.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
