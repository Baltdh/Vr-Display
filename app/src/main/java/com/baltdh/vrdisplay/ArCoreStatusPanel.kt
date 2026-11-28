package com.baltdh.vrdisplay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.ArCoreApk

@Composable
fun ArCoreStatusPanel() {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var status by remember { mutableStateOf("Verificando disponibilidade do ARCore…") }
    var available by remember { mutableStateOf(false) }
    var installPending by remember { mutableStateOf(false) }
    var availabilityCheck by remember { mutableIntStateOf(0) }

    DisposableEffect(applicationContext, availabilityCheck) {
        var active = true
        available = false
        status = "Verificando disponibilidade do ARCore…"

        try {
            ArCoreApk.getInstance().checkAvailabilityAsync(applicationContext) { result ->
                if (active) {
                    available = result.isSupported
                    status = when {
                        result.isSupported ->
                            "Dispositivo compatível. Verifique ou instale o Google Play Services para RA."
                        result.isTransient ->
                            "Verificação temporariamente indisponível. Tente novamente."
                        else ->
                            "ARCore não é compatível ou não está disponível neste dispositivo."
                    }
                }
            }
        } catch (exception: Exception) {
            if (active) {
                status = exception.localizedMessage
                    ?: "Não foi possível verificar a disponibilidade do ARCore."
            }
        }

        onDispose {
            active = false
        }
    }

    val resumeInstallCheck = rememberUpdatedState(
        newValue = {
            if (installPending) {
                val hostActivity = activity
                if (hostActivity == null) {
                    installPending = false
                    status = "Não foi possível recuperar a Activity para confirmar a instalação."
                } else {
                    try {
                        when (ArCoreApk.getInstance().requestInstall(hostActivity, false)) {
                            ArCoreApk.InstallStatus.INSTALLED -> {
                                installPending = false
                                available = true
                                status = "ARCore instalado e atualizado. A Session ainda não foi iniciada."
                            }
                            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                                status = "A instalação ou atualização do ARCore ainda está pendente."
                            }
                        }
                    } catch (exception: Exception) {
                        installPending = false
                        status = exception.localizedMessage
                            ?: "Não foi possível confirmar a instalação do ARCore."
                    }
                }
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeInstallCheck.value.invoke()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Preparação ARCore", style = MaterialTheme.typography.titleLarge)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Button(
            enabled = available && !installPending && activity != null,
            onClick = {
                val hostActivity = activity
                if (hostActivity == null) {
                    status = "Não foi possível obter a Activity."
                } else {
                    try {
                        when (ArCoreApk.getInstance().requestInstall(hostActivity, true)) {
                            ArCoreApk.InstallStatus.INSTALLED -> {
                                status = "ARCore instalado e atualizado. A Session ainda não foi iniciada."
                            }
                            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                                installPending = true
                                status = "Solicitação aberta. Ao retornar ao app, a instalação será confirmada automaticamente."
                            }
                        }
                    } catch (exception: Exception) {
                        installPending = false
                        status = exception.localizedMessage
                            ?: "Falha ao solicitar instalação do ARCore."
                    }
                }
            }
        ) {
            Text(if (installPending) "Aguardando confirmação…" else "Solicitar instalação / atualização")
        }
        TextButton(onClick = { availabilityCheck += 1 }) {
            Text("Verificar disponibilidade novamente")
        }
        Text(
            "Este painel verifica a compatibilidade e confirma a instalação no retorno ao app. Ainda não cria uma Session ARCore, não ativa tracking, renderização 3D nem Depth API.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> {
        val base = baseContext
        if (base === this) null else base.findActivity()
    }
    else -> null
}
