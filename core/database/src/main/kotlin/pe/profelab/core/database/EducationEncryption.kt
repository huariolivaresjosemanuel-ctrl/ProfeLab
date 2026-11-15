package pe.profelab.core.database

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-bound key never leaves AndroidKeyStore. Backup uses a separate portable key. */
internal object EducationEncryption {
    private const val alias="pe.profelab.private.education.v1"
    private const val marker="encryptedEducation"
    @Synchronized private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let {return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).setKeySize(256).build())
        }.generateKey()
    }
    fun encrypted(json:String):Boolean {
        if(!json.contains("\"$marker\""))return false
        val body=JSONObject(json)
        return body.has(marker) && (body.optString("privateFormat")=="profelab-education-v1" || (body.has("iv") && body.has("ciphertext")))
    }
    fun privateParentId(revisionId:String):String? {
        if(!revisionId.startsWith("history:"))return null
        val parent=revisionId.removePrefix("history:").substringBeforeLast(':',"")
        val number=revisionId.substringAfterLast(':').toLongOrNull()
        return parent.takeIf {it.isNotBlank() && number!=null && number>0}
    }
    fun parentIds(records:List<WorkRecord>)=records.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet()
    fun preservedRevisionIds(rows:List<WorkspaceEntity>)=rows.filter {it.kind=="revision" && encrypted(it.json)}.map {it.id}.toSet()
    private fun aad(id:String,kind:String)="profelab:education:v1:$id:$kind".toByteArray(Charsets.UTF_8)
    fun read(entity:WorkspaceEntity):WorkRecord {
        if((!entity.kind.startsWith("edu_") && entity.kind!="revision") || !entity.json.contains("\"$marker\""))return WorkRecord(entity.id,entity.kind,entity.title,entity.json,entity.updatedAt,entity.revision,entity.archived)
        if(entity.kind=="revision" && !encrypted(entity.json))return WorkRecord(entity.id,entity.kind,entity.title,entity.json,entity.updatedAt,entity.revision,entity.archived)
        val body=JSONObject(entity.json)
        if(!body.has(marker))return WorkRecord(entity.id,entity.kind,entity.title,entity.json,entity.updatedAt,entity.revision,entity.archived)
        require(body.getInt(marker)==1){"Versión de protección privada no compatible"}
        val iv=Base64.getDecoder().decode(body.getString("iv"));require(iv.size==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv));updateAAD(aad(entity.id,entity.kind))}
        val plaintext=cipher.doFinal(Base64.getDecoder().decode(body.getString("ciphertext")))
        try {val payload=JSONObject(plaintext.toString(Charsets.UTF_8));return WorkRecord(entity.id,entity.kind,payload.getString("title"),payload.getString("json"),entity.updatedAt,entity.revision,entity.archived)}
        finally {plaintext.fill(0)}
    }
    fun write(record:WorkRecord,forcePrivate:Boolean=false):WorkspaceEntity {
        if(!forcePrivate && !record.kind.startsWith("edu_"))return WorkspaceEntity(record.id,record.kind,record.title,record.json,record.updatedAt,record.revision,record.archived)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.ENCRYPT_MODE,key());updateAAD(aad(record.id,record.kind))}
        val plaintext=JSONObject().put("title",record.title).put("json",record.json).toString().toByteArray(Charsets.UTF_8)
        val ciphertext=try {cipher.doFinal(plaintext)}finally {plaintext.fill(0)}
        val body=JSONObject().put("privateFormat","profelab-education-v1").put(marker,1).put("iv",Base64.getEncoder().encodeToString(cipher.iv)).put("ciphertext",Base64.getEncoder().encodeToString(ciphertext)).toString()
        EducationRecordLimits.requireFits(record.id,record.kind,"Registro educativo privado",body)
        return WorkspaceEntity(record.id,record.kind,"Registro educativo privado",body,record.updatedAt,record.revision,record.archived)
    }
    fun decodeBackupRow(row:JSONObject):JSONObject {
        val record=read(WorkspaceEntity(row.getString("id"),row.getString("kind"),row.getString("title"),row.getString("json"),row.getLong("updatedAt"),row.getInt("revision"),row.getInt("archived")!=0))
        return row.put("title",record.title).put("json",record.json)
    }
    fun encodeBackupRow(row:JSONObject,privateParentIds:Set<String>,privateRevisionIds:Set<String>):JSONObject {
        val record=WorkRecord(row.getString("id"),row.getString("kind"),row.getString("title"),row.getString("json"),row.getLong("updatedAt"),row.getInt("revision"),row.getInt("archived")!=0)
        val entity=record.entity(privateParentIds,privateRevisionIds)
        return row.put("title",entity.title).put("json",entity.json)
    }
}

internal fun WorkspaceEntity.domain()=EducationEncryption.read(this)
internal fun WorkRecord.entity(privateParentIds:Set<String> = emptySet(),privateRevisionIds:Set<String> = emptySet())=EducationEncryption.write(this,kind=="revision" && (id in privateRevisionIds || EducationEncryption.privateParentId(id) in privateParentIds))
