package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.model.*
import pe.profelab.core.ui.CanonicalMascot

internal data class WorkshopSector(val id:String,val title:String,val summary:String,val icon:ImageVector,val color:Color)
internal val workshopSectors get()=listOf(
    WorkshopSector("experiments","Experimentos","Explora, indaga y aprende con la ciencia",Icons.Outlined.Science,WorkshopColors.Mint),
    WorkshopSector("taller","Proyectos","Materiales didácticos para crear",Icons.Outlined.Brush,WorkshopColors.Blue),
    WorkshopSector("cooking","Cocina educativa","Recetas con sentido educativo",Icons.Outlined.Restaurant,WorkshopColors.Yellow),
    WorkshopSector("dance","Danza","Expresión, ritmo y movimiento",Icons.Outlined.MusicNote,WorkshopColors.Peach),
    WorkshopSector("heritage","Tradiciones","Valora y comparte nuestra cultura",Icons.Outlined.Public,WorkshopColors.Lavender),
    WorkshopSector("ideas","Mis ideas","Guarda y desarrolla tus proyectos",Icons.Outlined.Lightbulb,WorkshopColors.Mint))

@Composable
internal fun WorkshopHub(all:List<WorkRecord>,definitions:List<WorkRecord>,openSector:(String)->Unit,open:(String)->Unit,inventory:()->Unit,shopping:()->Unit,advanced:()->Unit,create:()->Unit){
    val personal=all.filter {it.kind in setOf("project","experiment") && !it.archived}.sortedByDescending {it.updatedAt}
    WorkshopPage("Mi Taller","Elige un sector y explora actividades",screenTag="workshop-catalog"){
        item("sectors"){
            Box(Modifier.testTag(if(definitions.isNotEmpty())"workshop-hub-ready"else "workshop-hub-loading")){
            FlowGrid(workshopSectors){s,modifier->
                val count=if(s.id=="ideas")personal.size else definitions.count {WorkshopFlowAdapter.sector(it)==s.id}
                val active=s.id in setOf("taller","experiments","ideas") || count>0
                Card(onClick={openSector(s.id)},enabled=active,modifier=modifier.testTag("workshop-sector:${s.id}"),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=s.color,disabledContainerColor=s.color.copy(alpha=.7f))){
                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        WorkshopSectorMediaSlot(s.id,Modifier.size(52.dp)){Icon(s.icon,null,Modifier.size(52.dp),tint=WorkshopColors.Green)}
                        Text(s.title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,color=WorkshopColors.Deep)
                        Text(if(active)s.summary else "Próximamente · sin pack compatible",style=MaterialTheme.typography.bodySmall)
                        if(active)Text(if(s.id=="ideas")"$count creaciones"else "$count actividades",style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
            }
        }
        item("recent"){FlowCard("Recientes",WorkshopColors.Mint,Icons.Outlined.Schedule){
            if(personal.isEmpty())Text("Aquí aparecerán las actividades que comiences. No hay proyectos personales precargados.",style=MaterialTheme.typography.bodySmall)
            else personal.take(4).forEach {r->TextButton(onClick={open(r.id)},modifier=Modifier.testTag("workshop-resume:${r.id}")){Text(r.title.ifBlank {"Borrador personal"})}}
        }}
        item("utilities"){FlowCard("Inventario y compras",WorkshopColors.Paper,Icons.Outlined.Inventory2){Text("Gestiona materiales, existencias y genera tus listas");FlowAction("Revisar inventario","workshop-inventory",inventory,false);FlowAction("Lista de compras","workshop-shopping",shopping,false)}}
        item("create"){FlowAction("Crear mi propia idea","workshop-create",create,false)}
        item("recommendations"){TextButton(onClick=advanced){Text("Recomendaciones y filtros avanzados")}}
        item("companion"){Row {CanonicalMascot(Modifier.width(96.dp).height(112.dp));Text("Puedes consultar las guías sin registrar inventario. Tu compañera ofrece ayuda desde el botón de la aplicación.",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)}}
    }
}
