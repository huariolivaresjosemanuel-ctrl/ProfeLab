package pe.profelab.core.files

import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import pe.profelab.core.model.*
import org.json.JSONObject

class DocumentFiles(private val context: Context) {
    val directory=File(context.filesDir,"documents").apply {mkdirs()}
    fun file(id: String): File {require(id.matches(Regex("[a-zA-Z0-9-]+")));return File(directory,id)}
    suspend fun importUri(uri: String, mime: String, maximum: Long=100L*1024*1024): StoredFile {
        val source=Uri.parse(uri);require(source.scheme=="content")
        val input=context.contentResolver.openInputStream(source) ?: error("Archivo no disponible")
        val result=PrivateFiles(context).copyFrom(input,maximum)
        try {
            if(mime=="application/pdf") require(result.file.inputStream().use {val header=ByteArray(5); java.io.DataInputStream(it).readFully(header); header.contentEquals("%PDF-".toByteArray())}) {"No es un PDF"}
            return result
        } catch(e:Exception){result.file.delete();throw e}
    }
    suspend fun exportPdf(title: String,text: String,images:List<File> = emptyList()): File = withContext(Dispatchers.IO) {
        val out=File(context.cacheDir,"exports").apply {mkdirs()};val file=File(out,"${newId()}.pdf")
        // Android PdfDocument exposes close(), but does not implement Closeable/AutoCloseable.
        val doc=PdfDocument()
        try {
            val paint=Paint().apply {textSize=12f;isAntiAlias=true}
            val lines= mutableListOf<String>()
            (title+"\nDocumento elaborado por el usuario — ProfeLab\n\n"+text).lines().forEach { paragraph ->
                var remaining=paragraph
                if(remaining.isBlank()) lines.add("")
                while(remaining.isNotEmpty()) {
                    val length=paint.breakText(remaining,true,500f,null).coerceAtLeast(1)
                    lines.add(remaining.take(length));remaining=remaining.drop(length)
                }
            }
            lines.chunked(44).forEachIndexed { i,chunk ->
                val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,i+1).create())
                try {chunk.forEachIndexed {n,line->page.canvas.drawText(line,45f,50f+n*17,paint)}}
                finally {doc.finishPage(page)}
            }
            images.forEachIndexed {i,image->
                val options=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
                android.graphics.BitmapFactory.decodeFile(image.absolutePath,options)
                var sample=1;while(options.outWidth/sample>1600||options.outHeight/sample>1600)sample*=2
                val bitmap=android.graphics.BitmapFactory.decodeFile(image.absolutePath,android.graphics.BitmapFactory.Options().apply {inSampleSize=sample}) ?: error("Imagen no disponible")
                try {
                    val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,(lines.size+43)/44+i+1).create())
                    try {
                        val scale=minOf(505f/bitmap.width,740f/bitmap.height)
                        page.canvas.drawBitmap(bitmap,null,android.graphics.RectF(45f,50f,45f+bitmap.width*scale,50f+bitmap.height*scale),paint)
                    } finally {doc.finishPage(page)}
                } finally {bitmap.recycle()}
            }
            file.outputStream().use {doc.writeTo(it)}
            file
        } catch(failure:Throwable) {
            file.delete()
            throw failure
        } finally {
            doc.close()
        }
    }
    suspend fun writeTo(source: File, destination: String)=withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(Uri.parse(destination),"wt")!!.use {out->source.inputStream().use {it.copyTo(out)}}
    }
    fun share(source: File) {
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",source)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartir copia PDF"))
    }
}
