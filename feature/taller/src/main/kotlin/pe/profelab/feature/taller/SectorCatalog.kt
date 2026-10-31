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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.model.*

@Composable
internal fun SectorCatalog(sector:String,all:List<WorkRecord>,definitions:List<WorkRecord>,open:(String)->Unit,back:()->Unit,favorite:(WorkRecord)->Unit,advanced:()->Unit,create:(Boolean)->Unit){
    var query by rememberSaveable(sector){mutableStateOf("")};var favoritesOnly by rememberSaveable(sector){mutableStateOf(false)}
    var area by rememberSaveable(sector){mutableStateOf("Todas")}
    val favorites=all.filter {it.kind=="favorite" && !it.archived}.map {it.id.removePrefix("favorite:")}.toSet()
    val source=if(sector=="ideas")all.filter {it.kind in setOf("project","experiment") && !it.archived}else definitions.filter {WorkshopFlowAdapter.sector(it)==sector}
    val shown=source.filter {r->val j=JSONObject(r.json);(r.title+" "+WorkshopFlowAdapter.read(r).summary).contains(query,true) && (!favoritesOnly || r.id in favorites) && (area=="Todas" || j.optString("area")==area)}
    val title=workshopSectors.firstOrNull {it.id==sector}?.title ?: "Taller"
    WorkshopPage(title,if(sector=="ideas")"Continuar creando"else "Encuentra una actividad para tu aula",back,screenTag="workshop-sector-catalog"){
        item("count"){Text("${shown.size} actividades",Modifier.testTag(if(source.isNotEmpty())"workshop-catalog-ready"else "workshop-catalog-empty"),style=MaterialTheme.typography.bodySmall)}
        item("search"){OutlinedTextField(query,{query=it.take(160)},label={Text("Buscar actividades")},leadingIcon={Icon(Icons.Outlined.Search,null)},modifier=Modifier.fillMaxWidth())}
        item("filters"){WorkshopChoices(listOf("Todas")+source.map {JSONObject(it.json).optString("area")}.filter {it.isNotBlank()}.distinct(),area){area=it};Row {Checkbox(favoritesOnly,{favoritesOnly=it});Text("Solo favoritos")};TextButton(onClick=advanced){Text("Recomendaciones y filtros avanzados")}}
        if(source.isEmpty())item("empty"){FlowCard("${if(sector=="ideas")"Tus ideas empiezan aquí"else "Próximamente"}"){Text(if(sector=="ideas")"Crea una idea o comienza una copia de una actividad."else "No hay contenido compatible instalado para esta categoría.")}}
        items(shown,key={it.id}){r->
            Card(onClick={open(r.id)},shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=WorkshopColors.Paper),modifier=Modifier.fillMaxWidth().testTag("workshop-card:${r.id}")){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    WorkshopPicture(flowCover(r),Modifier.fillMaxWidth().height(190.dp),missingMessage="Portada específica pendiente de revisión")
                    Text(r.title.ifBlank {"Borrador personal"},fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                    Text(WorkshopFlowAdapter.read(r).summary,style=MaterialTheme.typography.bodyMedium)
                    ProjectMetadata(JSONObject(r.json))
                    Row {
                        TextButton(onClick={open(r.id)},modifier=Modifier.weight(1f).testTag("workshop-open:${r.id}")){Text(if(sector=="ideas")"Continuar / ver ficha"else "Ver proyecto")}
                        IconButton(onClick={favorite(r)}){Icon(if(r.id in favorites)Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,"Alternar favorito")}
                    }
                }
            }
        }
        if(shown.isEmpty() && source.isNotEmpty())item("no-match"){Text("Sin coincidencias. Cambia los filtros.")}
        if(sector=="ideas")item("create"){FlowAction("Nuevo proyecto","workshop-create-project",{create(false)});FlowAction("Nuevo experimento","workshop-create-experiment",{create(true)},false)}
    }
}
