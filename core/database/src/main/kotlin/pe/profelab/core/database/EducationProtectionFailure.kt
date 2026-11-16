package pe.profelab.core.database

import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteFullException
import android.security.keystore.KeyPermanentlyInvalidatedException
import org.json.JSONException
import pe.profelab.core.model.education.EducationProtectionException
import pe.profelab.core.model.education.EducationProtectionFailureCode
import pe.profelab.core.model.education.EducationProtectionStage
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException

/** Classify by type/stage only. Raw exception messages/causes never cross the private boundary. */
internal fun protectionFailure(stage:EducationProtectionStage,error:Exception):EducationProtectionException {
    if(error is EducationProtectionException)return error
    val chain=generateSequence(error as Throwable?){it.cause}.take(8).toList()
    val code=when {
        chain.any {it is EducationRecordTooLargeException}->EducationProtectionFailureCode.PRIVATE_RECORD_TOO_LARGE
        chain.any {it is EducationKeyMissingException}->EducationProtectionFailureCode.KEY_MISSING
        chain.any {it is KeyPermanentlyInvalidatedException}->EducationProtectionFailureCode.KEY_INVALIDATED
        chain.any {it is AEADBadTagException || it is BadPaddingException}->EducationProtectionFailureCode.AUTHENTICATION_FAILED
        chain.any {it is SQLiteFullException || it is SQLiteDiskIOException || it is java.io.IOException}->EducationProtectionFailureCode.STORAGE_UNAVAILABLE
        chain.any {it is java.security.GeneralSecurityException}->EducationProtectionFailureCode.KEY_UNAVAILABLE
        stage==EducationProtectionStage.SET_SECURE_DELETE->EducationProtectionFailureCode.SECURE_DELETE_UNVERIFIED
        stage==EducationProtectionStage.OPEN_DB->EducationProtectionFailureCode.DATABASE_UNAVAILABLE
        chain.any {it is JSONException || it is IllegalArgumentException || it is IllegalStateException}->EducationProtectionFailureCode.INVALID_PRIVATE_RECORD
        else->EducationProtectionFailureCode.UNKNOWN
    }
    return EducationProtectionException(stage,code,error.javaClass.simpleName)
}
