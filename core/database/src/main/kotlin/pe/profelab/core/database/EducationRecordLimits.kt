package pe.profelab.core.database

/** Leaves headroom below API26's 2 MiB CursorWindow, counting UTF-8 rather than characters. */
internal class EducationRecordTooLargeException:IllegalArgumentException("Private record exceeds the safe storage budget")
internal object EducationRecordLimits {
    const val maximumStoredBytes=1_900_000
    fun requireFits(id:String,kind:String,title:String,json:String){
        val bytes=listOf(id,kind,title,json).sumOf {it.toByteArray(Charsets.UTF_8).size.toLong()}+128L
        if(bytes>maximumStoredBytes)throw EducationRecordTooLargeException()
    }
    /** Reject before any KeyStore call; GCM adds a 16-byte tag and base64 expands bytes by 4/3. */
    fun requireEncryptedPayloadFits(id:String,kind:String,payloadBytes:Int){
        require(payloadBytes>=0)
        val encoded=4L*((payloadBytes.toLong()+16L+2L)/3L)
        val total=id.toByteArray(Charsets.UTF_8).size+kind.toByteArray(Charsets.UTF_8).size+"Registro educativo privado".toByteArray(Charsets.UTF_8).size+encoded+256L+128L
        if(total>maximumStoredBytes)throw EducationRecordTooLargeException()
    }
}
