package pe.profelab.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import pe.profelab.feature.pdf.PdfEngine
import pe.profelab.feature.pdf.PdfDocumentHandle
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class PdfIntegrationTest {
    @Test fun textLayerAndExportedMarksPreserveOriginal()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val input=File(context.cacheDir,"qa-text.pdf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("qa/EJEMPLO_PROFELAB_TEXTO_SELECCIONABLE_2P.pdf").use {source->input.outputStream().use {source.copyTo(it)}}
        val original=MessageDigest.getInstance("SHA-256").digest(input.readBytes())
        val uri=Uri.fromFile(input);val boxes=PdfEngine.text(context,uri,0,"")
        assertTrue("Real text positions must exist",boxes.isNotEmpty())
        val first=boxes.first {it.text.isNotBlank() && it.right>it.left && it.bottom>it.top}
        val a=JSONObject().put("type","highlight").put("page",0).put("left",first.left).put("top",first.top).put("right",first.right).put("bottom",first.bottom).put("color",0xFFFFD54FL).put("width",.004)
        val output=PdfEngine.export(context,uri,"",listOf(a))
        assertArrayEquals(original,MessageDigest.getInstance("SHA-256").digest(input.readBytes()))
        PdfDocumentHandle(context,uri).use {before->PdfDocumentHandle(context,Uri.fromFile(output)).use {after->
            assertEquals(before.pageCount,after.pageCount)
            val b=requireNotNull(before.render(0,1000));val c=requireNotNull(after.render(0,1000))
            assertFalse("Flattened copy must visibly differ",b.sameAs(c))
        }}
        output.delete();input.delete()
    }
}
