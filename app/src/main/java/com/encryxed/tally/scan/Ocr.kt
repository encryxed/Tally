package com.encryxed.tally.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.encryxed.tally.parse.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/** One reading of the photo, at a particular rotation. */
data class OrientationRead(
    val rotationDegrees: Int,
    val lines: List<OcrLine>,
)

/**
 * Bridges ML Kit's on-device text recogniser to the plain [OcrLine] list that
 * [com.encryxed.tally.parse.ReceiptParser] understands.
 *
 * The recogniser is the bundled Latin model: it ships inside the APK and runs
 * entirely on the phone, which is why the app needs no network access.
 */
object Ocr {

    /** Long edge the photo is scaled to before recognition. */
    private const val MAX_DIMENSION = 2200

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Reads the photo at all four right-angle rotations.
     *
     * The parser's whole model of a receipt is spatial — the shop name sits at
     * the top, the total near the bottom, an amount shares a row with its
     * label. A photo taken with the receipt lying sideways breaks every one of
     * those assumptions at once, and the result is not a slightly worse read
     * but an arbitrary one.
     *
     * Rather than trying to infer the angle from the glyphs, we simply read it
     * four ways and let the caller keep whichever parse comes out best. OCR is
     * fast enough on-device that this costs well under a second, and it is far
     * more robust than guessing.
     */
    suspend fun readAllOrientations(context: Context, imageUri: Uri): List<OrientationRead> {
        val source = decodeScaled(context, imageUri) ?: return emptyList()

        return try {
            listOf(0, 90, 180, 270).mapNotNull { degrees ->
                val rotated = rotate(source, degrees)
                try {
                    val lines = recognize(rotated)
                    if (lines.isEmpty()) null else OrientationRead(degrees, lines)
                } finally {
                    if (rotated !== source) rotated.recycle()
                }
            }
        } finally {
            source.recycle()
        }
    }

    private suspend fun recognize(bitmap: Bitmap): List<OcrLine> {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
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
            // A receipt is a single column, so top-to-bottom is reading order.
            .sortedBy { it.top }
    }

    /**
     * Decodes the photo at a workable size. Full-resolution camera output is
     * tens of megabytes as a bitmap, and we hold a rotated copy alongside it.
     */
    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun rotate(source: Bitmap, degrees: Int): Bitmap =
        if (degrees == 0) {
            source
        } else {
            Bitmap.createBitmap(
                source, 0, 0, source.width, source.height,
                Matrix().apply { postRotate(degrees.toFloat()) },
                true,
            )
        }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }
