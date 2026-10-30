package pe.profelab.core.files

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.profelab.core.model.newId
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

data class StoredFile(val id: String, val file: File, val sha256: String, val bytes: Long)

/** Foundation for H5/H6; callers must validate MIME/content and obtain user consent. */
class PrivateFiles(context: Context) {
    private val root = File(context.filesDir, "documents")
    suspend fun copyFrom(input: InputStream, maximumBytes: Long): StoredFile = withContext(Dispatchers.IO) {
        require(maximumBytes > 0)
        check(root.isDirectory || root.mkdirs())
        val id = newId()
        val temp = File(root,"$id.partial")
        val final = File(root,id)
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        try {
            input.use { source -> temp.outputStream().use { target ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    count += read
                    require(count <= maximumBytes) { "El archivo supera el límite permitido" }
                    digest.update(buffer,0,read); target.write(buffer,0,read)
                }
                target.fd.sync()
            } }
            check(temp.renameTo(final)) { "No se pudo finalizar la copia" }
            StoredFile(id,final,digest.digest().joinToString("") { "%02x".format(it) },count)
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }
}
