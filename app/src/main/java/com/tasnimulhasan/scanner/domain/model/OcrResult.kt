package com.tasnimulhasan.scanner.domain.model

/**
 * Value Object — raw OCR output with full spatial structure preserved.
 *
 * WHY WE KEEP boundingBox:
 *   Many receipts print item names and prices in two separate columns.
 *   ML Kit detects them as separate text blocks with different X positions.
 *   Without bounding-box data we cannot know which price belongs to which item.
 *   We expose left/right/top/bottom (normalised 0–1 relative to image size)
 *   so ParseReceiptUseCase can reconstruct logical rows by aligning Y positions.
 */
data class OcrResult(
    val rawText: String,
    val blocks: List<TextBlock>,
    val confidence: Float,
)

data class TextBlock(
    val text: String,
    val lines: List<OcrLine>,
    /** Normalised bounding box (0.0–1.0) relative to image dimensions */
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)