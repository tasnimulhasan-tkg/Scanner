package com.tasnimulhasan.scanner.domain.repository

import android.net.Uri
import com.tasnimulhasan.scanner.domain.model.OcrResult

/**
 * Domain repository interface — part of the domain layer.
 * Implementation lives in the data layer (Dependency Inversion).
 */
interface OcrRepository {
    /**
     * Processes an image URI and returns the raw OCR result.
     */
    suspend fun extractText(imageUri: Uri): Result<OcrResult>
}
