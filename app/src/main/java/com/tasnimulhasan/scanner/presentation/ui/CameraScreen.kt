package com.tasnimulhasan.scanner.presentation.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tasnimulhasan.scanner.ui.theme.AccentAmber
import com.tasnimulhasan.scanner.ui.theme.ScanGreen
import com.tasnimulhasan.scanner.util.CameraFileHelper
import java.util.concurrent.Executors

/**
 * CameraScreen — full-screen CameraX viewfinder.
 *
 * FLOW:
 *  1. Check CAMERA permission via Accompanist
 *  2. Bind CameraX Preview + ImageCapture use cases to lifecycle
 *  3. User taps shutter → ImageCapture writes JPEG to our FileProvider URI
 *  4. onPhotoCaptured(uri) is called → parent ViewModel kicks off OCR
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraFileHelper: CameraFileHelper,
    onPhotoCaptured: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Request permission as soon as the screen appears
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted) {
        // ── Permission denied UI ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Camera permission is required.\nPlease grant it in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }

    // ── CameraX setup ─────────────────────────────────────────────────────
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }

    // Animated scan line
    val infiniteTransition = rememberInfiniteTransition(label = "cam_scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_y",
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Live camera preview ───────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    bindCamera(ctx, lifecycleOwner, previewView, imageCapture)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Receipt framing overlay ───────────────────────────────────────
        ReceiptFrameOverlay(scanY = scanY)

        // ── Hint text ─────────────────────────────────────────────────────
        Text(
            text = "Align receipt within the frame",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )

        // ── Back button ───────────────────────────────────────────────────
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        // ── Shutter button ────────────────────────────────────────────────
        ShutterButton(
            isCapturing = isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    val outputUri = cameraFileHelper.createTempImageUri()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver.openOutputStream(outputUri)!!
                    ).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(outputUri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraScreen", "Capture failed", exception)
                                isCapturing = false
                            }
                        },
                    )
                }
            },
        )
    }
}

// ── CameraX bind helper ───────────────────────────────────────────────────────

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera bind failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ReceiptFrameOverlay(scanY: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Semi-dark border outside the frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Bright receipt frame
        Box(
            modifier = Modifier
                .size(width = 310.dp, height = 420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .border(
                    width = 2.dp,
                    color = AccentAmber,
                    shape = RoundedCornerShape(12.dp),
                )
        ) {
            // Animated scan line inside the frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (scanY * 400 - 200).dp)
                    .padding(horizontal = 8.dp)
                    .size(height = 2.dp, width = 294.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ScanGreen.copy(alpha = 0f),
                                ScanGreen.copy(alpha = 0.9f),
                                ScanGreen.copy(alpha = 0.9f),
                                ScanGreen.copy(alpha = 0f),
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isCapturing) Color.Gray else Color.White)
            .border(3.dp, AccentAmber, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, enabled = !isCapturing) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Capture",
                tint = if (isCapturing) Color.LightGray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}