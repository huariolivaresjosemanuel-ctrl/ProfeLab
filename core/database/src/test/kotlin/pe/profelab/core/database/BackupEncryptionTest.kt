package pe.profelab.core.database

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupEncryptionTest {
    private val password="Only synthetic QA secret 32".toCharArray()
    private fun encrypt(bytes:ByteArray):ByteArray=ByteArrayOutputStream().also {out->BackupEncryption.output(out,password).use {it.write(bytes)}}.toByteArray()
    private fun decrypt(bytes:ByteArray,secret:CharArray?=password)=BackupEncryption.input(ByteArrayInputStream(bytes),secret).use {it.readBytes()}
    @Test fun portableEnvelopeRoundTripsAndRandomizesEveryExport(){
        val original="Synthetic private data and photo bytes".toByteArray()
        val a=encrypt(original);val b=encrypt(original)
        assertArrayEquals(original,decrypt(a));assertArrayEquals(original,decrypt(b));assertFalse(a.contentEquals(b))
        assertFalse(a.toString(Charsets.UTF_8).contains("Synthetic private data"))
    }
    @Test fun wrongMissingOrShortPasswordFails(){
        val bytes=encrypt("Synthetic fixture".toByteArray())
        assertTrue(runCatching {decrypt(bytes,"Different QA secret 32".toCharArray())}.isFailure)
        assertTrue(runCatching {decrypt(bytes,null)}.isFailure)
        assertTrue(runCatching {BackupEncryption.output(ByteArrayOutputStream(),"short".toCharArray())}.isFailure)
    }
    @Test fun changedCiphertextAndTruncatedTagFailBeforeUse(){
        val bytes=encrypt(ByteArray(512){it.toByte()});val changed=bytes.copyOf().also {it[it.lastIndex-18]=(it[it.lastIndex-18].toInt() xor 1).toByte()}
        assertTrue(runCatching {decrypt(changed)}.isFailure)
        assertTrue(runCatching {decrypt(bytes.copyOf(bytes.size-7))}.isFailure)
    }
    @Test fun saltAndIvAreAuthenticatedAndLegacyBytesStayReadable(){
        val bytes=encrypt("Synthetic fixture".toByteArray())
        for(index in listOf(8,24)){val changed=bytes.copyOf().also {it[index]=(it[index].toInt() xor 1).toByte()};assertTrue(runCatching {decrypt(changed)}.isFailure)}
        val legacy=byteArrayOf(0x50,0x4b,3,4,7,8,9)
        assertArrayEquals(legacy,decrypt(legacy,null))
    }
}
