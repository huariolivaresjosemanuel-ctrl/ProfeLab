package pe.profelab.core.ui

import androidx.compose.runtime.*
import pe.profelab.core.model.AssistantAction

/** Navigation callbacks stay in their owning feature, with availability exposed as a stable key set. */
class AssistantRegistry {
    private data class Slot(val owner:Any,val actions:Set<AssistantAction>,val current:()->Map<AssistantAction,()->Unit>)
    private val slots=mutableStateMapOf<String,Slot>()
    fun actions(context:String)=slots[context]?.actions.orEmpty()
    fun invoke(context:String,action:AssistantAction):Boolean {val handler=slots[context]?.current?.invoke()?.get(action) ?: return false;handler();return true}
    internal fun update(context:String,owner:Any,keys:Set<AssistantAction>,current:()->Map<AssistantAction,()->Unit>){
        val old=slots[context];if(old==null || old.owner!==owner || old.actions!=keys)slots[context]=Slot(owner,keys,current)
    }
    internal fun remove(context:String,owner:Any){if(slots[context]?.owner===owner)slots.remove(context)}
}
val LocalAssistantRegistry=staticCompositionLocalOf<AssistantRegistry?> {null}
@Composable
fun BindAssistant(context:String,handlers:Map<AssistantAction,()->Unit>){
    val registry=LocalAssistantRegistry.current ?: return
    val owner=remember {Any()};val latest=rememberUpdatedState(handlers)
    SideEffect {registry.update(context,owner,handlers.keys.toSet()){latest.value}}
    DisposableEffect(registry,context,owner){onDispose {registry.remove(context,owner)}}
}
