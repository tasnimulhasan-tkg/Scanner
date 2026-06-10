package com.tasnimulhasan.scanner.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.scanner.domain.model.Receipt
import com.tasnimulhasan.scanner.domain.model.ReceiptItem
import com.tasnimulhasan.scanner.domain.usecase.ParseReceiptUseCase
import com.tasnimulhasan.scanner.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface OcrUiState {
    data object Idle : OcrUiState
    data object Loading : OcrUiState
    data class Success(val receipt: Receipt, val rawText: String) : OcrUiState
    data class Error(val message: String) : OcrUiState
}

data class OcrScreenState(
    val selectedImageUri: Uri? = null,
    val uiState: OcrUiState = OcrUiState.Idle,
    val showRawText: Boolean = false,
    val isDemoMode: Boolean = false,
    val showCamera: Boolean = false,  // ← NEW: drives CameraScreen visibility
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase,
    private val parseReceiptUseCase: ParseReceiptUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OcrScreenState())
    val state: StateFlow<OcrScreenState> = _state.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _state.update { it.copy(selectedImageUri = uri, isDemoMode = false) }
        processImage(uri)
    }

    /** Opens the full-screen CameraX viewfinder */
    fun onOpenCamera() {
        _state.update { it.copy(showCamera = true) }
    }

    /** Called when the user backs out of the camera without taking a photo */
    fun onCloseCamera() {
        _state.update { it.copy(showCamera = false) }
    }

    /** Called by CameraScreen after a photo is successfully saved */
    fun onPhotoCaptured(uri: Uri) {
        _state.update { it.copy(showCamera = false, selectedImageUri = uri, isDemoMode = false) }
        processImage(uri)
    }

    fun onLoadDemoReceipt() {
        _state.update { it.copy(isDemoMode = true, uiState = OcrUiState.Loading) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200) // Simulate processing
            val demoReceipt = buildDemoReceipt()
            _state.update {
                it.copy(
                    uiState = OcrUiState.Success(
                        receipt = demoReceipt,
                        rawText = DEMO_RAW_TEXT,
                    )
                )
            }
        }
    }

    fun onToggleRawText() {
        _state.update { it.copy(showRawText = !it.showRawText) }
    }

    fun onReset() {
        _state.update { OcrScreenState() }
    }

    private fun processImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(uiState = OcrUiState.Loading) }
            processImageUseCase(uri)
                .onSuccess { ocrResult ->
                    val receipt = parseReceiptUseCase(ocrResult)
                    _state.update {
                        it.copy(
                            uiState = OcrUiState.Success(
                                receipt = receipt,
                                rawText = ocrResult.rawText,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            uiState = OcrUiState.Error(
                                message = error.message ?: "Failed to process image"
                            )
                        )
                    }
                }
        }
    }

    // ── Demo Data ─────────────────────────────────────────────────────────────

    private fun buildDemoReceipt() = Receipt(
        id = "DEMO-001",
        storeName = "FreshMart Superstore",
        storeAddress = "142 Gulshan Avenue, Dhaka 1212",
        date = "09/06/2026",
        items = listOf(
            ReceiptItem("Organic Whole Milk 1L", "2", "$2.49", "$4.98"),
            ReceiptItem("Sourdough Bread Loaf", "1", "$3.75", "$3.75"),
            ReceiptItem("Free-Range Eggs 12pk", "1", "$5.99", "$5.99"),
            ReceiptItem("Greek Yogurt 500g", "2", "$3.25", "$6.50"),
            ReceiptItem("Chicken Breast 1kg", "1", "$8.99", "$8.99"),
            ReceiptItem("Broccoli Florets 500g", "1", "$2.49", "$2.49"),
            ReceiptItem("Sparkling Water 6pk", "1", "$4.50", "$4.50"),
            ReceiptItem("Dark Chocolate 85%", "3", "$2.99", "$8.97"),
        ),
        subtotal = "$46.17",
        tax = "$2.31",
        total = "$48.48",
        paymentMethod = "Visa",
    )

    companion object {
        private val DEMO_RAW_TEXT = """
            FreshMart Superstore
            142 Gulshan Avenue, Dhaka 1212
            Tel: +880-2-8812345
            Date: 09/06/2026  Time: 14:32
            Receipt #: 00847291
            --------------------------------
            Organic Whole Milk 1L   x2  $4.98
            Sourdough Bread Loaf        $3.75
            Free-Range Eggs 12pk        $5.99
            Greek Yogurt 500g       x2  $6.50
            Chicken Breast 1kg          $8.99
            Broccoli Florets 500g       $2.49
            Sparkling Water 6pk         $4.50
            Dark Chocolate 85%      x3  $8.97
            --------------------------------
            Subtotal                   $46.17
            Tax (5%)                    $2.31
            TOTAL                      $48.48
            --------------------------------
            Payment: VISA **** 4291
            Thank you for shopping with us!
        """.trimIndent()
    }
}