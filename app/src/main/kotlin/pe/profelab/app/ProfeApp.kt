package pe.profelab.app

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import android.graphics.Bitmap
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import pe.profelab.feature.taller.WorkshopScreen
import pe.profelab.feature.sessions.SessionWorkspace
import pe.profelab.feature.notebook.NotebookWorkspace
import pe.profelab.feature.library.LibraryWorkspace
import pe.profelab.feature.pdf.PdfReaderScreen
import pe.profelab.feature.capture.CaptureScreen
import pe.profelab.feature.ocr.OcrScreen
import pe.profelab.feature.ocr.PdfOcrBatchScreen
import pe.profelab.feature.pdf.PdfDocumentHandle
import pe.profelab.core.files.PrivateFiles
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import pe.profelab.core.model.*
import pe.profelab.core.security.ContentPolicy
import pe.profelab.core.ui.*
import pe.profelab.feature.home.*
import pe.profelab.feature.library.*
import pe.profelab.feature.profile.*

@Composable
fun ProfeApp(graph: AppGraph) {
    val context=LocalContext.current
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route
    val home: HomeViewModel=viewModel(factory=modelFactory { HomeViewModel(graph.repository,graph.repository,graph.repository) })
    val homeState by home.state.collectAsStateWithLifecycle()
    var preferencesError by remember { mutableStateOf<String?>(null) }
    val preferencesFlow=remember(graph) { graph.preferences.preferences.catch {
        preferencesError="No se pudieron leer las preferencias. Se muestran los valores iniciales."
        emit(UserPreferences())
    } }
    val prefs by preferencesFlow.collectAsStateWithLifecycle(initialValue=UserPreferences())
    val reminderWarning by graph.reminderWarning.collectAsStateWithLifecycle()
    val message by home.message.collectAsStateWithLifecycle()
    val snackbar=remember { SnackbarHostState() }
    val scope=rememberCoroutineScope()
    val retainedOcrWarning by graph.ocrDrafts.warning.collectAsStateWithLifecycle()
    LaunchedEffect(retainedOcrWarning){retainedOcrWarning?.let {snackbar.showSnackbar(it)}}
    val workspace: WorkspaceViewModel=viewModel(factory=modelFactory {WorkspaceViewModel(graph.workspace)})
    val records by workspace.records.collectAsStateWithLifecycle()
    var personalizationReady by remember {mutableStateOf(false)}
    var registrationBusy by remember {mutableStateOf(false)}
    var registrationError by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(homeState.loading,homeState.profile?.id){if(!homeState.loading){
        try {graph.transfers.recoverPendingPreferences();graph.preferences.migrateLegacyProfile(homeState.profile);personalizationReady=true}
        catch(e:CancellationException){throw e}catch(_:Exception){preferencesError="No se pudo recuperar la personalización. Tus trabajos se conservan."}
    }}
    val workspaceError by workspace.error.collectAsStateWithLifecycle()
    var templates by remember {mutableStateOf<List<WorkRecord>>(emptyList())}
    var sources by remember {mutableStateOf<List<SourceReference>>(emptyList())}
    var seedError by remember {mutableStateOf<String?>(null)}
    var pendingPdf by rememberSaveable {mutableStateOf<String?>(null)}
    var pendingSource by rememberSaveable {mutableStateOf<String?>(null)}
    var temporaryPdf by remember {mutableStateOf<WorkRecord?>(null)}
    var exportFile by remember {mutableStateOf<File?>(null)}
    var ocrUri by rememberSaveable {mutableStateOf<String?>(null)}
    var ocrDestination by rememberSaveable {mutableStateOf<String?>(null)}
    var ocrDocumentId by rememberSaveable {mutableStateOf<String?>(null)};var ocrPageIndex by rememberSaveable {mutableIntStateOf(0)}
    var batchDocument by rememberSaveable {mutableStateOf<String?>(null)};var batchPassword by remember {mutableStateOf("")};var batchCount by rememberSaveable {mutableIntStateOf(0)}
    val incoming by graph.incomingPdf.collectAsStateWithLifecycle()
    LaunchedEffect(Unit){try {templates=graph.seeds.templates();sources=graph.catalog.sources()}catch(e:CancellationException){throw e}catch(_:Exception){seedError="No se pudo abrir el contenido incluido en la APK."}}
    LaunchedEffect(incoming){incoming?.let {pendingPdf=it;pendingSource=null;graph.incomingPdf.value=null}}
    fun capture(id:String){ocrDocumentId=null;ocrDestination=id.takeIf {it.isNotBlank()};nav.navigate("capture")}
    fun openPdfAt(record:WorkRecord,initialPage:Int){scope.launch {
        try {
            if(record.kind=="document_temp")temporaryPdf=record
            else {
                val data=JSONObject(record.json)
                val actual=withContext(Dispatchers.IO){graph.files.file(data.getString("fileId")).inputStream().use {input->val md=java.security.MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;md.update(buffer,0,n)};md.digest().joinToString(""){"%02x".format(it)}}}
                require(actual==data.getString("sha256")){"El documento cambió"}
            }
            nav.navigate("pdf/${Uri.encode(record.id)}?start=$initialPage")
        }catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("El PDF falta o cambió. Reimpórtalo; las marcas anteriores se conservan por ID y hash.")}
    }}
    fun openPdf(record:WorkRecord){openPdfAt(record,-1)}
    fun preview(file:File){exportFile=file;nav.navigate("export-preview")}
    fun renderWork(record:WorkRecord){scope.launch {
        try {
            fun render(r:WorkRecord):String {
                return pe.profelab.core.files.WorkReport.render(r,records.associate {it.id to it.title})
            }
            val j=JSONObject(record.json);val linked=if(record.kind=="portfolio"){val a=j.optJSONArray("links") ?: JSONArray();(0 until a.length()).mapNotNull {i->records.find {it.id==a.getString(i)}}}else emptyList()
            if(record.kind=="session"){
                val minutes=listOf("startMinutes","developmentMinutes","closeMinutes").map {j.optString(it).toIntOrNull()}
                val total=j.optString("totalMinutes").toIntOrNull()
                if(total==null || total<=0 || minutes.any {it==null || it<0} || minutes.filterNotNull().sum()!=total){snackbar.showSnackbar("Corrige los minutos de la secuencia antes de exportar. El borrador se conserva.");return@launch}
            }
            val images=if(j.optBoolean("exportImages"))(listOf(record)+linked).flatMap {r->val a=JSONObject(r.json).optJSONArray("attachments") ?: JSONArray();(0 until a.length()).mapNotNull {i->val item=a.getJSONObject(i);if(item.optString("mime").startsWith("image/"))graph.files.file(item.getString("id")) else null}} else emptyList()
            preview(graph.files.exportPdf(record.title,render(record)+linked.joinToString("\n\n",prefix="\n\n"){render(it)},images))
        }catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("No se pudo generar el PDF. Tu trabajo se conserva.")}
    }}
    fun saveDocument(uri:String,source:String?=pendingSource){scope.launch {
        try {
            val parsed=Uri.parse(uri)
            val copied=if(parsed.scheme=="file") {
                val file=File(requireNotNull(parsed.path));require(file.canonicalPath.startsWith(context.cacheDir.canonicalPath+File.separator))
                PrivateFiles(context).copyFrom(file.inputStream(),100L*1024*1024)
            }else graph.files.importUri(uri,"application/pdf")
            val existing=records.find {it.kind=="document" && !it.archived && JSONObject(it.json).optString("sha256")==copied.sha256}
            if(existing!=null){
                val data=JSONObject(existing.json)
                if(!graph.files.file(data.optString("fileId")).exists()){
                    val repaired=existing.copy(json=data.put("fileId",copied.id).toString());graph.workspace.saveRecord(repaired);openPdf(repaired)
                }else {copied.file.delete();openPdf(existing)}
            } else {
                val title=sources.find {it.id==source}?.title ?: runCatching {context.contentResolver.query(parsed,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use {c->if(c.moveToFirst())c.getString(0)else null}}.getOrNull() ?: "PDF importado"
                val r=WorkRecord(newId(),"document",title,JSONObject().put("fileId",copied.id).put("sha256",copied.sha256).put("sourceId",source).put("lastPage",0).toString(),System.currentTimeMillis())
                graph.workspace.saveRecord(r);openPdf(r)
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("No se pudo importar. Revisa formato PDF, permisos y espacio disponible.")}
    }}
    val importPdf=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let {pendingPdf=it.toString()}}
    fun requestImport(source:String?){pendingSource=source;importPdf.launch(arrayOf("application/pdf"))}
    fun recordUri(record:WorkRecord):Uri {val j=JSONObject(record.json);return if(j.has("fileId"))Uri.fromFile(graph.files.file(j.getString("fileId"))) else Uri.parse(j.getString("uri"))}
    fun ocrPage(bitmap:Bitmap){val f=File(context.cacheDir,"ocr-${newId()}.png");f.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};ocrUri=Uri.fromFile(f).toString();nav.navigate("ocr")}
    fun openRecord(ref:EntityRef){if(ref.kind==EntityKind.TASK)nav.navigate("task/${Uri.encode(ref.id)}") else if(ref.kind==EntityKind.PROFILE)nav.navigate("profile") else nav.navigate("record/${Uri.encode(ref.id)}")}
    fun top(destination: String) {
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) { saveState=true }
            launchSingleTop=true; restoreState=true
        }
    }
    fun tasks(filter: TaskFilter) { nav.navigate("tasks/${filter.name}") { launchSingleTop=true } }
    fun edit(id: String) { nav.navigate("task/${Uri.encode(id)}") }
    fun back() { nav.popBackStack() }
    fun complete(task: Task, value: Boolean) {
        scope.launch {
            if (!home.complete(task,value)) return@launch
            if(snackbar.showSnackbar(if(value) "Tarea completada" else "Tarea reabierta",actionLabel="Deshacer",duration=SnackbarDuration.Short)==SnackbarResult.ActionPerformed) home.complete(task,!value)
        }
    }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); home.dismissMessage() } }
    LaunchedEffect(preferencesError) { preferencesError?.let { snackbar.showSnackbar(it) } }
    CompositionLocalProvider(LocalOpenResource provides {id->val r=records.find {it.id==id};if(r?.kind=="document")openPdf(r)else nav.navigate("record/${Uri.encode(id)}")},LocalOpenDocumentPage provides {id,index->val r=records.find {it.id==id};if(r?.kind=="document")openPdfAt(r,index)else scope.launch {snackbar.showSnackbar("El documento de la cita no está disponible.")}}) {
    Scaffold(
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            Surface {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=16.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    BookMark(Modifier.size(32.dp)); Text("ProfeLab",style=MaterialTheme.typography.titleLarge,color=ProfeColors.Green,modifier=Modifier.weight(1f))
                    IconButton(onClick={tasks(TaskFilter.REMINDERS)}) { Icon(Icons.Outlined.NotificationsNone,"Avisos locales") }
                    TextButton(onClick={nav.navigate("profile") { launchSingleTop=true }}) { Text(homeState.profile?.name?.trim()?.take(1)?.uppercase()?.takeIf { it.isNotBlank() } ?: "Perfil") }
                }
            }
        },
        bottomBar={if(route in topDestinations.map { it.route } || route=="practices") ProfeNavigationBar(route,::top)},
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            NavHost(navController=nav,startDestination="home") {
                composable("home") { HomeScreen(homeState,::top,::tasks,::edit,{nav.navigate("activity")},{nav.navigate("profile")},::complete,home::retry,::openRecord,prefs){
                    if(personalizationReady && !prefs.onboardingCompleted)LocalWelcomeCard(prefs,registrationBusy,registrationError){name,role->
                        registrationBusy=true;registrationError=null;scope.launch {
                            try {graph.preferences.setPersonalization(name,role)}
                            catch(e:CancellationException){throw e}catch(_:Exception){registrationError="No se pudo guardar. Puedes reintentar o seguir usando las pestañas."}
                            finally{registrationBusy=false}
                        }
                    }
                } }
                composable("taller") { WorkshopScreen(records,templates,graph.workspace,::renderWork,::capture) }
                composable("sessions") { SessionWorkspace(records,workspace::save,workspace::archive,::renderWork,::capture) }
                composable("notebook") { NotebookWorkspace(records,workspace::save,workspace::archive,::renderWork,::capture) }
                composable("practices") { NotebookWorkspace(records,workspace::save,workspace::archive,::renderWork,::capture,initialSection="Prácticas") }
                composable("library") {
                    LibraryWorkspace(sources,records,workspace::save,workspace::archive,::requestImport,::openPdf,{ url ->
                        try {
                            require(ContentPolicy.isOfficialSource(url))
                            context.startActivity(Intent(Intent.ACTION_VIEW,url.toUri()))
                        } catch (_: Exception) { scope.launch { snackbar.showSnackbar("No se pudo abrir la fuente. Comprueba que tienes un navegador disponible.") } }
                    },{capture("")},{r->runCatching {graph.files.file(JSONObject(r.json).getString("fileId")).isFile}.getOrDefault(false)})
                }
                composable("tasks/{filter}",arguments=listOf(navArgument("filter") { type=NavType.StringType })) { e ->
                    val filter=runCatching { TaskFilter.valueOf(e.arguments?.getString("filter").orEmpty()) }.getOrDefault(TaskFilter.ALL)
                    Column {
                        reminderWarning?.let { ErrorNotice(it,graph::refreshReminders) }
                        Box(Modifier.weight(1f)) { TaskListScreen(homeState,filter,prefs.showCompleted,{edit("new")},::edit,::complete,::back) }
                    }
                }
                composable("task/{id}",arguments=listOf(navArgument("id") { type=NavType.StringType })) { e ->
                    val id=requireNotNull(e.arguments?.getString("id"))
                    val vm: TaskEditorViewModel=viewModel(factory=modelFactory { TaskEditorViewModel(id,graph.repository,graph.repository) })
                    val state by vm.state.collectAsStateWithLifecycle()
                    TaskEditorScreen(vm,state,id=="new",::back)
                }
                composable("activity") { ActivityScreen(homeState,::openRecord,::back) }
                composable("profile") {
                    val vm: ProfileViewModel=viewModel(factory=modelFactory { ProfileViewModel(graph.repository,graph.repository,graph.preferences) })
                    val state by vm.state.collectAsStateWithLifecycle()
                    val settings by vm.settings.collectAsStateWithLifecycle()
                    ProfileScreen(vm,state,settings,::back,{nav.navigate("about")},{nav.navigate("workspace-tools")}){InstallationCard()}
                }
                composable("about") { AboutScreen(::back) }
                composable("workspace-tools") {WorkspaceTools(graph,records,workspace::save,::back){scope.launch {try {val d=graph.ocrDrafts.readDraft("ocr-review");if(d==null)snackbar.showSnackbar("No hay borrador OCR guardado.")else {val j=JSONObject(d.payload);ocrUri=j.getString("uri");ocrDestination=j.optString("owner").takeIf {it.isNotBlank()};ocrDocumentId=j.optString("documentId").takeIf {it.isNotBlank()};ocrPageIndex=j.optInt("pageIndex");nav.navigate("ocr")}}catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("No se pudo recuperar el borrador OCR; se conserva el guardado previo.")}}}}
                composable("record/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){e->
                    val id=e.arguments?.getString("id");val r=records.find {it.id==id}
                    when(r?.kind){
                        "project","experiment"->WorkshopScreen(records,templates,graph.workspace,::renderWork,::capture,id,::back)
                        "session"->SessionWorkspace(records,workspace::save,workspace::archive,::renderWork,::capture,id)
                        "note"->NotebookWorkspace(records,workspace::save,workspace::archive,::renderWork,::capture,id)
                        "document"->LaunchedEffect(id){nav.popBackStack();openPdf(r)}
                        else->Page {PageTitle("Recurso",back=::back);InfoCard("Origen no disponible","El trabajo pudo archivarse o aún no terminó de cargar.")}
                    }
                }
                composable("pdf/{id}?start={start}",arguments=listOf(navArgument("id"){type=NavType.StringType},navArgument("start"){type=NavType.IntType;defaultValue=-1})){e->
                    val id=e.arguments?.getString("id");val r=records.find {it.id==id} ?: temporaryPdf?.takeIf {it.id==id}
                    if(r==null)Page {PageTitle("PDF",back=::back);InfoCard("Archivo no disponible","Vuelve a abrir el documento o impórtalo de nuevo.")}
                    else PdfReaderScreen(r,recordUri(r),records,workspace::save,workspace::archive,::preview,{bitmap,index->ocrDestination=null;ocrDocumentId=r.id;ocrPageIndex=index;ocrPage(bitmap)},::back,if(r.kind=="document_temp")({saveDocument(recordUri(r).toString(),null)}) else null,{password,count->batchDocument=r.id;batchPassword=password;batchCount=count;nav.navigate("pdf-ocr")},e.arguments?.getInt("start") ?: -1)
                }
                composable("capture"){CaptureScreen({uri->ocrUri=uri.toString();nav.navigate("ocr")},::back)}
                composable("ocr"){
                    val uri=ocrUri
                    if(uri==null)Page {PageTitle("OCR",back=::back);InfoCard("Selecciona una imagen","El contexto temporal no está disponible.")}
                    else OcrScreen(Uri.parse(uri),graph.ocrDrafts,ocrDestination,{text,bitmap,raw->
                        try {
                            val f=if(bitmap!=null)withContext(Dispatchers.IO){val image=File(context.cacheDir,"review-${newId()}.png");image.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};try {PrivateFiles(context).copyFrom(image.inputStream(),30L*1024*1024)}finally{image.delete()}}else null
                            val existing=ocrDestination?.let {graph.workspace.record(it)}
                            val r=existing ?: WorkRecord(newId(),"note","Texto OCR revisado","{}",System.currentTimeMillis())
                            val data=JSONObject(r.json);val a=data.optJSONArray("attachments") ?: JSONArray();if(f!=null)a.put(JSONObject().put("id",f.id).put("mime","image/png"))
                            data.put("attachments",a).put("ocrRaw",raw).put("ocrReviewed",text).put("body",data.optString("body")+"\n"+text).put("ocrEngine","ML Kit Latin bundled 16.0.1").put("ocrConfirmed",true)
                            ocrDocumentId?.let {doc->data.put("links",JSONArray().put(doc)).put("citations",JSONArray().put(JSONObject().put("documentId",doc).put("pageIndex",ocrPageIndex))).put("ocrPages",JSONArray().put(JSONObject().put("documentId",doc).put("pageIndex",ocrPageIndex).put("text",text)))}
                            graph.workspace.saveRecord(r.copy(json=data.toString()));graph.ocrDrafts.clearDraft("ocr-review");nav.navigate("record/${Uri.encode(r.id)}");true
                        }catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("No se pudo guardar el texto revisado.");false}
                    },::back,ocrDocumentId,ocrPageIndex)
                }
                composable("export-preview"){
                    exportFile?.let {f->ExportPreview(f,graph,{openPdf(WorkRecord(newId(),"document_temp","Vista previa",JSONObject().put("uri",Uri.fromFile(f).toString()).toString(),System.currentTimeMillis()))},::back)} ?: Page {PageTitle("Exportación",back=::back);InfoCard("Vista previa caducada","Vuelve a generar la copia desde tu trabajo guardado.")}
                }
                composable("pdf-ocr"){
                    val document=records.find {it.id==batchDocument}
                    if(document==null)Page {PageTitle("OCR PDF",back=::back);InfoCard("Documento no disponible","Vuelve a abrir el PDF y selecciona el lote.")}
                    else PdfOcrBatchScreen(document.id,graph.repository,batchCount,{index->withContext(Dispatchers.IO){PdfDocumentHandle(context,recordUri(document),batchPassword.takeIf {it.isNotEmpty()}).use {it.render(index,1400)}}},{pages->
                        try {
                            val attachments=JSONArray();val citations=JSONArray()
                            pages.forEach {page->val stored=PrivateFiles(context).copyFrom(File(page.filePath).inputStream(),30L*1024*1024);attachments.put(JSONObject().put("id",stored.id).put("mime","image/png"));citations.put(JSONObject().put("documentId",document.id).put("pageIndex",page.page))}
                            val ocrPages=JSONArray();pages.forEach {ocrPages.put(JSONObject().put("documentId",document.id).put("pageIndex",it.page).put("text",it.corrected))}
                            val data=JSONObject().put("noteType","Apuntes").put("body",pages.joinToString("\n\n"){"Página ${it.page+1}\n${it.corrected}"}).put("ocrRaw",pages.joinToString("\n\n"){it.raw}).put("ocrConfirmed",true).put("attachments",attachments).put("links",JSONArray().put(document.id)).put("citations",citations).put("ocrPages",ocrPages)
                            val note=WorkRecord(newId(),"note","OCR revisado: ${document.title}",data.toString(),System.currentTimeMillis());graph.workspace.saveRecord(note);nav.navigate("record/${Uri.encode(note.id)}");true
                        }catch(e:CancellationException){throw e}catch(_:Exception){snackbar.showSnackbar("No se pudo guardar el lote; revisa almacenamiento.");false}
                    },::back)
                }
            }
            (workspaceError ?: seedError)?.let {message->AlertDialog(onDismissRequest={},title={Text("Problema de datos locales")},text={Text(message)},confirmButton={TextButton(onClick={seedError=null;workspace.dismissError()}){Text("Entendido; revisaré el guardado")}})}
        }
    }
    }
    pendingPdf?.let {uri->AlertDialog(onDismissRequest={pendingPdf=null},title={Text("Abrir PDF")},text={Text("Leer ahora usa acceso temporal. Guardar copia el archivo a tu Biblioteca privada. No se envía a internet.")},confirmButton={TextButton(onClick={pendingPdf=null;saveDocument(uri)}){Text("Guardar en mi Biblioteca")}},dismissButton={TextButton(onClick={pendingPdf=null;openPdf(WorkRecord(newId(),"document_temp","PDF temporal",JSONObject().put("uri",uri).toString(),System.currentTimeMillis()))}){Text("Leer ahora")}})}
}
