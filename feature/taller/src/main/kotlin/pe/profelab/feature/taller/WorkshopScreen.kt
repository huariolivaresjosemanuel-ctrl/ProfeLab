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
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*
import pe.profelab.core.ui.modelFactory
import pe.profelab.core.ui.BindAssistant

@Composable
fun WorkshopScreen(all:List<WorkRecord>,templates:List<WorkRecord>,store:WorkspaceStore,export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null,exit:()->Unit={}){
    val vm:WorkshopViewModel=viewModel(factory=modelFactory {WorkshopViewModel(store)})
    val state by vm.state.collectAsStateWithLifecycle();val context=LocalContext.current;val scope=rememberCoroutineScope()
    var base by remember {mutableStateOf<List<MaterialDefinition>>(emptyList())};var loadError by remember {mutableStateOf<String?>(null)}
    var bundledLayouts by remember {mutableStateOf<List<WorkRecord>>(emptyList())};var reload by remember {mutableIntStateOf(0)}
    LaunchedEffect(reload){try{val seed=WorkshopSeed(context);base=seed.materials();bundledLayouts=seed.layouts();loadError=null}catch(e:CancellationException){throw e}catch(_:Exception){loadError="No se pudo leer el catálogo. Tus existencias se conservan; reintenta antes de registrar materiales."}}
    LaunchedEffect(all){vm.reconcile(all)}
    val catalog=remember(base,all){(base+all.filter {it.kind in setOf("catalog_material","user_material") && !it.archived}.mapNotNull {runCatching {WorkshopCodec.definition(JSONObject(it.json))}.getOrNull()}).associateBy {it.id}.values.toList()}
    val definitions=remember(templates,all){(templates+all.filter {it.kind in setOf("catalog_project","catalog_experiment") && !it.archived}).distinctBy {it.id}}
    var section by rememberSaveable {mutableStateOf("taller")};var selected by rememberSaveable(initialId){mutableStateOf(initialId)}
    var destination by rememberSaveable(initialId){mutableStateOf(if(initialId==null)"hub"else "detail")}
    var trail by rememberSaveable(initialId){mutableStateOf("[]")}
    var mode by rememberSaveable(selected){mutableStateOf(WorkshopPerspective.PRACTICAL)}
    var requestedGuideMode by remember {mutableStateOf<String?>(null)};var recommendationRequest by remember {mutableStateOf(false)}
    var backError by remember {mutableStateOf(false)};val holder=rememberSaveableStateHolder()
    fun configured(r:WorkRecord):WorkRecord {
        if(r.kind in setOf("project","experiment"))return r
        val data=JSONObject(r.json);val layout=(all+bundledLayouts).find {it.kind=="catalog_layout" && !it.archived && JSONObject(it.json).optString("id")==data.optString("layoutId")}
        layout?.let {val config=JSONObject(it.json);data.put("visualTemplate",config.optString("template","DIDACTIC")).put("sectionOrder",config.optJSONArray("sectionOrder"))}
        return r.copy(json=data.toString())
    }
    val found=selected?.let {id->vm.draft(id) ?: (all+templates).find {it.id==id && !it.archived}}
    val current=found?.let {vm.current(configured(it))};val view=current?.let(WorkshopFlowAdapter::read)
    LaunchedEffect(selected,current!=null){mode=current?.let {WorkshopFlowAdapter.read(it).perspective} ?: WorkshopPerspective.PRACTICAL}
    fun go(target:String,id:String?=selected,sector:String=section){
        if(target==destination && id==selected && sector==section)return
        val old=JSONArray(trail);val a=JSONArray();for(i in maxOf(0,old.length()-99) until old.length())a.put(old.get(i));a.put(JSONObject().put("destination",destination).put("selected",selected ?: JSONObject.NULL).put("section",section));trail=a.toString()
        selected=id;section=sector;destination=target
    }
    fun window(w:WorkshopWindow){go(when(w){WorkshopWindow.DETAIL->"detail";WorkshopWindow.MATERIALS->"materials";WorkshopWindow.PURPOSE->"purpose";WorkshopWindow.STEPS->"steps";WorkshopWindow.CLASSROOM->"classroom"})}
    fun back(){scope.launch {
        if(!vm.flush()){backError=true;return@launch}
        val a=JSONArray(trail)
        if(a.length()>0){val last=a.getJSONObject(a.length()-1);val rest=JSONArray();for(i in 0 until a.length()-1)rest.put(a.get(i));trail=rest.toString();destination=last.getString("destination");selected=last.optString("selected").takeIf {!last.isNull("selected")};section=last.getString("section")}
        else if(initialId!=null)exit()else {selected=null;destination="hub"}
    }}
    fun utility(name:String){scope.launch {if(vm.flush())go(name)else backError=true}}
    fun open(id:String){go("detail",id)}
    fun begin(source:WorkRecord?=null,experiment:Boolean=false,duplicate:Boolean=false){
        if(vm.state.value.pending>0)return
        val data=WorkshopCodec.normalize(JSONObject(source?.json ?: "{}"),catalog)
        val previousImage=data.optString("originImageId").ifBlank {data.optString("forkedFromTemplateId")}
        val sourceId=source?.id.orEmpty();val origin=if(sourceId.matches(Regex("(?:mat|com|cta|per|arte|tut)-[0-9]{3}")))sourceId else previousImage
        if(origin.isNotBlank())data.put("originImageId",origin)
        if(source!=null){data.put(if(duplicate)"duplicatedFromId"else "forkedFromTemplateId",source.id);data.put("activityId",WorkshopFlowAdapter.read(source).activityId)}
        data.remove("workshopFlow");data.remove("classroomAssociation")
        data.put("status","BORRADOR").put("safetyReviewedByUser",false)
        val steps=data.optJSONArray("steps") ?: JSONArray();for(i in 0 until steps.length())steps.optJSONObject(i)?.apply {put("done",false);remove("notes")}
        data.put("records",JSONArray()).put("attachments",JSONArray())
        if(experiment)listOf("results","analysis","limitations","conclusion").forEach {data.put(it,"")}
        if(!data.has("visualTemplate"))data.put("visualTemplate",if(experiment)"EXPERIMENT"else if(source==null)"CREATIVE"else "DIDACTIC")
        val r=WorkRecord(newId(),if(experiment)"experiment"else "project",source?.title.orEmpty()+if(duplicate)" (copia)"else "",data.toString(),System.currentTimeMillis())
        val initialized=if(data.optJSONObject("teacherGuide")==null)r.copy(json=data.put("teacherGuide",TeacherGuides.defaultFor(r)).toString())else r
        vm.save(initialized);go(if(source!=null && !duplicate)"materials"else "editor",r.id)
    }
    fun favorite(r:WorkRecord){val id="favorite:${r.id}";if(all.any {it.id==id && !it.archived})vm.archive(id)else vm.save(WorkRecord(id,"favorite",r.title,"{}",System.currentTimeMillis()))}
    fun perspective(p:WorkshopPerspective){mode=p;current?.takeIf {view?.personal==true}?.let {r->vm.patch(r){j->val flow=j.optJSONObject("workshopFlow") ?: JSONObject();j.put("workshopFlow",flow.put("version",1).put("perspective",p.name))}}}
    fun advanced(){recommendationRequest=false;utility("advanced")}
    val assistantHandlers=mutableMapOf<AssistantAction,()->Unit>(
        AssistantAction.INVENTORY to {utility("inventory")},AssistantAction.SHOPPING to {utility("shopping")},
        AssistantAction.RECOMMENDATIONS to {recommendationRequest=true;utility("advanced")},
        AssistantAction.EXPERIMENTS to {scope.launch {if(vm.flush())go("catalog",null,"experiments")else backError=true}}
    )
    if(current!=null && destination in setOf("detail","materials","purpose","steps","classroom","guide"))assistantHandlers[AssistantAction.TEACHER]={scope.launch {if(vm.flush()){perspective(WorkshopPerspective.TEACHER);window(WorkshopWindow.CLASSROOM)}else backError=true}}
    BindAssistant("taller",assistantHandlers)
    BackHandler(enabled=destination!="hub"){back()}
    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(primary=WorkshopColors.Green,secondary=WorkshopColors.Green,primaryContainer=WorkshopColors.Mint,secondaryContainer=WorkshopColors.Mint,background=WorkshopColors.Cream,surface=WorkshopColors.Cream)){
        Column(Modifier.fillMaxSize()){
            if(state.pending>0)LinearProgressIndicator(Modifier.fillMaxWidth())
            holder.SaveableStateProvider("$destination:${selected.orEmpty()}:$section"){
                val actions=current?.let {r->ActivityActions(::window,{begin(r,r.kind.contains("experiment"))},{window(if(view?.lastVisitedStepId!=null || (view?.completedSteps ?: 0)>0)WorkshopWindow.STEPS else WorkshopWindow.MATERIALS)},
                    {go("editor")},{go("guide")},{go("teacher-editor")},{utility("inventory")},{utility("shopping")},{favorite(r)},
                    {scope.launch {if(vm.flush()){val latest=vm.current(r);export(latest.copy(json=WorkshopCodec.normalize(JSONObject(latest.json),catalog).put("teacherGuide",TeacherGuides.resolve(latest)).toString()))}else backError=true}},::perspective)}
                when(destination){
                    "hub"->WorkshopHub(all,definitions,{go("catalog",null,it)},::open,{utility("inventory")},{utility("shopping")},::advanced,{begin()})
                    "catalog"->SectorCatalog(section,all,definitions,::open,::back,::favorite,::advanced,{begin(experiment=it)})
                    "advanced"->WorkshopCatalog(all,templates,catalog,state,when(section){"experiments"->"Experimentos";"ideas"->"Mis ideas";else->"Materiales didácticos"},{section=when(it){"Experimentos"->"experiments";"Mis ideas"->"ideas";else->"taller"}},::open,{begin(experiment=it)},{utility("inventory")},{utility("shopping")},::favorite,{r,index,value->val v=WorkshopFlowAdapter.read(r);v.steps.getOrNull(index)?.let {vm.completeStep(r,it.id,value)}},focusRecommendations=recommendationRequest,recommendationsConsumed={recommendationRequest=false})
                    "inventory"->WorkshopInventory(all,catalog,vm,state,::back)
                    "shopping"->WorkshopShopping(all,catalog,vm,state,::back)
                    "editor","teacher-editor"->if(current!=null)WorkshopEditor(current,all,catalog,vm,::back,{begin(current,current.kind=="experiment",true)},capture,initialTab=if(destination=="teacher-editor")"Docente"else "Presentación")
                    else->if(current==null || view==null || actions==null)WorkshopPage("Proyecto",back=::back){item{Text("El recurso no está disponible o todavía se está guardando.")}}
                    else when(destination){
                        "materials"->MaterialsChecklist(current,all,catalog,view,vm,::back,actions)
                        "purpose"->PurposePanel(current,view,mode,::back,actions)
                        "steps"->GuidedStepPager(current,view,vm,::back,actions)
                        "classroom"->TeacherClassroomPanel(current,view,vm,::back,actions)
                        "guide"->WorkshopGuide(current,all,catalog,vm,::back,actions.begin,actions.edit,export,capture,actions.inventory,actions.shopping,actions.favorite,actions.editTeacher,requestedMode=requestedGuideMode,modeConsumed={requestedGuideMode=null})
                        else->ActivityDetail(current,all,catalog,view,mode,::back,actions)
                    }
                }
            }
        }
        (state.error ?: loadError)?.let {message->AlertDialog(onDismissRequest={},title={Text("Revisa el guardado del Taller")},text={Text(message)},confirmButton={TextButton(onClick={if(loadError!=null)reload++else vm.retry()}){Text("Reintentar")}},dismissButton={if(loadError==null)TextButton(onClick={vm.clearError()}){Text("Corregir el formulario")}})}
        if(backError)AlertDialog(onDismissRequest={backError=false},title={Text("Hay cambios sin confirmar")},text={Text("Resuelve los avisos de guardado antes de salir. El borrador permanece en esta pantalla.")},confirmButton={TextButton(onClick={vm.retry();backError=false}){Text("Reintentar guardado")}},dismissButton={TextButton(onClick={backError=false}){Text("Seguir editando")}})
    }
}
