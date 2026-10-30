package pe.profelab.app

import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import pe.profelab.core.model.*
import pe.profelab.core.security.ContentPolicy
import pe.profelab.core.ui.*
import pe.profelab.feature.home.*
import pe.profelab.feature.library.*
import pe.profelab.feature.notebook.NotebookScreen
import pe.profelab.feature.profile.*
import pe.profelab.feature.sessions.SessionsScreen
import pe.profelab.feature.taller.TallerScreen

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
                composable("home") { HomeScreen(homeState,::top,::tasks,::edit,{nav.navigate("activity")},{nav.navigate("profile")},::complete,home::retry) }
                composable("taller") { TallerScreen() }
                composable("sessions") { SessionsScreen() }
                composable("notebook") { NotebookScreen() }
                composable("practices") { NotebookScreen("Prácticas") }
                composable("library") {
                    val vm: LibraryViewModel=viewModel(factory=modelFactory { LibraryViewModel(graph.catalog) })
                    val state by vm.state.collectAsStateWithLifecycle()
                    LibraryScreen(state,vm::reload) { url ->
                        try {
                            require(ContentPolicy.isOfficialSource(url))
                            context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
                        } catch (_: Exception) { scope.launch { snackbar.showSnackbar("No se pudo abrir la fuente. Comprueba que tienes un navegador disponible.") } }
                    }
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
                composable("activity") { ActivityScreen(homeState,{ if(it.kind==EntityKind.TASK) edit(it.id) else nav.navigate("profile") },::back) }
                composable("profile") {
                    val vm: ProfileViewModel=viewModel(factory=modelFactory { ProfileViewModel(graph.repository,graph.repository,graph.preferences) })
                    val state by vm.state.collectAsStateWithLifecycle()
                    val settings by vm.settings.collectAsStateWithLifecycle()
                    ProfileScreen(vm,state,settings,::back) { nav.navigate("about") }
                }
                composable("about") { AboutScreen(::back) }
            }
        }
    }
}
