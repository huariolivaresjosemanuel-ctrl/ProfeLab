package pe.profelab.core.model

enum class WelcomePhase { NONE, ENTRY, FORM, GREETING }
enum class WelcomeClip(val asset:String,val poster:String,val durationMs:Long) {
    ENTRY("welcome/flow_entry_v1.mp4","welcome/flow_entry_poster_v1.jpg",3625),
    GREETING("welcome/flow_greeting_v1.mp4","welcome/flow_greeting_poster_v1.jpg",4625)
}
object WelcomeMediaPolicy {
    fun initial(mode:WelcomeMode)=when(mode){WelcomeMode.NONE->WelcomePhase.NONE;WelcomeMode.RETURNING->WelcomePhase.GREETING;else->WelcomePhase.ENTRY}
    fun clip(phase:WelcomePhase)=when(phase){WelcomePhase.ENTRY->WelcomeClip.ENTRY;WelcomePhase.GREETING->WelcomeClip.GREETING;else->null}
    fun canPlay(prefs:UserPreferences,powerSave:Boolean,systemAnimations:Boolean)=prefs.mascotVisible && !prefs.reduceMotion && !powerSave && systemAnimations
}
