package pe.profelab.core.database

/** Leaves headroom below API26's 2 MiB CursorWindow, counting UTF-8 rather than characters. */
internal object EducationRecordLimits {
    const val maximumStoredBytes=1_900_000
    fun requireFits(id:String,kind:String,title:String,json:String){
        val bytes=listOf(id,kind,title,json).sumOf {it.toByteArray(Charsets.UTF_8).size.toLong()}+128L
        require(bytes<=maximumStoredBytes){"El registro privado supera el tamaño seguro de almacenamiento. Reduce su contenido o divídelo en registros menores; los datos anteriores se conservan."}
    }
}
