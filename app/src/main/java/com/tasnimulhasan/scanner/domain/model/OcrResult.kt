package com.tasnimulhasan.scanner.domain.model

/**
 * Value Object — raw OCR text result before parsing.
 */
data class OcrResult(
    val rawText: String,
    val blocks: List<TextBlock>,
    val confidence: Float,
)

/**
 * Value Object — a detected text block with bounding information.
 */
data class TextBlock(
    val text: String,
    val lines: List<String>,
)
