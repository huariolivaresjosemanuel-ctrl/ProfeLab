package pe.profelab.core.model.education

import org.junit.Assert.*
import org.junit.Test

class EducationFamilyPolicyTest {
    @Test fun authorizationFailsClosedForRevocationScopeVerificationAndTime(){
        fun block(authorized:Boolean=true,verified:Boolean=true,scope:String="LEARNING_REPORT",from:Long=10,until:Long=30,revoked:Long?=null,archived:Boolean=false,now:Long=20)=EducationFamilyPolicy.authorizationBlock(authorized,verified,scope,from,until,revoked,archived,now)
        assertNull(block());assertNotNull(block(authorized=false));assertNotNull(block(verified=false));assertNotNull(block(scope="OTHER"))
        assertNotNull(block(revoked=15));assertNotNull(block(archived=true));assertNotNull(block(now=9));assertNotNull(block(now=31));assertNotNull(block(until=9))
        assertNull(block(now=10));assertNull(block(now=30))
    }
    @Test fun channelsAddressOnlyOneContactAndEscapeTheReviewedBody(){
        val text="Actividad & criterio\nOrientación: más práctica"
        val wa=EducationFamilyPolicy.externalUri("WHATSAPP","+51 900 000 000","",text)
        assertTrue(wa.startsWith("https://wa.me/51900000000?text="));assertTrue(wa.contains("%26"));assertFalse(wa.contains("\n"))
        assertTrue(EducationFamilyPolicy.externalUri("SMS","+51 900 000 000","",text).startsWith("smsto:+51900000000?body="))
        assertTrue(EducationFamilyPolicy.externalUri("EMAIL","","guardian@example.invalid",text).startsWith("mailto:guardian%40example.invalid?"))
        assertFalse(EducationFamilyPolicy.validEmail("a@example.invalid,b@example.invalid"));assertFalse(EducationFamilyPolicy.validEmail("a@example.invalid\nBcc:b@example.invalid"))
        assertFalse(EducationFamilyPolicy.validPhone("+51;900000000"));assertFalse(EducationFamilyPolicy.safeBody("informe\u0000"))
        assertTrue(runCatching {EducationFamilyPolicy.externalUri("WHATSAPP","","",text)}.isFailure)
    }
}
