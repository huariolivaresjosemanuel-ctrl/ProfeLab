package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

@Composable
internal fun WorkshopCatalog(all:List<WorkRecord>,templates:List<WorkRecord>,catalog:List<MaterialDefinition>,state:WorkshopSaveState,
    section:String,onSection:(String)->Unit,open:(String)->Unit,create:(Boolean)->Unit,inventory:()->Unit,shopping:()->Unit,favorite:(WorkRecord)->Unit,step:(WorkRecord,Int,Boolean)->Unit,focusRecommendations:Boolean=false,recommendationsConsumed:()->Unit={}){
    val listState=rememberLazyListState()
    var query by rememberSaveable {mutableStateOf("")};var showFilters by rememberSaveable {mutableStateOf(false)}
    var grade by rememberSaveable {mutableStateOf("Todos")};var area by rememberSaveable {mutableStateOf("Todas")}
    var difficulty by rememberSaveable {mutableStateOf("Todas")};var availability by rememberSaveable {mutableStateOf("Todos")}
    var onlyFavorites by rememberSaveable {mutableStateOf(false)};var material by rememberSaveable {mutableStateOf("")}
    var minutes by rememberSaveable {mutableStateOf("")};var safety by rememberSaveable {mutableStateOf("Todos")}
    var category by rememberSaveable {mutableStateOf("Todas")}
    val personal=all.filter {it.kind in setOf("project","experiment") && !it.archived}
    val sources=(templates+all.filter {it.kind in setOf("catalog_project","catalog_experiment") && !it.archived}).distinctBy {it.id}
    val candidates=if(section=="Mis ideas")personal else sources.filter {if(section=="Experimentos")it.kind.contains("experiment")else !it.kind.contains("experiment")}
    val results=remember(all,templates,catalog){(sources+personal).associate {it.id to WorkshopCodec.assess(it,all,catalog)}}
    val favorites=all.filter {it.kind=="favorite" && !it.archived}.map {it.id.removePrefix("favorite:")}.toSet()
    fun matches(r:WorkRecord):Boolean {val j=JSONObject(r.json);val a=results.getValue(r.id)
        return (r.title+" "+j.optString("objectiveSuggested")+" "+j.optString("researchQuestion")).contains(query,true) &&
        (category=="Todas" || j.optString("categoryId",if(r.kind.contains("experiment"))"experiment"else "didactic")==category) &&
        (grade=="Todos" || WorkshopCodec.strings(j.opt("grades")).contains(grade) || j.optString("grade")==grade) &&
        (area=="Todas" || j.optString("area")==area) && (difficulty=="Todas" || j.optString("difficulty").contains(difficulty,true)) &&
        (!onlyFavorites || r.id in favorites) && WorkshopCodec.materials(j,catalog).any {it.name.contains(material,true)}.let {it || material.isBlank()} &&
        (minutes.isBlank() || minutes.toIntOrNull()?.let {limit->limit>0 && j.optInt("estimatedMinutes",0) in 1..limit}==true) &&
        when(safety){"Pendientes"->a.safety==SafetyStatus.PENDING;"No recomendados"->a.safety==SafetyStatus.FORBIDDEN;"Revisadas"->a.safety in setOf(SafetyStatus.REVIEWED,SafetyStatus.ADULT);else->true} &&
        when(availability){"Materiales completos"->a.complete;"Faltantes"->!a.complete;"Preparación adulta"->a.safety==SafetyStatus.ADULT || (j.optJSONObject("safety")?.optBoolean("adultRequired")==true);"Puedo hacerlo"->a.complete && a.safety==SafetyStatus.REVIEWED;else->true}
    }
    val shown=candidates.filter(::matches)
    val progress=personal.filter {r->val s=JSONObject(r.json).optJSONArray("steps");s!=null && s.length()>0 && (0 until s.length()).any {s.optJSONObject(it)?.optBoolean("done")==true} && !(0 until s.length()).all {s.optJSONObject(it)?.optBoolean("done")==true}}
    val complete=personal.count {r->val s=JSONObject(r.json).optJSONArray("steps");s!=null && s.length()>0 && (0 until s.length()).all {s.optJSONObject(it)?.optBoolean("done")==true}}
    val featured=shown.firstOrNull()
    val preparing=progress.firstOrNull() ?: personal.firstOrNull {JSONObject(it.json).optString("status")!="COMPLETADO"}
    LaunchedEffect(focusRecommendations){if(focusRecommendations && section!="Mis ideas"){
        val target=5+(if(showFilters)1 else 0)+(if(featured!=null)1 else 0)+(if(preparing!=null)1 else 0)+(if(section=="Experimentos")1 else 0)
        listState.scrollToItem(target);recommendationsConsumed()
    }}
    WorkshopPage("Mi taller","Crea materiales didácticos, paso a paso",screenTag="workshop-catalog",listState=listState,trailing={Button(onClick={create(section=="Experimentos")}){Icon(Icons.Outlined.Add,null);Text(if(section=="Experimentos")"Nuevo experimento"else "Nuevo proyecto")}}){
        item("search"){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(query,{query=it},placeholder={Text("Buscar proyectos, materiales o temas…")},leadingIcon={Icon(Icons.Outlined.Search,null)},modifier=Modifier.weight(1f),singleLine=true);IconButton(onClick={showFilters=!showFilters}){Icon(Icons.Outlined.Tune,"Filtros del catálogo")}}}
        item("sections"){WorkshopChoicesRow(listOf("Materiales didácticos","Experimentos","Mis ideas"),section,Modifier.testTag("workshop-sections"),onSection)}
        if(showFilters)item("filters"){WorkshopCard("Filtrar proyectos",WorkshopColors.Paper){
            val categories=sources.map {JSONObject(it.json).optString("categoryId",if(it.kind.contains("experiment"))"experiment"else "didactic")}.distinct()
            if(categories.size>1){Text("Tipo de actividad");val labels=pe.profelab.core.content.WorkshopCategories.labels;WorkshopChoices(listOf("Todas")+categories.map {labels[it] ?: it},labels[category] ?: category){label->category=if(label=="Todas")"Todas"else categories.first {labels[it]==label || it==label}}}
            Text("Área");WorkshopChoices(listOf("Todas")+sources.map {JSONObject(it.json).optString("area")}.filter {it.isNotBlank()}.distinct(),area){area=it}
            Text("Grado de Primaria");WorkshopChoices(listOf("Todos","1","2","3","4","5","6"),grade){grade=it}
            Text("Dificultad");WorkshopChoices(listOf("Todas","Fácil","Media"),difficulty){difficulty=it}
            Text("Disponibilidad");WorkshopChoices(listOf("Todos","Materiales completos","Faltantes","Preparación adulta","Puedo hacerlo"),availability){availability=it}
            Text("Seguridad");WorkshopChoices(listOf("Todos","Pendientes","Revisadas","No recomendados"),safety){safety=it}
            OutlinedTextField(minutes,{minutes=it},label={Text("Duración máxima en minutos")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(material,{material=it},label={Text("Material necesario")},modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(onlyFavorites,{onlyFavorites=it});Text("Solo favoritos")}
            TextButton(onClick={query="";grade="Todos";area="Todas";difficulty="Todas";availability="Todos";safety="Todos";minutes="";material="";category="Todas";onlyFavorites=false}){Text("Limpiar filtros")}
        }}
        item("counts"){Column {Text("${sources.count {results[it.id]?.complete==true}} con materiales completos · ${progress.size} en progreso · $complete completados",style=MaterialTheme.typography.bodySmall);TextButton(onClick=shopping){Icon(Icons.Outlined.ShoppingCart,null);Text("Mi lista de compras")}}}
        if(featured!=null && section!="Mis ideas")item("featured"){ProjectHero(featured,results.getValue(featured.id),featured.id in favorites,{open(featured.id)},{favorite(featured)})}
        item("inventory"){WorkshopCard("Materiales disponibles",WorkshopColors.Mint){
            WorkshopHeading("Mi inventario","Gestionar inventario",inventory)
            val stock=all.filter {it.kind=="inventory" && !it.archived}
            if(stock.isEmpty())Text("Aún no has registrado existencias. Añade lo que realmente tienes para calcular los proyectos compatibles.")
            else LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(stock.take(12),key={it.id}){r->val j=JSONObject(r.json);Card(onClick=inventory,colors=CardDefaults.cardColors(containerColor=WorkshopColors.Paper),modifier=Modifier.width(112.dp)){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){MaterialGlyph(j.optString("category"),WorkshopCodec.kind(j)==MaterialKind.TOOL);Text(r.title,style=MaterialTheme.typography.labelLarge);Text("${j.optString("quantity")} ${safeMaterialText(j.optString("unit"))}",style=MaterialTheme.typography.bodySmall)}}}}
        }}
        if(preparing!=null)item("preparation"){
            val steps=JSONObject(preparing.json).optJSONArray("steps");val total=steps?.length() ?: 0;val done=(0 until total).count {steps?.optJSONObject(it)?.optBoolean("done")==true}
            WorkshopCard("Preparación",WorkshopColors.Paper){WorkshopHeading(preparing.title.ifBlank {"Borrador personal"},"Continuar",{open(preparing.id)});Text("$done/$total pasos completados");if(total>0)LinearProgressIndicator(progress={done.toFloat()/total},modifier=Modifier.fillMaxWidth())
                for(i in 0 until minOf(total,4)){val s=steps?.optJSONObject(i);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(s?.optBoolean("done")==true,{step(preparing,i,it)});Text(s?.optString("title").orEmpty(),modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)}}
            }
        }
        if(section=="Experimentos")item("review"){WorkshopCard("Investiga y experimenta",WorkshopColors.Lavender){Text("Pregunta e hipótesis → pasos y materiales → resultados reales → inclusión y seguridad.");Text("Los ocho experimentos originales siguen pendientes de revisión pedagógica y de seguridad. Ninguno se presenta como validado para niños.",style=MaterialTheme.typography.bodySmall)}}
        if(section!="Mis ideas"){
            val recommended=personal.filter {val a=results.getValue(it.id);a.safety !in setOf(SafetyStatus.PENDING,SafetyStatus.FORBIDDEN) && matches(it)}.sortedWith(compareByDescending<WorkRecord>{results.getValue(it.id).complete}.thenByDescending {results.getValue(it.id).score}.thenBy {it.title})
            item("recommend-heading"){WorkshopHeading("¿Qué puedo crear con lo que tengo?")}
            if(recommended.isEmpty())item("recommend-empty"){Text("Las cantidades ya se comparan abajo. Para incluir una actividad en recomendaciones, inicia una copia y revisa su seguridad; las plantillas no están validadas.",style=MaterialTheme.typography.bodySmall)}
            items(recommended.take(4),key={"recommend:${it.id}"}){r->ProjectSmallCard(r,results.getValue(r.id),r.id in favorites,{open(r.id)},{favorite(r)})}
            val compatible=shown.filter {results.getValue(it.id).score>0 && results.getValue(it.id).safety!=SafetyStatus.FORBIDDEN}.sortedWith(compareByDescending<WorkRecord>{results.getValue(it.id).complete}.thenByDescending {results.getValue(it.id).score}.thenBy {it.title}).take(4)
            if(compatible.isNotEmpty()){
                item("compatibility-heading"){Column {WorkshopHeading("Más compatibles con mis materiales");Text("Comparación de existencias, sin aval de seguridad. Las advertencias se conservan en cada tarjeta.",style=MaterialTheme.typography.bodySmall)}}
                items(compatible,key={"compatible:${it.id}"}){r->ProjectSmallCard(r,results.getValue(r.id),r.id in favorites,{open(r.id)},{favorite(r)})}
            }
        }
        item("catalog-heading"){WorkshopHeading(if(section=="Mis ideas")"Continuar creando"else "Más proyectos para ti")}
        if(shown.isEmpty())item("empty"){WorkshopCard("Sin coincidencias",WorkshopColors.Paper){Text("Cambia los filtros o crea una idea. No hay proyectos personales precargados.")}}
        items(shown.chunked(2),key={row->"row:"+row.joinToString {it.id}}){row->
            BoxWithConstraints {if(maxWidth>=300.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale<1.4f)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach {r->Box(Modifier.weight(1f)){ProjectSmallCard(r,results.getValue(r.id),r.id in favorites,{open(r.id)},{favorite(r)})}};if(row.size==1)Spacer(Modifier.weight(1f))}else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){row.forEach {r->ProjectSmallCard(r,results.getValue(r.id),r.id in favorites,{open(r.id)},{favorite(r)})}}}
        }
        if(section=="Mis ideas"){
            val archived=all.filter {it.archived && it.kind in setOf("project","experiment")}
            if(archived.isNotEmpty())item("archive-info"){Text("${archived.size} trabajos archivados. Recupéralos desde Perfil → Respaldo, paquetes y trabajos archivados.",style=MaterialTheme.typography.bodySmall)}
        }
        item("save-state"){Text(if(state.pending>0)"Guardando cambios…"else state.message,style=MaterialTheme.typography.bodySmall)}
    }
}
@Composable
private fun ProjectHero(r:WorkRecord,a:WorkshopAssessment,favorite:Boolean,open:()->Unit,toggle:()->Unit){
    val j=JSONObject(r.json)
    WorkshopCard("Proyecto destacado",WorkshopColors.Paper){
        BoxWithConstraints {
            val picture:@Composable (Modifier)->Unit={modifier->WorkshopPicture(projectImages(r).firstOrNull(),modifier.aspectRatio(1f),missingMessage=j.optString("imageMissingDescription","Sin imagen disponible"))}
            val text:@Composable ColumnScope.()->Unit={Text(r.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(j.optString("objectiveSuggested",j.optString("researchQuestion")),style=MaterialTheme.typography.bodyMedium);ProjectMetadata(j);CoverageBadge(a);Row {Button(onClick=open,modifier=Modifier.weight(1f).testTag("workshop-open:${r.id}")){Text("Ver guía")};IconButton(onClick=toggle){Icon(if(favorite)Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,if(favorite)"Quitar favorito"else "Guardar favorito")}}}
            if(maxWidth>=300.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale<1.4f)Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){picture(Modifier.weight(1f));Column(Modifier.weight(1.25f),verticalArrangement=Arrangement.spacedBy(8.dp),content=text)}
            else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){picture(Modifier.fillMaxWidth());text()}
        }
    }
}
@Composable
internal fun ProjectMetadata(j:JSONObject){Text(listOf(j.optString("area"),WorkshopCodec.strings(j.opt("grades")).joinToString(", ").let {if(it.isBlank())"Grado por indicar"else "$it.º Primaria"},j.optString("estimatedMinutes").let {if(it.isBlank())"Tiempo por indicar"else "$it min"},j.optString("difficulty")).filter {it.isNotBlank()}.joinToString(" · "),style=MaterialTheme.typography.bodySmall)}
@Composable
private fun ProjectSmallCard(r:WorkRecord,a:WorkshopAssessment,favorite:Boolean,open:()->Unit,toggle:()->Unit){
    Card(onClick=open,colors=CardDefaults.cardColors(containerColor=WorkshopColors.Paper),shape=MaterialTheme.shapes.large,modifier=Modifier.testTag("workshop-card:${r.id}")){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        WorkshopPicture(projectImages(r).firstOrNull(),Modifier.fillMaxWidth().aspectRatio(1.3f),missingMessage=JSONObject(r.json).optString("imageMissingDescription","Sin imagen disponible"))
        Text(r.title.ifBlank {"Borrador sin título"},fontWeight=FontWeight.SemiBold);ProjectMetadata(JSONObject(r.json));CoverageBadge(a)
        Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick=open,modifier=Modifier.weight(1f)){Text("Ver guía")};IconButton(onClick=toggle){Icon(if(favorite)Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,"Alternar favorito")}}
    }}
}
