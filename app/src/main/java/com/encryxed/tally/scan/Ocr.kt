package com.encryxed.tally.scan

import android.content.Context
import android.net.Uri
import com.encryxed.tally.parse.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges ML Kit's on-device text recogniser to the plain [OcrLine] list that
 * [com.encryxed.tally.parse.ReceiptParser] understands.
 *
 * The recogniser here is the bundled Latin model: it ships inside the APK and
 * runs entirely on the phone, which is why the app needs no network access.
 */
object Ocr {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun readLines(context: Context, imageUri: Uri): List<OcrLine> {
        val image = InputImage.fromFilePath(context, imageUri)
        val text = recognizer.process(image).await()

        return text.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLine(
                    text = line.text,
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom,
                )
            }
            // ML Kit returns blocks in reading order, but a receipt is a single
            // column — sorting top-to-bottom keeps line offsets meaningful.
            .sortedBy { it.top }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }
