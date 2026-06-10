package com.tasnimulhasan.scanner.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates a content:// URI pointing to a temp file in cache/camera_photos/.
 *
 * WHY FileProvider?
 *   Android 7+ (API 24) blocks file:// URIs across app boundaries for security.
 *   FileProvider wraps a real file path into a safe content:// URI that the
 *   camera app can write to and ML Kit can later read from.
 *
 * HOW IT WORKS:
 *   1. We create an empty File in getCacheDir()/camera_photos/
 *   2. FileProvider converts its path into content://com.ocr.receipt.fileprovider/...
 *   3. We pass that URI to TakePicture contract — camera writes the JPEG there
 *   4. On success, we pass the same URI to ML Kit for OCR
 */
@Singleton
class CameraFileHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createTempImageUri(): Uri {
        val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val photoFile = File.createTempFile("receipt_", ".jpg", photoDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile,
        )
    }
}