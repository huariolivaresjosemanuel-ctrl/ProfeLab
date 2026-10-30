package pe.profelab.core.security

import org.junit.Assert.*
import org.junit.Test

class ContentPolicyTest {
    @Test fun officialHttpsOnly() {
        assertTrue(ContentPolicy.isOfficialSource("https://repositorio.minedu.gob.pe/handle/20.500.12799/4549"))
        listOf("http://www.minedu.gob.pe/x","https://www.minedu.gob.pe.evil.test/x","https://evil.test@www.minedu.gob.pe/x","https://www.minedu.gob.pe:8443/x","javascript:alert(1)").forEach { assertFalse(it,ContentPolicy.isOfficialSource(it)) }
    }
    @Test fun rejectUnsafeArchivePaths() {
        listOf("../outside","a/../../outside","C:/temp/x","/root/x","a\\b","a//b","./a","").forEach { assertFalse(it,ContentPolicy.safeRelativePath(it)) }
        assertTrue(ContentPolicy.safeRelativePath("projects/mat-001.json"))
    }
}
