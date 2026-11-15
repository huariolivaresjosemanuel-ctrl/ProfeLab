package pe.profelab.core.database

import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable authenticated envelope around the complete ZIP, including attachments and manifest. */
internal object BackupEncryption {
    private val magic="PFLB32E1".toByteArray(Charsets.US_ASCII)
    private const val iterations=210_000
    fun requirePassphrase(passphrase:CharArray){require(passphrase.size in 12..1024){"Usa una contraseña de respaldo de 12 a 1024 caracteres y consérvala fuera del dispositivo"}}
    private fun cipher(mode:Int,passphrase:CharArray,salt:ByteArray,iv:ByteArray):Cipher {
        requirePassphrase(passphrase)
        val spec=PBEKeySpec(passphrase,salt,iterations,256)
        val raw=try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}finally {spec.clearPassword()}
        try {return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode,SecretKeySpec(raw,"AES"),GCMParameterSpec(128,iv));updateAAD(magic+salt+iv)
        }}finally {raw.fill(0)}
    }
    fun output(output:OutputStream,passphrase:CharArray):OutputStream {
        requirePassphrase(passphrase)
        val random=SecureRandom();val salt=ByteArray(16).also {random.nextBytes(it)};val iv=ByteArray(12).also {random.nextBytes(it)}
        output.write(magic+salt+iv)
        return CipherOutputStream(output,cipher(Cipher.ENCRYPT_MODE,passphrase,salt,iv))
    }
    /** Legacy ZIP is readable; encrypted input requires a secret and validates its tag at EOF. */
    fun input(input:InputStream,passphrase:CharArray?):InputStream {
        val source=PushbackInputStream(input,magic.size);val header=ByteArray(magic.size);var count=0
        while(count<header.size){val n=source.read(header,count,header.size-count);if(n<0)break;count+=n}
        if(count!=magic.size || !header.contentEquals(magic)){source.unread(header,0,count);return source}
        require(passphrase!=null){"Este respaldo está cifrado; introduce su contraseña"}
        val fields=ByteArray(28);var offset=0
        while(offset<fields.size){val n=source.read(fields,offset,fields.size-offset);require(n>0){"Respaldo cifrado incompleto"};offset+=n}
        return CipherInputStream(source,cipher(Cipher.DECRYPT_MODE,passphrase,fields.copyOfRange(0,16),fields.copyOfRange(16,28)))
    }
}
