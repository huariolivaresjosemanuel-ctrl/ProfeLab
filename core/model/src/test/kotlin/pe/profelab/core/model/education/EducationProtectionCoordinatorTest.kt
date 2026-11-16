package pe.profelab.core.model.education

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EducationProtectionCoordinatorTest {
    @Test fun concurrentWelcomeGateAndRetryShareOneOperationAndOneCleanup()=runTest {
        var verified=0;var cleaned=0;val release=CompletableDeferred<Unit>()
        val gate=EducationProtectionCoordinator(backgroundScope,{report->verified++;report(EducationProtectionStage.VERIFY_EDU);release.await()},{cleaned++})
        val callers=List(24){index->async {if(index%2==0)gate.ensureReady()else gate.retry()}}
        runCurrent();assertEquals(1,verified);assertEquals(0,cleaned);assertEquals(EducationProtectionStatus.VERIFYING,gate.state.value.status)
        release.complete(Unit)
        assertTrue(callers.awaitAll().all {it.status==EducationProtectionStatus.READY && it.attempt==1})
        assertEquals(1,verified);assertEquals(1,cleaned)
    }

    @Test fun readyIsIdempotentAcrossFurtherCallersAndExplicitRetry()=runTest {
        var verified=0;var cleaned=0
        val gate=EducationProtectionCoordinator(backgroundScope,{verified++},{cleaned++})
        val first=gate.ensureReady()
        repeat(10){assertEquals(first,gate.ensureReady());assertEquals(first,gate.retry())}
        assertEquals(EducationProtectionStatus.READY,first.status);assertEquals(1,verified);assertEquals(1,cleaned)
    }

    @Test fun transientFailureNeedsExplicitRetryAndNeverLoopsOnScreenRecreation()=runTest {
        var verified=0;var cleaned=0
        val gate=EducationProtectionCoordinator(backgroundScope,{
            verified++
            if(verified==1)throw EducationProtectionException(EducationProtectionStage.OPEN_DB,EducationProtectionFailureCode.DATABASE_UNAVAILABLE,"SQLiteException")
        },{cleaned++})
        val failure=gate.ensureReady()
        assertEquals(EducationProtectionStatus.RETRYABLE_ERROR,failure.status)
        repeat(5){assertEquals(failure,gate.ensureReady())}
        assertEquals(1,verified);assertEquals(0,cleaned)
        val recovered=gate.retry();assertEquals(EducationProtectionStatus.READY,recovered.status);assertEquals(2,recovered.attempt);assertEquals(2,verified);assertEquals(1,cleaned)
    }

    @Test fun unrecoverableKeyOrAuthenticationFailuresRemainClosedWithoutRegeneration()=runTest {
        val failures=listOf(EducationProtectionFailureCode.KEY_MISSING,EducationProtectionFailureCode.KEY_INVALIDATED,EducationProtectionFailureCode.AUTHENTICATION_FAILED,EducationProtectionFailureCode.INVALID_PRIVATE_RECORD,EducationProtectionFailureCode.PRIVATE_RECORD_TOO_LARGE)
        failures.forEach {code->
            var verified=0;var cleaned=0
            val gate=EducationProtectionCoordinator(backgroundScope,{verified++;throw EducationProtectionException(EducationProtectionStage.VERIFY_EDU,code,"AEADBadTagException")},{cleaned++})
            val first=gate.ensureReady();assertEquals(EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR,first.status)
            repeat(3){assertEquals(first,gate.retry());assertEquals(first,gate.ensureReady())}
            assertEquals(1,verified);assertEquals(0,cleaned)
        }
    }

    @Test fun cleanupFailureReauthenticatesStorageBeforeRetryingCleanup()=runTest {
        var verified=0;var cleaned=0
        val gate=EducationProtectionCoordinator(backgroundScope,{verified++},{cleaned++;if(cleaned==1)throw java.io.IOException("Synthetic private path must not enter state")})
        val failed=gate.ensureReady()
        assertEquals(EducationProtectionStatus.RETRYABLE_ERROR,failed.status);assertEquals(EducationProtectionStage.CLEANUP_STAGING,failed.stage)
        assertFalse(failed.toString().contains("private path"));assertEquals("IOException",failed.exceptionType)
        assertEquals(EducationProtectionStatus.READY,gate.retry().status);assertEquals(2,verified);assertEquals(2,cleaned)
    }

    @Test fun keyLostAfterCleanupFailureCannotReuseAnEarlierSuccessfulAuthentication()=runTest {
        var verified=0;var cleaned=0
        val gate=EducationProtectionCoordinator(backgroundScope,{
            verified++
            if(verified==2)throw EducationProtectionException(EducationProtectionStage.VERIFY_EDU,EducationProtectionFailureCode.KEY_MISSING,"EducationKeyMissingException")
        },{cleaned++;throw java.io.IOException("Synthetic staging failure")})
        assertEquals(EducationProtectionStage.CLEANUP_STAGING,gate.ensureReady().stage)
        val failed=gate.retry()
        assertEquals(EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR,failed.status)
        assertEquals(EducationProtectionFailureCode.KEY_MISSING,failed.failureCode)
        assertEquals(2,verified);assertEquals(1,cleaned)
        assertEquals(failed,gate.retry());assertEquals(failed,gate.ensureReady())
    }

    @Test fun rotatingOrCancellingCallerDoesNotCancelApplicationOwnedVerification()=runTest {
        var verified=0;val release=CompletableDeferred<Unit>()
        val gate=EducationProtectionCoordinator(backgroundScope,{verified++;release.await()},{})
        val first=async {gate.ensureReady()};runCurrent();assertEquals(1,verified)
        first.cancelAndJoin();assertEquals(EducationProtectionStatus.VERIFYING,gate.state.value.status)
        val recreated=async {gate.ensureReady()};runCurrent();assertEquals(1,verified)
        release.complete(Unit);assertEquals(EducationProtectionStatus.READY,recreated.await().status);assertEquals(1,verified)
    }

    @Test fun cancelledStorageAttemptStaysClosedAndCanBeRetriedSafely()=runTest {
        var verified=0;var cleaned=0
        val gate=EducationProtectionCoordinator(backgroundScope,{verified++;if(verified==1)throw CancellationException("Synthetic private diagnostic")},{cleaned++})
        assertTrue(runCatching {gate.ensureReady()}.exceptionOrNull() is CancellationException)
        val failed=gate.state.value;assertEquals(EducationProtectionStatus.RETRYABLE_ERROR,failed.status);assertEquals(EducationProtectionFailureCode.CANCELLED,failed.failureCode);assertEquals(0,cleaned)
        assertFalse(failed.toString().contains("private diagnostic"));assertEquals(EducationProtectionStatus.READY,gate.retry().status);assertEquals(2,verified);assertEquals(1,cleaned)
    }

    @Test fun cancelledApplicationScopeCannotLeaveAGateFalselyReady()=runTest {
        val job=Job().apply {cancel()};val scope=CoroutineScope(coroutineContext+job)
        var verified=0
        val gate=EducationProtectionCoordinator(scope,{verified++},{})
        assertTrue(runCatching {gate.ensureReady()}.exceptionOrNull() is CancellationException)
        assertEquals(0,verified);assertEquals(EducationProtectionStatus.RETRYABLE_ERROR,gate.state.value.status);assertEquals(EducationProtectionFailureCode.CANCELLED,gate.state.value.failureCode)
    }

    @Test fun readinessIsNotPublishedByAnEarlyRepositoryCallback()=runTest {
        val cleanupRelease=CompletableDeferred<Unit>()
        val gate=EducationProtectionCoordinator(backgroundScope,{report->report(EducationProtectionStage.READY)},{cleanupRelease.await()})
        val caller=async {gate.ensureReady()};runCurrent()
        assertEquals(EducationProtectionStatus.VERIFYING,gate.state.value.status);assertEquals(EducationProtectionStage.CLEANUP_STAGING,gate.state.value.stage)
        cleanupRelease.complete(Unit);assertEquals(EducationProtectionStatus.READY,caller.await().status)
    }

    @Test fun boundaryExceptionAndUnknownFailureDoNotExposeMessagesOrRawCauses()=runTest {
        val typed=EducationProtectionException(EducationProtectionStage.READ_LEGACY,EducationProtectionFailureCode.INVALID_PRIVATE_RECORD,"not-an-exception: guardian@example.invalid")
        assertNull(typed.message);assertNull(typed.cause);assertEquals("Exception",typed.exceptionType);assertFalse(typed.toString().contains("guardian"))
        val gate=EducationProtectionCoordinator(backgroundScope,{report->report(EducationProtectionStage.READ_LEGACY);throw IllegalStateException("A synthetic student, phone and payload must stay private")},{})
        val failed=gate.ensureReady();assertEquals(EducationProtectionFailureCode.UNKNOWN,failed.failureCode);assertEquals(EducationProtectionStage.READ_LEGACY,failed.stage);assertEquals("IllegalStateException",failed.exceptionType)
        assertFalse(failed.toString().contains("student"));assertFalse(failed.toString().contains("payload"))
    }
}
