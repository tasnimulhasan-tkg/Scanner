package com.tasnimulhasan.scanner.data.source

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.model.TextBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Data Source: Wraps Google ML Kit Text Recognition API.
 * Converts ML Kit callbacks into coroutine-friendly suspend functions.
 */
@Singleton
class MlKitOcrDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageUri: Uri): Result<OcrResult> =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val blocks = visionText.textBlocks.map { block ->
                            TextBlock(
                                text = block.text,
                                lines = block.lines.map { it.text },
                            )
                        }
                        val result = OcrResult(
                            rawText = visionText.text,
                            blocks = blocks,
                            confidence = 1.0f,
                        )
                        continuation.resume(Result.success(result))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
}