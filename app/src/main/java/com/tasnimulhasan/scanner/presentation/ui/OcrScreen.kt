package com.tasnimulhasan.scanner.presentation.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tasnimulhasan.scanner.presentation.ui.components.ImagePickerButton
import com.tasnimulhasan.scanner.presentation.ui.components.ReceiptCard
import com.tasnimulhasan.scanner.presentation.ui.components.ScannerOverlay
import com.tasnimulhasan.scanner.presentation.viewmodel.OcrUiState
import com.tasnimulhasan.scanner.presentation.viewmodel.OcrViewModel
import com.tasnimulhasan.scanner.util.CameraFileHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    viewModel: OcrViewModel = hiltViewModel(),
    cameraFileHelper: CameraFileHelper,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ── Camera fullscreen overlay ─────────────────────────────────────────
    if (state.showCamera) {
        CameraScreen(
            cameraFileHelper = cameraFileHelper,
            onPhotoCaptured = viewModel::onPhotoCaptured,
            onBack = viewModel::onCloseCamera,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "Receipt Scanner",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            AnimatedContent(
                targetState = state.uiState,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 6 }) togetherWith fadeOut()
                },
                label = "ocr_state",
            ) { uiState ->
                when (uiState) {

                    // ── Idle ─────────────────────────────────────────────
                    is OcrUiState.Idle -> {
                        IdleContent(
                            onImageSelected = viewModel::onImageSelected,
                            onCameraClicked = viewModel::onOpenCamera,
                            onDemoClicked = viewModel::onLoadDemoReceipt,
                        )
                    }

                    // ── Loading ──────────────────────────────────────────
                    is OcrUiState.Loading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            state.selectedImageUri?.let { uri ->
                                if (!state.isDemoMode) {
                                    SelectedImagePreview(uri = uri)
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                            ScannerOverlay()
                        }
                    }

                    // ── Success ──────────────────────────────────────────
                    is OcrUiState.Success -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            state.selectedImageUri?.let { uri ->
                                if (!state.isDemoMode) {
                                    SelectedImagePreview(uri = uri)
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                            ReceiptCard(
                                receipt = uiState.receipt,
                                rawText = uiState.rawText,
                                showRawText = state.showRawText,
                                onToggleRawText = viewModel::onToggleRawText,
                                onReset = viewModel::onReset,
                            )
                        }
                    }

                    // ── Error ────────────────────────────────────────────
                    is OcrUiState.Error -> {
                        ErrorContent(
                            message = uiState.message,
                            onRetry = viewModel::onReset,
                        )
                    }
                }
            }
        }
    }
}

// ── Inner composables ─────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    onImageSelected: (Uri) -> Unit,
    onCameraClicked: () -> Unit,
    onDemoClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Scan a Receipt",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Use your camera, pick from gallery,\nor try the built-in demo receipt",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        ImagePickerButton(
            onImageSelected = onImageSelected,
            onCameraClicked = onCameraClicked,
            onDemoClicked = onDemoClicked,
        )

        Spacer(Modifier.height(32.dp))
        FeaturesList()
    }
}

@Composable
private fun SelectedImagePreview(uri: Uri) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Selected receipt image",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Scan failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
private fun FeaturesList() {
    val features = listOf(
        "✦  Live camera scan with receipt framing",
        "✦  Powered by Google ML Kit",
        "✦  Extracts items, totals & store info",
        "✦  Clean Architecture + DDD",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}