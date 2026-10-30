// Adapted from DocReader MIT, commit 092d7fd48eae18d2409d8c97149620218db99657. See licenses/DocReader-MIT.txt.
package pe.profelab.feature.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.Closeable
import java.io.File

/**
 * Thin wrapper around Android's built-in PdfRenderer (API 21+), so page rendering needs no
 * third-party library at all. When [password] is given the document is first decrypted by
 * PdfBox into a private cache file, because PdfRenderer cannot open encrypted files.
 *
 * Keep one instance open per document and call [close] when leaving the reader.
 */
class PdfDocumentHandle(context: Context, uri: Uri, password: String? = null) : Closeable {

    private var tempFile: File? = null
    private val descriptor: ParcelFileDescriptor
    private val renderer: PdfRenderer

    init {
        var tmp: File? = null
        val desc: ParcelFileDescriptor
        if (password.isNullOrEmpty()) {
            desc = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("Could not open the document")
        } else {
            val decrypted = File.createTempFile("docreader_dec_", ".pdf", context.cacheDir)
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Could not read the document")
                input.use { stream ->
                    PDDocument.load(stream, password).use { doc -> doc.setAllSecurityToBeRemoved(true); doc.save(decrypted) }
                }
                tmp = decrypted
                desc = ParcelFileDescriptor.open(decrypted, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (failure: Exception) {
                decrypted.delete()
                throw failure
            }
        }
        tempFile = tmp
        descriptor = desc
        renderer = try { PdfRenderer(desc) } catch (failure: Exception) {
            desc.close();tmp?.delete();throw failure
        }
    }

    val pageCount: Int get() = renderer.pageCount

    /** Page size in points (72 dpi), used by the print adapter. */
    fun pageSize(index: Int): Pair<Int, Int>? = runCatching {
        renderer.openPage(index).use { it.width to it.height }
    }.getOrNull()

    /**
     * Height / width ratio of [index]'s page at its natural (post-rotation) orientation.
     * Used by the reader to keep the pinch focal point fixed while zooming.
     */
    fun pageAspect(index: Int): Float = runCatching {
        renderer.openPage(index).use { it.height.toFloat() / it.width.toFloat() }
    }.getOrDefault(1.4142f)

    /** Renders [index] scaled to [targetWidth] px wide. Returns null if rendering fails. */
    @Synchronized
    fun render(index: Int, targetWidth: Int): Bitmap? {
        if (index < 0 || index >= pageCount) return null
        return runCatching {
            renderer.openPage(index).use { page ->
                val scale = minOf(targetWidth.coerceIn(200,1600).toFloat()/page.width,2400f/page.height)
                val width = (page.width*scale).toInt().coerceAtLeast(1)
                val height = (page.height*scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }.getOrNull()
    }

    @Synchronized
    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
        runCatching { tempFile?.delete() }
        tempFile = null
    }
}
