package com.baltdh.vrdisplay

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Composable
fun CameraPreviewPanel(
    sideBySide: Boolean,
    frontCamera: Boolean,
    ipd: Float,
    zoom: Float,
    horizontal: Float,
    vertical: Float,
    rotation: Int,
    immersive: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var metrics by remember(frontCamera) { mutableStateOf<CameraPerformanceSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val executor = remember(frontCamera) { Executors.newSingleThreadExecutor() }
    val lastFrameAt = remember(frontCamera) { AtomicLong(0L) }

    DisposableEffect(lifecycleOwner, frontCamera, executor) {
        val future = ProcessCameraProvider.getInstance(context)
        val disposed = AtomicBoolean(false)
        val performanceMeter = CameraPerformanceMeter()
        var activeProvider: ProcessCameraProvider? = null
        var activeAnalysis: ImageAnalysis? = null
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val pendingFrame = AtomicReference<Bitmap?>(null)
        val frameUpdateScheduled = AtomicBoolean(false)

        fun schedulePendingFrameUpdate() {
            if (disposed.get()) {
                pendingFrame.getAndSet(null)?.recycle()
                return
            }
            if (frameUpdateScheduled.compareAndSet(false, true)) {
                mainExecutor.execute {
                    try {
                        val nextFrame = pendingFrame.getAndSet(null)
                        if (nextFrame != null) {
                            if (disposed.get()) nextFrame.recycle() else frame = nextFrame
                        }
                    } finally {
                        frameUpdateScheduled.set(false)
                        if (disposed.get()) pendingFrame.getAndSet(null)?.recycle()
                        else if (pendingFrame.get() != null) schedulePendingFrameUpdate()
                    }
                }
            }
        }

        error = null
        frame = null
        metrics = null
        val listener = Runnable {
            if (disposed.get()) return@Runnable
            var analysis: ImageAnalysis? = null
            try {
                val provider = future.get()
                if (disposed.get()) return@Runnable

                analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { image ->
                    try {
                        val now = SystemClock.elapsedRealtime()
                        if (!disposed.get() && now - lastFrameAt.get() >= 66L) {
                            lastFrameAt.set(now)
                            val processingStartedAt = SystemClock.elapsedRealtime()
                            val source = image.toBitmap()
                            val corrected = try {
                                val matrix = Matrix().apply {
                                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                                }
                                Bitmap.createBitmap(
                                    source, 0, 0, source.width, source.height, matrix, true
                                )
                            } catch (t: Throwable) {
                                source.recycle()
                                throw t
                            }
                            if (source !== corrected) source.recycle()

                            val processedAt = SystemClock.elapsedRealtime()
                            val snapshot = performanceMeter.recordFrame(
                                timestampMs = processedAt,
                                processingMs = (processedAt - processingStartedAt).toFloat(),
                                width = corrected.width,
                                height = corrected.height
                            )
                            if (disposed.get()) {
                                corrected.recycle()
                            } else {
                                pendingFrame.getAndSet(corrected)?.recycle()
                                if (snapshot != null) {
                                    mainExecutor.execute {
                                        if (!disposed.get()) metrics = snapshot
                                    }
                                }
                                schedulePendingFrameUpdate()
                            }
                        }
                    } catch (t: Throwable) {
                        mainExecutor.execute {
                            if (!disposed.get()) {
                                error = t.message ?: "Falha ao processar imagem da câmera."
                            }
                        }
                    } finally {
                        image.close()
                    }
                }

                val selector = if (frontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider.bindToLifecycle(lifecycleOwner, selector, analysis)
                activeProvider = provider
                activeAnalysis = analysis
            } catch (t: Throwable) {
                analysis?.clearAnalyzer()
                if (!disposed.get()) {
                    error = t.message ?: "Não foi possível iniciar a câmera."
                }
            }
        }

        future.addListener(listener, mainExecutor)

        onDispose {
            disposed.set(true)
            activeAnalysis?.let { useCase ->
                try {
                    useCase.clearAnalyzer()
                    activeProvider?.unbind(useCase)
                } catch (_: Throwable) {
                    // A câmera pode já ter sido desvinculada pelo lifecycle.
                }
            }
            executor.shutdown()
            pendingFrame.getAndSet(null)?.recycle()
            frame = null
            metrics = null
        }
    }

    Column(if (immersive) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
        if (!immersive) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Passthrough CameraX", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClose) { Text("Fechar câmera") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Box(if (immersive) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(220.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val bitmap = frame?.asImageBitmap() ?: return@Canvas
                val eyes = if (sideBySide) 2 else 1
                val eyeWidth = size.width / eyes
                if (eyeWidth <= 0f || size.height <= 0f) return@Canvas
                val targetAspect = eyeWidth / size.height
                val sourceAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val sourceOffset: IntOffset
                val sourceSize: IntSize
                if (sourceAspect > targetAspect) {
                    val cropWidth = (bitmap.height * targetAspect).toInt().coerceIn(1, bitmap.width)
                    sourceOffset = IntOffset((bitmap.width - cropWidth) / 2, 0)
                    sourceSize = IntSize(cropWidth, bitmap.height)
                } else {
                    val cropHeight = (bitmap.width / targetAspect).toInt().coerceIn(1, bitmap.height)
                    sourceOffset = IntOffset(0, (bitmap.height - cropHeight) / 2)
                    sourceSize = IntSize(bitmap.width, cropHeight)
                }
                for (eye in 0 until eyes) {
                    val centerX = eye * eyeWidth + eyeWidth / 2f
                    val centerY = size.height / 2f
                    val gap = if (sideBySide) ipd * eyeWidth * .12f else 0f
                    withTransform({
                        translate(
                            left = centerX - eyeWidth / 2f + if (eye == 0) gap else -gap,
                            top = 0f
                        )
                        scale(zoom, zoom, pivot = Offset(eyeWidth / 2f, centerY))
                        rotate(rotation.toFloat(), pivot = Offset(eyeWidth / 2f, centerY))
                        translate(
                            left = horizontal * eyeWidth * .15f,
                            top = vertical * size.height * .15f
                        )
                    }) {
                        drawImage(
                            image = bitmap,
                            srcOffset = sourceOffset,
                            srcSize = sourceSize,
                            dstSize = IntSize(eyeWidth.toInt(), size.height.toInt())
                        )
                    }
                }
            }
            metrics?.let { current ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "%.1f FPS • %dx%d • %.1f ms".format(
                            current.framesPerSecond,
                            current.width,
                            current.height,
                            current.averageProcessingMs
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
