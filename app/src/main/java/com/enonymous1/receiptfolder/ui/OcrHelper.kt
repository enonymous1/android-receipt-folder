package com.enonymous1.receiptfolder.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Runs ML Kit text recognition on a saved photo and returns the raw text.
 * The recogniser runs on-device (no network, no account), and the photo
 * itself is never uploaded anywhere in v2.
 */
suspend fun recogniseReceiptText(context: Context, imageFile: File): String {
    val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
    return try {
        suspendCoroutine { continuation ->
            recogniser.process(image)
                .addOnSuccessListener { visionText -> continuation.resume(visionText.text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
    } finally {
        recogniser.close()
    }
}

/** Copies a picked image into the app's private receipts directory. */
fun copyPickedImage(context: Context, sourceUri: Uri): File? {
    val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
    val target = File(receiptsDir, "receipt-${System.currentTimeMillis()}.jpg")
    return try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }
        target
    } catch (_: Exception) {
        null
    }
}
