package com.baltdh.vrdisplay

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Composable
fun CameraPreviewPanel(
    sideBySide: Boolean,
    frontCamera: Boolean,
    ipd: Float,
    zoom: Float,
    horizontal: Float,
    vertical: Float,
    rotation: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val executor = remember(frontCamera) { Executors.newSingleThreadExecutor() }
    val lastFrameAt = remember { AtomicLong(0L) }

    DisposableEffect(lifecycleOwner, frontCamera) {
        val future = ProcessCameraProvider.getInstance(context)
        var activeProvider: ProcessCameraProvider? = null
        var activeAnalysis: ImageAnalysis? = null
        var disposed = false
        val listener = Runnable {
            if (disposed) return@Runnable
            try {
                val provider = future.get()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastFrameAt.get() >= 66L) {
                            lastFrameAt.set(now)
                            val source = image.toBitmap()
                            val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                            val corrected = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                            ContextCompat.getMainExecutor(context).execute { frame = corrected }
                            if (source !== corrected) source.recycle()
                        }
                    } catch (t: Throwable) {
                        ContextCompat.getMainExecutor(context).execute { error = t.message ?: "Falha ao processar imagem" }
                    } finally {
                        image.close()
                    }
                }
                val selector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbind(analysis)
                provider.bindToLifecycle(lifecycleOwner, selector, analysis)
                activeProvider = provider
                activeAnalysis = analysis
            } catch (t: Throwable) {
                error = t.message ?: "Não foi possível iniciar a câmera"
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            try { activeAnalysis?.let { activeProvider?.unbind(it) } } catch (_: Throwable) { }
            executor.shutdown()
            frame = null
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Passthrough CameraX", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) { Text("Fechar câmera") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val bitmap = frame?.asImageBitmap() ?: return@Canvas
            val eyes = if (sideBySide) 2 else 1
            val eyeWidth = size.width / eyes
            for (eye in 0 until eyes) {
                val centerX = eye * eyeWidth + eyeWidth / 2f
                val centerY = size.height / 2f
                val gap = if (sideBySide) ipd * eyeWidth * .12f else 0f
                withTransform({
                    translate(left = centerX - eyeWidth / 2f + if (eye == 0) gap else -gap, top = 0f)
                    scale(zoom, zoom, pivot = Offset(eyeWidth / 2f, centerY))
                    rotate(rotation.toFloat(), pivot = Offset(eyeWidth / 2f, centerY))
                    translate(left = horizontal * eyeWidth * .15f, top = vertical * size.height * .15f)
                }) {
                    drawImage(bitmap, dstSize = androidx.compose.ui.unit.IntSize(eyeWidth.toInt(), size.height.toInt()))
                }
            }
        }
    }
}
