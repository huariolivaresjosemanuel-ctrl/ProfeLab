package pe.profelab.core.model.education

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Stable diagnostics contain phase/codes only: never a row, path, secret, IV or exception message. */
enum class EducationProtectionStage { OPEN_DB,SET_SECURE_DELETE,READ_LEGACY,ENCRYPT_EDU,VERIFY_EDU,CLEANUP_STAGING,READY }
enum class EducationProtectionStatus { VERIFYING,READY,RETRYABLE_ERROR,UNRECOVERABLE_PROTECTED_ERROR }
enum class EducationProtectionFailureCode {
    DATABASE_UNAVAILABLE,SECURE_DELETE_UNVERIFIED,KEY_MISSING,KEY_UNAVAILABLE,KEY_INVALIDATED,
    AUTHENTICATION_FAILED,INVALID_PRIVATE_RECORD,PRIVATE_RECORD_TOO_LARGE,STORAGE_UNAVAILABLE,UNKNOWN,CANCELLED;

    val recoverable get()=this !in setOf(KEY_MISSING,KEY_INVALIDATED,AUTHENTICATION_FAILED,INVALID_PRIVATE_RECORD,PRIVATE_RECORD_TOO_LARGE)
}

private fun sanitizedExceptionType(value:String)=value.takeIf {it.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]{0,79}"))} ?: "Exception"

/** A boundary exception deliberately carries neither raw cause nor its potentially private message. */
class EducationProtectionException(val stage:EducationProtectionStage,val code:EducationProtectionFailureCode,exceptionType:String="Exception"):Exception() {
    val exceptionType=sanitizedExceptionType(exceptionType)
}

data class EducationProtectionState(
    val status:EducationProtectionStatus=EducationProtectionStatus.VERIFYING,
    val stage:EducationProtectionStage=EducationProtectionStage.OPEN_DB,
    val attempt:Int=0,
    val failureCode:EducationProtectionFailureCode?=null,
    val exceptionType:String?=null,
)

/**
 * One process-owned opening operation, shared by welcome and Compose. The injected Application scope
 * owns its job, so cancelling a caller (for example rotation) cannot cancel or duplicate migration.
 * Error states are stable: re-entering a screen never triggers an automatic retry. Every explicit
 * retry authenticates storage afresh before cleanup; the repository preserves existing ciphertext.
 */
class EducationProtectionCoordinator(
    private val scope:CoroutineScope,
    private val verifyStorage:suspend ((EducationProtectionStage)->Unit)->Unit,
    private val cleanup:suspend ()->Unit,
) {
    private val mutex=Mutex()
    private val mutableState=MutableStateFlow(EducationProtectionState())
    val state:StateFlow<EducationProtectionState> = mutableState.asStateFlow()
    private var inFlight:Deferred<EducationProtectionState>?=null

    suspend fun ensureReady():EducationProtectionState=request(explicitRetry=false)
    suspend fun retry():EducationProtectionState=request(explicitRetry=true)

    private suspend fun request(explicitRetry:Boolean):EducationProtectionState {
        val selected=mutex.withLock {
            val running=inFlight
            val current=mutableState.value
            when {
                running!=null -> null to running
                current.status==EducationProtectionStatus.READY -> current to null
                current.status==EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR -> current to null
                current.status==EducationProtectionStatus.RETRYABLE_ERROR && !explicitRetry -> current to null
                else -> {
                    val attempt=current.attempt+1
                    mutableState.value=EducationProtectionState(stage=EducationProtectionStage.OPEN_DB,attempt=attempt)
                    val task=scope.async(start=CoroutineStart.LAZY) { perform(attempt) }
                    inFlight=task
                    null to task
                }
            }
        }
        selected.first?.let {return it}
        val task=requireNotNull(selected.second)
        task.start()
        try {return task.await()}
        catch(cancelled:CancellationException) {
            // A scope may already have been cancelled before a lazy task entered perform().
            // A cancelled UI caller leaves a still-running Application job untouched.
            if(task.isCancelled && task.isCompleted)withContext(NonCancellable) {mutex.withLock {
                if(inFlight===task){
                    mutableState.value=mutableState.value.copy(status=EducationProtectionStatus.RETRYABLE_ERROR,failureCode=EducationProtectionFailureCode.CANCELLED,exceptionType="CancellationException")
                    inFlight=null
                }
            }}
            throw cancelled
        }
    }

    private fun stage(attempt:Int,value:EducationProtectionStage) {
        val current=mutableState.value
        if(current.attempt==attempt && current.status==EducationProtectionStatus.VERIFYING)
            mutableState.value=current.copy(stage=value)
    }
    private suspend fun publish(result:EducationProtectionState):EducationProtectionState = withContext(NonCancellable) {
        mutex.withLock {
            mutableState.value=result
            inFlight=null
            result
        }
    }
    private suspend fun perform(attempt:Int):EducationProtectionState {
        try {
            verifyStorage {value->
                // Only the coordinator may publish successful readiness, after cleanup.
                if(value!=EducationProtectionStage.READY && value!=EducationProtectionStage.CLEANUP_STAGING)stage(attempt,value)
            }
            stage(attempt,EducationProtectionStage.CLEANUP_STAGING)
            cleanup()
            return publish(EducationProtectionState(EducationProtectionStatus.READY,EducationProtectionStage.READY,attempt))
        }catch(cancelled:CancellationException){
            publish(EducationProtectionState(EducationProtectionStatus.RETRYABLE_ERROR,mutableState.value.stage,attempt,EducationProtectionFailureCode.CANCELLED,"CancellationException"))
            throw cancelled
        }catch(failure:EducationProtectionException){
            return publish(EducationProtectionState(if(failure.code.recoverable)EducationProtectionStatus.RETRYABLE_ERROR else EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR,failure.stage,attempt,failure.code,failure.exceptionType))
        }catch(failure:Exception){
            return publish(EducationProtectionState(EducationProtectionStatus.RETRYABLE_ERROR,mutableState.value.stage,attempt,EducationProtectionFailureCode.UNKNOWN,sanitizedExceptionType(failure.javaClass.simpleName)))
        }
    }
}
