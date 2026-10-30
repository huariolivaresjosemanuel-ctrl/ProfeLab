package pe.profelab.core.model

enum class AssistantAction(val target:String) {
    HOME("home"),TALLER("taller"),SESSIONS("sessions"),NOTEBOOK("notebook"),LIBRARY("library"),PROFILE("profile"),
    TASKS_TODAY("tasks_today"),RECENT_WORK("recent_work"),INVENTORY("workshop_inventory"),
    RECOMMENDATIONS("workshop_recommendations"),SHOPPING("workshop_shopping"),TEACHER("workshop_teacher_mode"),
    EXPERIMENTS("workshop_experiments"),LIBRARY_SEARCH("library_search"),LIBRARY_IMPORT("library_import")
}
data class AssistantMessage(val id:String,val context:String,val text:String,val label:String,val action:AssistantAction,val requiresLiveData:Boolean)
data class AssistantFacts(val hasInventory:Boolean=false,val hasRecentWork:Boolean=false,val hasShopping:Boolean=false,val hasDocuments:Boolean=false,val hasCurrentProject:Boolean=false,val hasProjects:Boolean=false)
data class AssistantAdvice(val messages:List<AssistantMessage>,val notices:List<String>)

object AssistantPolicy {
    val contexts=setOf("global","home","taller","sessions","notebook","library")
    fun advice(catalog:List<AssistantMessage>,context:String,facts:AssistantFacts,available:Set<AssistantAction>):AssistantAdvice {
        if(context !in contexts || context=="global")return AssistantAdvice(emptyList(),emptyList())
        val notices=mutableListOf<String>()
        if(context=="taller" && !facts.hasShopping)notices+="Todavía no hay una lista de compras registrada."
        if(context=="library" && !facts.hasDocuments)notices+="Todavía no hay PDF propios guardados. Puedes importar un documento."
        val chosen=catalog.filter {m->m.context==context}.toMutableList()
        if(context in setOf("home","taller") && !facts.hasInventory){
            chosen.removeAll {it.action==AssistantAction.RECOMMENDATIONS || it.action==AssistantAction.INVENTORY}
            catalog.find {it.id=="assistant.global.no_inventory"}?.let {chosen+=it}
        }
        if(context in setOf("home","taller") && !facts.hasRecentWork){
            chosen.removeAll {it.action==AssistantAction.RECENT_WORK}
            if(facts.hasProjects)notices+="No hay trabajos pendientes para continuar."
            else catalog.find {it.id=="assistant.global.no_projects"}?.let {chosen+=it}
        }
        catalog.find {it.id=="assistant.global.privacy"}?.let {chosen+=it}
        if(context!="home")catalog.find {it.id=="assistant.global.progress"}?.let {chosen+=it}
        val visible=chosen.distinctBy {it.id}.filter {m->m.action in available && (!m.requiresLiveData || when(m.action){
            AssistantAction.RECENT_WORK->facts.hasRecentWork
            AssistantAction.INVENTORY,AssistantAction.RECOMMENDATIONS->facts.hasInventory
            AssistantAction.SHOPPING->facts.hasShopping
            AssistantAction.LIBRARY_SEARCH->facts.hasDocuments
            else->true
        })}
        return AssistantAdvice(visible,notices)
    }
}
