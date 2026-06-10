package com.tasnimulhasan.scanner.data.source

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tasnimulhasan.scanner.domain.model.OcrLine
import com.tasnimulhasan.scanner.domain.model.OcrResult
import com.tasnimulhasan.scanner.domain.model.TextBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Data Source — wraps ML Kit and preserves ALL spatial information.
 *
 * KEY FIX: previously we only stored block. Text and lost bounding-box data.
 * Now every block and every line gets its normalised bounding rect stored,
 * so the parser can reconstruct two-column receipt layouts.
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
                val imgW = image.width.toFloat().coerceAtLeast(1f)
                val imgH = image.height.toFloat().coerceAtLeast(1f)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val blocks = visionText.textBlocks.map { block ->
                            val bBox = block.boundingBox
                            TextBlock(
                                text = block.text,
                                lines = block.lines.map { line ->
                                    val lBox = line.boundingBox
                                    OcrLine(
                                        text = line.text,
                                        left = (lBox?.left ?: 0) / imgW,
                                        top = (lBox?.top ?: 0) / imgH,
                                        right = (lBox?.right ?: 0) / imgW,
                                        bottom = (lBox?.bottom ?: 0) / imgH,
                                    )
                                },
                                left   = (bBox?.left   ?: 0) / imgW,
                                top    = (bBox?.top    ?: 0) / imgH,
                                right  = (bBox?.right  ?: 0) / imgW,
                                bottom = (bBox?.bottom ?: 0) / imgH,
                            )
                        }

                        // Sort blocks top-to-bottom, then left-to-right
                        // so rawText reading order matches visual order
                        val sortedBlocks = blocks.sortedWith(
                            compareBy({ it.top }, { it.left })
                        )

                        continuation.resume(
                            Result.success(
                                OcrResult(
                                    rawText = sortedBlocks.joinToString("\n") { b ->
                                        b.lines.joinToString("\n") { it.text }
                                    },
                                    blocks = sortedBlocks,
                                    confidence = 1.0f,
                                )
                            )
                        )
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
}