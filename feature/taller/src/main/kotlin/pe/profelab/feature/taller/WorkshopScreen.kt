package pe.profelab.feature.taller

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
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
import pe.profelab.core.ui.BindAssistantOcclusion

@Composable
fun WorkshopScreen(all:List<WorkRecord>,templates:List<WorkRecord>,store:WorkspaceStore,export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null,exit:()->Unit={},
    participationContent:@Composable (WorkRecord)->Unit={},observationContent:@Composable (WorkRecord,String?,()->Unit)->Unit={_,_,_->},reduceMotion:Boolean=false){
    val vm:WorkshopViewModel=viewModel(factory=modelFactory {WorkshopViewModel(store)})
    val state by vm.state.collectAsStateWithLifecycle();val context=LocalContext.current;val scope=rememberCoroutineScope()
    var base by remember {mutableStateOf<List<MaterialDefinition>>(emptyList())};var loadError by remember {mutableStateOf<String?>(null)}
    var bundledLayouts by remember {mutableStateOf<List<WorkRecord>>(emptyList())};var reload by remember {mutableIntStateOf(0)}
    LaunchedEffect(reload){try{val seed=WorkshopSeed(context);base=seed.materials();bundledLayouts=seed.layouts();loadError=null}catch(e:CancellationException){throw e}catch(_:Exception){loadError="No se pudo leer el catálogo. Tus existencias se conservan; reintenta antes de registrar materiales."}}
    LaunchedEffect(all,state.pending){vm.reconcile(all)}
    val catalog=remember(base,all){(base+all.filter {it.kind in setOf("catalog_material","user_material") && !it.archived}.mapNotNull {runCatching {WorkshopCodec.definition(JSONObject(it.json))}.getOrNull()}).associateBy {it.id}.values.toList()}
    val definitions=remember(templates,all){(templates+all.filter {it.kind in setOf("catalog_project","catalog_experiment") && !it.archived}).distinctBy {it.id}}
    val mediaRecords=remember(all,definitions){WorkshopBundledMedia.overlays(definitions)+all.filter {it.kind=="catalog_media" && !it.archived}}
    var section by rememberSaveable {mutableStateOf("taller")};var selected by rememberSaveable(initialId){mutableStateOf(initialId)}
    var destination by rememberSaveable(initialId){mutableStateOf(if(initialId==null)"hub"else "detail")}
    var trail by rememberSaveable(initialId){mutableStateOf("[]")}
    var mode by rememberSaveable(selected){mutableStateOf(WorkshopPerspective.PRACTICAL)}
    var requestedGuideMode by remember {mutableStateOf<String?>(null)};var recommendationRequest by remember {mutableStateOf(false)}
    var backError by remember {mutableStateOf(false)};val holder=rememberSaveableStateHolder()
    var help by remember {mutableStateOf<String?>(null)}
    var observing by remember {mutableStateOf(false)};var observingStep by remember {mutableStateOf<String?>(null)}
    var assistantStep by remember {mutableStateOf<String?>(null)}
    val motionEnabled=rememberWorkshopMotionEnabled() && !reduceMotion
    var routeBusy by remember {mutableStateOf(motionEnabled)}
    var backInFlight by remember {mutableStateOf(false)}
    var initialMotion by remember {mutableStateOf(true)}
    var movingBackward by remember {mutableStateOf(false)}
    fun configured(r:WorkRecord):WorkRecord {
        if(r.kind in setOf("project","experiment"))return r
        val data=JSONObject(r.json);val layout=(all+bundledLayouts).find {it.kind=="catalog_layout" && !it.archived && JSONObject(it.json).optString("id")==data.optString("layoutId")}
        layout?.let {val config=JSONObject(it.json);data.put("visualTemplate",config.optString("template","DIDACTIC")).put("sectionOrder",config.optJSONArray("sectionOrder"))}
        return r.copy(json=data.toString())
    }
    val found=selected?.let {id->vm.draft(id) ?: (all+templates).find {it.id==id && !it.archived}}
    val current=found?.let {vm.current(configured(it))};val view=current?.let {EducationProgress.view(it,all)}
    BindAssistantOcclusion(observing || help!=null || backError || state.error!=null || loadError!=null)
    fun observe(stepId:String?){scope.launch {if(vm.flush()){observingStep=stepId;observing=true}else backError=true}}
    LaunchedEffect(selected,current!=null){mode=current?.let {WorkshopFlowAdapter.read(it).perspective} ?: WorkshopPerspective.PRACTICAL}
    fun go(target:String,id:String?=selected,sector:String=section){
        if(routeBusy || backInFlight)return
        if(target==destination && id==selected && sector==section)return
        val old=JSONArray(trail);val a=JSONArray();for(i in maxOf(0,old.length()-99) until old.length())a.put(old.get(i));a.put(JSONObject().put("destination",destination).put("selected",selected ?: JSONObject.NULL).put("section",section));trail=a.toString()
        initialMotion=false;movingBackward=false;routeBusy=motionEnabled
        selected=id;section=sector;destination=target
    }
    fun window(w:WorkshopWindow){go(when(w){WorkshopWindow.DETAIL->"detail";WorkshopWindow.MATERIALS->"materials";WorkshopWindow.PURPOSE->"purpose";WorkshopWindow.STEPS->"steps";WorkshopWindow.RESULTS->"results";WorkshopWindow.CLASSROOM->"classroom"})}
    fun back(){
        if(routeBusy || backInFlight)return
        backInFlight=true
        scope.launch {try {
        if(!vm.flush()){backError=true;return@launch}
        val a=JSONArray(trail)
        initialMotion=false;movingBackward=true;routeBusy=motionEnabled
        if(a.length()>0){val last=a.getJSONObject(a.length()-1);val rest=JSONArray();for(i in 0 until a.length()-1)rest.put(a.get(i));trail=rest.toString();destination=last.getString("destination");selected=last.optString("selected").takeIf {!last.isNull("selected")};section=last.getString("section")}
        else if(initialId!=null)exit()else {selected=null;destination="hub"}
        }finally {backInFlight=false}}
    }
    fun utility(name:String){scope.launch {if(vm.flush())go(name)else backError=true}}
    fun open(id:String){go("detail",id)}
    fun begin(source:WorkRecord?=null,experiment:Boolean=false,duplicate:Boolean=false){
        if(vm.state.value.pending>0 || routeBusy || backInFlight)return
        val data=ActivityRunData.freshPayload(WorkshopCodec.normalize(JSONObject(source?.json ?: "{}"),catalog))
        val previousImage=data.optString("originImageId").ifBlank {data.optString("forkedFromTemplateId")}
        val sourceId=source?.id.orEmpty();val origin=if(sourceId.matches(Regex("(?:mat|com|cta|per|arte|tut)-[0-9]{3}")))sourceId else previousImage
        if(origin.isNotBlank())data.put("originImageId",origin)
        if(source!=null){data.put(if(duplicate)"duplicatedFromId"else "forkedFromTemplateId",source.id);data.put("activityId",WorkshopFlowAdapter.read(source).activityId)}
        if(!data.has("visualTemplate"))data.put("visualTemplate",if(experiment)"EXPERIMENT"else if(source==null)"CREATIVE"else "DIDACTIC")
        val r=WorkRecord(newId(),if(experiment)"experiment"else "project",source?.title.orEmpty()+if(duplicate)" (copia)"else "",data.toString(),System.currentTimeMillis())
        val initialized=if(data.optJSONObject("teacherGuide")==null)r.copy(json=data.put("teacherGuide",TeacherGuides.defaultFor(r)).toString())else r
        vm.save(initialized);go(ActivityRunData.initialDestination(source!=null,duplicate,destination),r.id)
    }
    fun favorite(r:WorkRecord){val id="favorite:${r.id}";if(all.any {it.id==id && !it.archived})vm.archive(id)else vm.save(WorkRecord(id,"favorite",r.title,"{}",System.currentTimeMillis()))}
    fun perspective(p:WorkshopPerspective){if(routeBusy || backInFlight || mode==p)return;mode=p;current?.takeIf {view?.personal==true}?.let {r->vm.patch(r){j->val flow=j.optJSONObject("workshopFlow") ?: JSONObject();j.put("workshopFlow",flow.put("version",1).put("perspective",p.name))}}}
    fun advanced(){recommendationRequest=false;utility("advanced")}
    val assistantHandlers=mutableMapOf<AssistantAction,()->Unit>(
        AssistantAction.EXPLORE_WORKSHOP to {scope.launch {if(vm.flush())go("hub",null)else backError=true}},
        AssistantAction.INVENTORY to {utility("inventory")},AssistantAction.SHOPPING to {utility("shopping")},
        AssistantAction.RECOMMENDATIONS to {recommendationRequest=true;utility("advanced")},
        AssistantAction.EXPERIMENTS to {scope.launch {if(vm.flush())go("catalog",null,"experiments")else backError=true}}
    )
    if(current!=null && destination=="steps"){
        assistantHandlers[AssistantAction.STEP_HELP]={help="step"}
        assistantHandlers[AssistantAction.SAFETY]={help="safety"}
    }
    if(current!=null && destination in setOf("detail","materials","purpose","steps","results","classroom","guide"))assistantHandlers[AssistantAction.TEACHER]={scope.launch {if(vm.flush()){perspective(WorkshopPerspective.TEACHER);window(WorkshopWindow.CLASSROOM)}else backError=true}}
    BindAssistant("taller",assistantHandlers)
    BackHandler(enabled=destination!="hub"){back()}
    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(primary=WorkshopColors.Green,secondary=WorkshopColors.Green,primaryContainer=WorkshopColors.Mint,secondaryContainer=WorkshopColors.Mint,background=WorkshopColors.Cream,surface=WorkshopColors.Cream)){
        CompositionLocalProvider(LocalWorkshopMediaRecords provides mediaRecords,LocalWorkshopMotionEnabled provides motionEnabled,LocalWorkshopTransitionBusy provides routeBusy){Column(Modifier.fillMaxSize()){
            if(state.pending>0)LinearProgressIndicator(Modifier.fillMaxWidth())
            WorkshopRouteMotion("$destination:${selected.orEmpty()}:$section",destination,initialMotion,movingBackward,motionEnabled,{routeBusy=false}){
            val executionPage=if(destination=="steps" && current!=null)EducationData.selectedRun(current,all)?.id.orEmpty()else ""
            holder.SaveableStateProvider("$destination:${selected.orEmpty()}:$section:$executionPage"){
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
                        "materials"->MaterialsChecklist(current,all,catalog,view,vm,::back,actions,participationContent)
                        "purpose"->PurposePanel(current,view,mode,::back,actions)
                        "steps"->GuidedStepPager(current,view,vm,::back,actions,::observe,{assistantStep=it})
                        "results"->ActivityResultsScreen(current,view,vm,::back,actions,capture,{observe(null)})
                        "classroom"->TeacherClassroomPanel(current,view,vm,::back,actions,{observe(null)})
                        "guide"->WorkshopGuide(current,all,catalog,vm,::back,actions.begin,actions.edit,export,capture,actions.inventory,actions.shopping,actions.favorite,actions.editTeacher,requestedMode=requestedGuideMode,modeConsumed={requestedGuideMode=null})
                        else->ActivityDetail(current,all,catalog,view,mode,::back,actions)
                    }
                }
            }
            }
        }}
        (state.error ?: loadError)?.let {message->AlertDialog(onDismissRequest={},title={Text("Revisa el guardado del Taller")},text={Text(message)},confirmButton={TextButton(onClick={if(loadError!=null)reload++else vm.retry()}){Text("Reintentar")}},dismissButton={if(loadError==null)TextButton(onClick={vm.clearError()}){Text("Corregir el formulario")}})}
        if(backError)AlertDialog(onDismissRequest={backError=false},title={Text("Hay cambios sin confirmar")},text={Text("Resuelve los avisos de guardado antes de salir. El borrador permanece en esta pantalla.")},confirmButton={TextButton(onClick={vm.retry();backError=false}){Text("Reintentar guardado")}},dismissButton={TextButton(onClick={backError=false}){Text("Seguir editando")}})
        if(observing && current!=null)Dialog(onDismissRequest={observing=false}){
            Surface(shape=MaterialTheme.shapes.large){Column(Modifier.padding(16.dp)){
                Text("Observación y seguimiento",style=MaterialTheme.typography.titleLarge)
                observationContent(current,observingStep){observing=false}
            }}
        }
        help?.let {kind->AlertDialog(onDismissRequest={help=null},title={Text(if(kind=="step")"Ayuda sobre este paso"else "Seguridad y recomendaciones")},text={
            val j=current?.let {JSONObject(it.json)} ?: JSONObject()
            Column {
                if(kind=="step"){
                    val last=assistantStep ?: view?.lastVisitedStepId
                    val step=view?.steps?.firstOrNull {it.id==last} ?: view?.steps?.getOrNull(view?.resumeIndex ?: 0)
                    Text(step?.title.orEmpty());step?.instruction?.takeIf {it.isNotBlank()}?.let {Text(it)}
                    Text("Puedes consultar la guía docente o anotar lo que realmente observas; avanzar no completa el paso ni consume material.")
                }else{WorkshopCodec.strings(j.optJSONObject("safety")?.opt("warnings")).forEach {Text(it)};Text("Comprueba materiales reales, supervisión y adecuación al grupo antes de continuar.")}
            }
        },confirmButton={TextButton(onClick={help=null}){Text("Continuar")}})}
    }
}
