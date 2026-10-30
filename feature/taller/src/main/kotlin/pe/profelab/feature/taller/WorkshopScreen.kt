package pe.profelab.feature.taller

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopSeed
import pe.profelab.core.model.*
import pe.profelab.core.ui.modelFactory

@Composable
fun WorkshopScreen(all:List<WorkRecord>,templates:List<WorkRecord>,store:WorkspaceStore,export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null,exit:()->Unit={}){
    val vm:WorkshopViewModel=viewModel(factory=modelFactory {WorkshopViewModel(store)})
    val state by vm.state.collectAsStateWithLifecycle();val context=LocalContext.current;val scope=rememberCoroutineScope()
    var base by remember {mutableStateOf<List<MaterialDefinition>>(emptyList())};var loadError by remember {mutableStateOf<String?>(null)}
    var bundledLayouts by remember {mutableStateOf<List<WorkRecord>>(emptyList())}
    var reload by remember {mutableIntStateOf(0)}
    LaunchedEffect(reload){try{val seed=WorkshopSeed(context);base=seed.materials();bundledLayouts=seed.layouts();loadError=null}catch(e:CancellationException){throw e}catch(_:Exception){loadError="No se pudo leer el catálogo de materiales. Tus existencias se conservan; reintenta antes de registrar nombres nuevos."}}
    LaunchedEffect(all){vm.reconcile(all)}
    val catalog=remember(base,all){(base+all.filter {it.kind in setOf("catalog_material","user_material") && !it.archived}.mapNotNull {runCatching {WorkshopCodec.definition(JSONObject(it.json))}.getOrNull()}).associateBy {it.id}.values.toList()}
    var section by rememberSaveable {mutableStateOf("Materiales didácticos")};var selected by rememberSaveable(initialId){mutableStateOf(initialId)}
    var destination by rememberSaveable(initialId){mutableStateOf(if(initialId==null)"catalog"else "guide")};var returnTo by rememberSaveable {mutableStateOf("catalog")}
    var backError by remember {mutableStateOf(false)}
    val holder=rememberSaveableStateHolder()
    fun configured(r:WorkRecord):WorkRecord {
        if(r.kind in setOf("project","experiment"))return r
        val data=JSONObject(r.json);val layout=(all+bundledLayouts).find {it.kind=="catalog_layout" && !it.archived && JSONObject(it.json).optString("id")==data.optString("layoutId")}
        layout?.let {val config=JSONObject(it.json);data.put("visualTemplate",config.optString("template","DIDACTIC"));data.put("sectionOrder",config.optJSONArray("sectionOrder"))}
        return r.copy(json=data.toString())
    }
    val found=selected?.let {id->vm.draft(id) ?: (all+templates).find {it.id==id}}
    val current=found?.let {vm.current(configured(it))}
    fun open(id:String){selected=id;destination="guide"}
    fun begin(source:WorkRecord?=null,experiment:Boolean=false,duplicate:Boolean=false){
        val data=WorkshopCodec.normalize(JSONObject(source?.json ?: "{}"),catalog)
        val previousImage=data.optString("originImageId").ifBlank {data.optString("forkedFromTemplateId")}
        val sourceId=source?.id.orEmpty()
        val origin=if(sourceId.matches(Regex("(?:mat|com|cta|per|arte|tut)-[0-9]{3}")))sourceId else previousImage
        if(origin.isNotBlank())data.put("originImageId",origin)
        if(source!=null)data.put(if(duplicate)"duplicatedFromId"else "forkedFromTemplateId",source.id)
        data.put("status","BORRADOR").put("safetyReviewedByUser",false)
        val steps=data.optJSONArray("steps") ?: org.json.JSONArray();for(i in 0 until steps.length()){steps.optJSONObject(i)?.apply {put("done",false);remove("notes")}}
        data.put("records",org.json.JSONArray()).put("attachments",org.json.JSONArray())
        if(experiment)listOf("results","analysis","limitations","conclusion").forEach {data.put(it,"")}
        if(!data.has("visualTemplate"))data.put("visualTemplate",if(experiment)"EXPERIMENT"else if(source==null)"CREATIVE"else "DIDACTIC")
        val r=WorkRecord(newId(),if(experiment)"experiment"else "project",source?.title.orEmpty()+if(duplicate)" (copia)"else "",data.toString(),System.currentTimeMillis())
        val initialized=if(data.optJSONObject("teacherGuide")==null)r.copy(json=data.put("teacherGuide",pe.profelab.core.content.TeacherGuides.defaultFor(r)).toString())else r
        vm.save(initialized);selected=r.id;destination=if(source!=null && !duplicate)"guide"else "editor"
    }
    fun back(){scope.launch {
        if(!vm.flush()){backError=true;return@launch}
        when(destination){"editor","teacher-editor"->destination="guide";"inventory","shopping"->destination=returnTo;else->{if(initialId!=null && selected==initialId)exit()else {selected=null;destination="catalog"}}}
    }}
    fun utility(name:String){scope.launch {if(vm.flush()){returnTo=destination;destination=name}else backError=true}}
    fun favorite(r:WorkRecord){val id="favorite:${r.id}";if(all.any {it.id==id && !it.archived})vm.archive(id)else vm.save(WorkRecord(id,"favorite",r.title,"{}",System.currentTimeMillis()))}
    BackHandler(enabled=destination!="catalog"){back()}
    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(primary=WorkshopColors.Green,secondary=WorkshopColors.Green,primaryContainer=WorkshopColors.Mint,secondaryContainer=WorkshopColors.Mint,background=WorkshopColors.Cream,surface=WorkshopColors.Cream)){
        Column(Modifier.fillMaxSize()){
            if(state.pending>0)LinearProgressIndicator(Modifier.fillMaxWidth())
            holder.SaveableStateProvider(if(destination in setOf("guide","editor","teacher-editor"))"$destination:${selected.orEmpty()}"else destination){
                when(destination){
                    "inventory"->WorkshopInventory(all,catalog,vm,state,::back)
                    "shopping"->WorkshopShopping(all,catalog,vm,state,::back)
                    "guide"->if(current==null)WorkshopPage("Proyecto",back=::back){item{Text("El recurso no está disponible o todavía se está guardando.")}}else WorkshopGuide(current,all,catalog,vm,::back,{begin(current,current.kind.contains("experiment"))},{destination="editor"},export,capture,{utility("inventory")},{utility("shopping")},{favorite(current)},{destination="teacher-editor"})
                    "editor","teacher-editor"->if(current!=null)WorkshopEditor(current,all,catalog,vm,::back,{begin(current,current.kind=="experiment",true)},capture,initialTab=if(destination=="teacher-editor")"Docente"else "Presentación")
                    else->WorkshopCatalog(all,templates,catalog,state,section,{section=it},::open,{begin(experiment=it)},{utility("inventory")},{utility("shopping")},::favorite,{record,index,value->vm.patch(record){j->val steps=j.optJSONArray("steps") ?: org.json.JSONArray();val step=steps.optJSONObject(index) ?: JSONObject();steps.put(index,JSONObject(step.toString()).put("done",value));j.put("steps",steps);val done=(0 until steps.length()).count {steps.optJSONObject(it)?.optBoolean("done")==true};j.put("status",if(done==steps.length() && done>0)"COMPLETADO"else "EN_PROGRESO")}})
                }
            }
        }
        (state.error ?: loadError)?.let {message->AlertDialog(onDismissRequest={},title={Text("Revisa el guardado del Taller")},text={Text(message)},confirmButton={TextButton(onClick={if(loadError!=null)reload++else vm.retry()}){Text("Reintentar")}},dismissButton={if(loadError==null)TextButton(onClick={vm.clearError()}){Text("Corregir el formulario")}})}
        if(backError)AlertDialog(onDismissRequest={backError=false},title={Text("Hay cambios sin confirmar")},text={Text("Resuelve los avisos de guardado antes de salir. El borrador permanece en esta pantalla.")},confirmButton={TextButton(onClick={vm.retry();backError=false}){Text("Reintentar guardado")}},dismissButton={TextButton(onClick={backError=false}){Text("Seguir editando")}})
    }
}
