package pe.profelab.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.files.DocumentFiles
import pe.profelab.core.model.newId
import pe.profelab.feature.pdf.PdfDocumentHandle
import java.io.File

/** Regression coverage for the native Android report exporter, not PDFBox's annotation exporter. */
@RunWith(AndroidJUnit4::class)
class ReportExportTest {
    @Test fun multiPageReportAndChosenImageReopenAsPdf()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val image=File(context.cacheDir,"report-test-${newId()}.png")
        var output:File?=null
        try {
            val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
            try {bitmap.eraseColor(Color.GREEN);image.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}}
            finally {bitmap.recycle()}
            val file=DocumentFiles(context).exportPdf("Sesión de prueba",(1..120).joinToString("\n"){"Observación $it"},listOf(image))
            output=file
            assertTrue(file.length()>0)
            PdfDocumentHandle(context,Uri.fromFile(file)).use {reader->
                assertTrue("Report pages plus selected image must be preserved",reader.pageCount>=4)
                val rendered=requireNotNull(reader.render(reader.pageCount-1,600))
                try {assertTrue(rendered.width>0 && rendered.height>0)}finally{rendered.recycle()}
            }
        }finally{image.delete();output?.delete()}
    }

    @Test fun unreadableImageDoesNotPublishPartialReport()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val invalid=File(context.cacheDir,"report-invalid-${newId()}.txt").apply {writeText("not an image")}
        val directory=File(context.cacheDir,"exports").apply {mkdirs()}
        val before=directory.listFiles().orEmpty().map {it.name}.toSet()
        try {
            var rejected=false
            try {DocumentFiles(context).exportPdf("Prueba", "Texto conservado",listOf(invalid))}
            catch(_:IllegalStateException){rejected=true}
            assertTrue("Unreadable attachment must fail explicitly",rejected)
            assertEquals("No incomplete report may remain",before,directory.listFiles().orEmpty().map {it.name}.toSet())
            val next=DocumentFiles(context).exportPdf("Reintento", "Informe sin adjuntos")
            try {PdfDocumentHandle(context,Uri.fromFile(next)).use {assertEquals(1,it.pageCount)}}finally{next.delete()}
        }finally{invalid.delete()}
    }
}
