package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.model.*

@Composable
internal fun SectorCatalog(sector:String,all:List<WorkRecord>,definitions:List<WorkRecord>,open:(String)->Unit,back:()->Unit,favorite:(WorkRecord)->Unit,advanced:()->Unit,create:(Boolean)->Unit){
    var query by rememberSaveable(sector){mutableStateOf("")};var favoritesOnly by rememberSaveable(sector){mutableStateOf(false)}
    var area by rememberSaveable(sector){mutableStateOf("Todas")};var difficulty by rememberSaveable(sector){mutableStateOf("Todos")}
    var filters by rememberSaveable(sector){mutableStateOf(false)}
    val favorites=all.filter {it.kind=="favorite" && !it.archived}.map {it.id.removePrefix("favorite:")}.toSet()
    val source=if(sector=="ideas")all.filter {it.kind in setOf("project","experiment") && !it.archived}else definitions.filter {WorkshopFlowAdapter.sector(it)==sector}
    val shown=source.filter {r->val j=JSONObject(r.json);(r.title+" "+WorkshopFlowAdapter.read(r).summary).contains(query,true) && (!favoritesOnly || r.id in favorites) && (area=="Todas" || j.optString("area")==area) && (difficulty=="Todos" || j.optString("difficulty")==difficulty)}
    val title=workshopSectors.firstOrNull {it.id==sector}?.title ?: "Proyectos"
    BoxWithConstraints(Modifier.fillMaxSize()){
        val columns=if(maxWidth>=350.dp && LocalDensity.current.fontScale<1.4f)2 else 1
        WorkshopPage(title,when(sector){"ideas"->"Continuar creando";"experiments"->"Explora, indaga y aprende con la ciencia";else->"Encuentra una actividad para tu aula"},back,screenTag="workshop-sector-catalog"){
            item("search"){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(query,{query=it.take(160)},label={Text("Buscar actividad…")},leadingIcon={Icon(Icons.Outlined.Search,null)},modifier=Modifier.weight(1f),singleLine=true)
                IconButton(onClick={filters=!filters},modifier=Modifier.heightIn(min=56.dp)){Icon(Icons.Outlined.Tune,"Mostrar filtros")}
            }}
            item("difficulty"){WorkshopChoices(listOf("Todos")+source.map {JSONObject(it.json).optString("difficulty")}.filter {it.isNotBlank()}.distinct(),difficulty){difficulty=it}}
            if(filters)item("filters"){FlowCard("Filtrar actividades",WorkshopColors.Paper){
                WorkshopChoices(listOf("Todas")+source.map {JSONObject(it.json).optString("area")}.filter {it.isNotBlank()}.distinct(),area){area=it}
                Row {Checkbox(favoritesOnly,{favoritesOnly=it});Text("Solo favoritos")}
                TextButton(onClick=advanced){Text("Recomendaciones y filtros avanzados")}
            }}
            item("count"){Text("${shown.size} actividades",Modifier.testTag(if(source.isNotEmpty())"workshop-catalog-ready"else "workshop-catalog-empty"),style=MaterialTheme.typography.bodySmall)}
            if(source.isEmpty())item("empty"){FlowCard(if(sector=="ideas")"Tus ideas empiezan aquí"else "Próximamente"){
                Text(if(sector=="ideas")"Crea una idea o comienza una copia de una actividad."else "No hay contenido compatible instalado para esta categoría.")
            }}
            items(shown.chunked(columns),key={row->row.joinToString("|"){it.id}}){row->
                Row(Modifier.fillMaxWidth().testTag("workshop-catalog-row"),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    row.forEach {r->CatalogActivityCard(r,r.id in favorites,Modifier.weight(1f),{open(r.id)},{favorite(r)})}
                    repeat(columns-row.size){Spacer(Modifier.weight(1f))}
                }
            }
            if(shown.isEmpty() && source.isNotEmpty())item("no-match"){Text("Sin coincidencias. Cambia los filtros.")}
            item("more"){TextButton(onClick=advanced){Text("Más actividades y herramientas avanzadas")}}
            if(sector=="ideas")item("create"){FlowAction("Nuevo proyecto","workshop-create-project",{create(false)});FlowAction("Nuevo experimento","workshop-create-experiment",{create(true)},false)}
        }
    }
}

@Composable
private fun CatalogActivityCard(record:WorkRecord,favorite:Boolean,modifier:Modifier,open:()->Unit,toggleFavorite:()->Unit){
    val data=JSONObject(record.json)
    Card(onClick=open,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=WorkshopColors.Paper),modifier=modifier.testTag("workshop-card:${record.id}")){
        Box {
            WorkshopMediaSlot(record,"catalog_cover",modifier=Modifier.fillMaxWidth())
            IconButton(onClick=toggleFavorite,modifier=Modifier.align(androidx.compose.ui.Alignment.TopEnd)){
                Icon(if(favorite)Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,if(favorite)"Quitar favorito"else "Guardar favorito",tint=WorkshopColors.Green)
            }
        }
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(record.title.ifBlank {"Borrador personal"},fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
            Text(WorkshopFlowAdapter.read(record).summary,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
            ProjectMetadata(data)
            TextButton(onClick=open,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("workshop-open:${record.id}")){Text("Ver actividad")}
        }
    }
}
