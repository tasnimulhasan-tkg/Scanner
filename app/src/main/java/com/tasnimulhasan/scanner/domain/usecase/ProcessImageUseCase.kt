package com.tasnimulhasan.scanner.domain.usecase

import android.net.Uri
import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.repository.OcrRepository
import javax.inject.Inject

/**
 * Use Case: Orchestrates image-to-text extraction.
 * Domain logic lives here — no Android framework dependencies except Uri.
 */
class ProcessImageUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
) {
    suspend operator fun invoke(imageUri: Uri): Result<OcrResult> {
        if (imageUri == Uri.EMPTY) {
            return Result.failure(IllegalArgumentException("Image URI cannot be empty"))
        }
        return ocrRepository.extractText(imageUri)
    }
}
