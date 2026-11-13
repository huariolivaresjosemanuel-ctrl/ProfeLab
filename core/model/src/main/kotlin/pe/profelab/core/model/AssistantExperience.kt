package pe.profelab.core.model

/** Small contextual choices, with the complete catalogue still available as a secondary list. */
object AssistantExperience {
    fun tallerGreeting(hour:Int)="¡${HomeRules.greeting(hour)}! ¿Qué vamos a preparar hoy?"
    fun showTallerWelcome(context:String?,prefs:UserPreferences)=context=="taller" && prefs.mascotVisible && prefs.personalizedGreetings && !prefs.tallerWelcomeSeen
    fun advice(base:AssistantAdvice,context:String,available:Set<AssistantAction>):AssistantAdvice {
        val extra=listOf(
            AssistantMessage("assistant.context.explore","taller","Elige una actividad y consulta sus materiales y guía.","Explorar actividades",AssistantAction.EXPLORE_WORKSHOP,false),
            AssistantMessage("assistant.context.inventory","taller","Puedes consultar tu inventario o seguir sin registrarlo.","Mi inventario",AssistantAction.INVENTORY,false),
            AssistantMessage("assistant.context.step","taller","Consulta la instrucción del paso actual.","Ayuda sobre este paso",AssistantAction.STEP_HELP,false),
            AssistantMessage("assistant.context.safety","taller","Consulta las advertencias incluidas en esta actividad.","Seguridad y recomendaciones",AssistantAction.SAFETY,false)
        ).filter {it.context==context && it.action in available}
        return base.copy(messages=(base.messages+extra.filter {e->base.messages.none {it.action==e.action}}).distinctBy {it.id})
    }
    fun primary(advice:AssistantAdvice,context:String):List<AssistantMessage> {
        val priority=when {
            advice.messages.any {it.action==AssistantAction.STEP_HELP}->listOf(AssistantAction.STEP_HELP,AssistantAction.TEACHER,AssistantAction.SAFETY)
            context=="taller"->listOf(AssistantAction.EXPLORE_WORKSHOP,AssistantAction.INVENTORY,AssistantAction.RECENT_WORK)
            context=="home"->listOf(AssistantAction.RECENT_WORK,AssistantAction.TASKS_TODAY,AssistantAction.TALLER)
            context=="library"->listOf(AssistantAction.LIBRARY_SEARCH,AssistantAction.LIBRARY_IMPORT,AssistantAction.RECENT_WORK)
            else->listOf(AssistantAction.RECENT_WORK,AssistantAction.NOTEBOOK,AssistantAction.SESSIONS)
        }
        return (priority.mapNotNull {action->advice.messages.firstOrNull {it.action==action}}+advice.messages).distinctBy {it.action}.take(3)
    }
}
