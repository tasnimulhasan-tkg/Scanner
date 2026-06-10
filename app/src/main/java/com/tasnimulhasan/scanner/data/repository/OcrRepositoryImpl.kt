package com.tasnimulhasan.scanner.data.repository

import android.net.Uri
import com.tasnimulhasan.scanner.data.source.MlKitOcrDataSource
import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.repository.OcrRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Layer: Implements the OcrRepository domain interface.
 * Delegates to the ML Kit data source and maps results.
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val mlKitOcrDataSource: MlKitOcrDataSource,
) : OcrRepository {

    override suspend fun extractText(imageUri: Uri): Result<OcrResult> {
        return mlKitOcrDataSource.recognizeText(imageUri)
    }
}