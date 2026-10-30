package pe.profelab.core.model

enum class WelcomeMode { NONE, REGISTER, EXISTING_INTRO, RETURNING }

/** Registration, introduction and one activity session are deliberately different states. */
object GreetingPolicy {
    fun mode(prefs:UserPreferences,profileExists:Boolean,sessionHandled:Boolean,externalContent:Boolean):WelcomeMode = when {
        sessionHandled || externalContent -> WelcomeMode.NONE
        !prefs.onboardingCompleted && !profileExists -> WelcomeMode.REGISTER
        !prefs.mascotVisible -> WelcomeMode.NONE
        !prefs.welcomeIntroSeen -> WelcomeMode.EXISTING_INTRO
        prefs.personalizedGreetings -> WelcomeMode.RETURNING
        else -> WelcomeMode.NONE
    }
    fun message(hour:Int,prefs:UserPreferences,legacyName:String=""):String {
        val period=HomeRules.greeting(hour)
        if(!prefs.personalizedGreetings)return "$period. ¡Qué bueno verte!"
        val name=PersonalizationRules.name(prefs.displayName.ifBlank {legacyName})
        if(name.isBlank())return "$period. ¡Hola! ¿Qué crearemos hoy?"
        val role=prefs.role.greeting.takeIf {it.isNotBlank()}?.plus(" ").orEmpty()
        return "$period. ¡Hola, $role$name!"
    }
}
