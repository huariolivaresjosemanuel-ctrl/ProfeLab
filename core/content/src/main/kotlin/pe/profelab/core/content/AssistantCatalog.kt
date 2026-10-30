package pe.profelab.core.content

import android.content.Context
import org.json.JSONObject
import pe.profelab.core.model.*

object AssistantCatalog {
    fun parse(text:String):List<AssistantMessage>{
        JsonLimits.requireBounded(text,maxCharacters=60000,maxDepth=12)
        val root=JSONObject(text)
        require(root.getInt("schemaVersion")==1 && root.getString("appId")=="pe.profelab.app" && root.getBoolean("offlineOnly"))
        require(root.getString("language")=="es-PE")
        val contexts=root.getJSONArray("contexts");require((0 until contexts.length()).map {contexts.getString(it)}.toSet()==AssistantPolicy.contexts)
        val rules=root.getJSONObject("rules");listOf("neverFabricateCounts","noAiOrNetwork","hideDuringImmersivePdf","neverCoverSensitiveInput","requireExistingHandlers","respectReducedMotion").forEach {require(rules.getBoolean(it))}
        val list=root.getJSONArray("messages");require(list.length() in 1..50)
        val routes=setOf(AssistantAction.HOME,AssistantAction.TALLER,AssistantAction.SESSIONS,AssistantAction.NOTEBOOK,AssistantAction.LIBRARY,AssistantAction.PROFILE)
        return (0 until list.length()).map {i->val m=list.getJSONObject(i);val action=m.getJSONObject("action")
            val target=AssistantAction.entries.singleOrNull {it.target==action.getString("target")} ?: error("Acción de ayuda desconocida")
            require(action.getString("type")==if(target in routes)"open_route"else "open_existing_handler")
            require(m.getString("id").matches(Regex("assistant\\.[a-z_]+\\.[a-z_]+")))
            require(m.getString("context") in AssistantPolicy.contexts && m.getString("condition")=="user_request_or_relevant_nonintrusive_state")
            require(m.getString("fallback")=="hide_button_if_no_real_handler")
            val body=m.getString("text");val label=m.getString("buttonLabel");require(body.length in 1..500 && label.length in 1..80)
            require('{' !in body && '}' !in body && '|' !in label)
            AssistantMessage(m.getString("id"),m.getString("context"),body,label,target,m.getBoolean("requiresLiveData"))
        }.also {require(it.map {m->m.id}.distinct().size==it.size)}
    }
    fun load(context:Context)=context.assets.open("assistant/contract_v1.json").bufferedReader().use {parse(it.readText())}
}
