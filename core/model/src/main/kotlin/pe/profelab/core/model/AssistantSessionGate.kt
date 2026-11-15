package pe.profelab.core.model

/** Process-local frequency, with no preferences reset or student data. Owned by AppGraph. */
class AssistantSessionGate {
    private val claimed=linkedSetOf<String>()
    @Volatile var providedByWelcome=false
        private set
    @Synchronized fun welcome(){providedByWelcome=true;claim("greeting")}
    @Synchronized fun isClaimed(event:String,executionId:String="")="$event:$executionId" in claimed
    @Synchronized fun claim(event:String,executionId:String=""):Boolean {
        require(event in setOf("greeting","preparation","evaluation","saved"))
        if(event=="greeting")require(executionId.isBlank())
        if(event!="greeting")require(executionId.isNotBlank())
        val key="$event:$executionId"
        if(key in claimed || claimed.size>=2048)return false
        claimed+=key;return true
    }
    fun saved(operationId:String,success:Boolean):Boolean=success && claim("saved",operationId)
}
