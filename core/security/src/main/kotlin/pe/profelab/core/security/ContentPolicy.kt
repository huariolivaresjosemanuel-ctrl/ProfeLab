package pe.profelab.core.security

import java.net.URI

object ContentPolicy {
    fun isOfficialSource(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.userInfo == null && uri.port in listOf(-1, 443) &&
            uri.host in setOf("repositorio.minedu.gob.pe", "www.minedu.gob.pe")
    }.getOrDefault(false)

    /** Used by future importers before any extraction. Limits and checksums remain mandatory. */
    fun safeRelativePath(path: String): Boolean = path.isNotBlank() &&
        !path.startsWith('/') && !path.contains('\\') && !path.contains(':') && !path.contains('\u0000') &&
        path.split('/').none { it == ".." || it == "." || it.isEmpty() }
}
