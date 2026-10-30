package pe.profelab.feature.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bundled model. Recognition never commits text to another feature without the review UI. */
class LatinOcrEngine:java.io.Closeable {
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    suspend fun recognize(bitmap:Bitmap):String=suspendCancellableCoroutine {c->
        recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener {if(c.isActive)c.resume(it.text)}.addOnFailureListener {if(c.isActive)c.resumeWithException(it)}
    }
    override fun close(){recognizer.close()}
}
