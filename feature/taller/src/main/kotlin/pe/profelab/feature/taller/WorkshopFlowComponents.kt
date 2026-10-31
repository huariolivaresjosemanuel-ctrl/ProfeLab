package pe.profelab.feature.taller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.model.*
import pe.profelab.core.content.WorkshopCodec

@Composable
internal fun FlowCard(title:String="",color:Color=WorkshopColors.Mint,icon:ImageVector?=null,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=color,border=BorderStroke(1.dp,WorkshopColors.Green.copy(alpha=.06f)),shadowElevation=1.dp){
        Column(Modifier.background(Brush.linearGradient(listOf(Color.White.copy(alpha=.18f),Color.Transparent))).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(title.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                icon?.let {Icon(it,null,Modifier.size(28.dp),tint=WorkshopColors.Green)}
                Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=WorkshopColors.Deep)
            }
            content()
        }
    }
}
@Composable
internal fun FlowAction(text:String,tag:String,onClick:()->Unit,primary:Boolean=true,enabled:Boolean=true){
    if(primary)Button(onClick,Modifier.fillMaxWidth().heightIn(min=52.dp).testTag(tag),enabled=enabled,shape=RoundedCornerShape(28.dp),contentPadding=PaddingValues(16.dp)){
        Text(text,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Icon(Icons.AutoMirrored.Outlined.ArrowForward,null)
    }else OutlinedButton(onClick,Modifier.fillMaxWidth().heightIn(min=52.dp).testTag(tag),enabled=enabled,shape=RoundedCornerShape(28.dp),contentPadding=PaddingValues(16.dp)){Text(text,fontWeight=FontWeight.SemiBold)}
}
@Composable
internal fun PerspectiveBar(selected:WorkshopPerspective,onSelect:(WorkshopPerspective)->Unit){
    BoxWithConstraints {
        if(maxWidth<330.dp || LocalDensity.current.fontScale>=1.4f)WorkshopChoices(WorkshopPerspective.entries.map {it.label},selected.label){label->onSelect(WorkshopPerspective.entries.first {it.label==label})}
        else Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            WorkshopPerspective.entries.forEach {p->
                FilterChip(p==selected,{onSelect(p)},label={Text(p.label)},modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(24.dp),
                    colors=FilterChipDefaults.filterChipColors(containerColor=WorkshopColors.Mint,selectedContainerColor=WorkshopColors.Green,selectedLabelColor=Color.White))
            }
        }
    }
}
@Composable
internal fun ActivitySummary(record:WorkRecord,view:ActivityViewState,open:(()->Unit)?=null){
    FlowCard {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            WorkshopPicture(flowCover(record),Modifier.width(80.dp).height(88.dp),missingMessage="Imagen pendiente")
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(record.title.ifBlank {"Mi proyecto"},style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                ProjectMetadata(JSONObject(record.json))
                if(view.personal){LinearProgressIndicator(progress={view.progress},modifier=Modifier.fillMaxWidth());Text("${view.completedSteps}/${view.steps.size} pasos completados",style=MaterialTheme.typography.bodySmall)}
            }
        }
        open?.let {TextButton(onClick=it){Text("Ver ficha del proyecto")}}
    }
}
internal fun flowCover(record:WorkRecord)=projectImages(record).let {images->images.firstOrNull {!it.asset} ?: images.firstOrNull()}

@Composable
internal fun FlowMetadata(j:JSONObject){
    val items=listOf(Icons.Outlined.Schedule to j.optString("estimatedMinutes").let {if(it.isBlank())"Tiempo por indicar"else "$it min"},
        Icons.Outlined.Groups to WorkshopCodec.strings(j.opt("grades")).joinToString("–"){"$it.º"}.ifBlank {"Grado por indicar"},
        Icons.Outlined.SignalCellularAlt to j.optString("difficulty").ifBlank {"Nivel por indicar"})
    if(LocalDensity.current.fontScale>=1.4f)Column(verticalArrangement=Arrangement.spacedBy(6.dp)){items.forEach {FlowMetadataBadge(it,Modifier.fillMaxWidth())}}
    else Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){items.forEach {FlowMetadataBadge(it,Modifier.weight(1f))}}
}
@Composable
private fun FlowMetadataBadge(item:Pair<ImageVector,String>,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(18.dp),color=WorkshopColors.Paper){Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(item.first,null,Modifier.size(18.dp),tint=WorkshopColors.Green);Text(item.second,style=MaterialTheme.typography.labelMedium)}}}

@Composable
internal fun <T> FlowGrid(entries:List<T>,content:@Composable (T,Modifier)->Unit){
    BoxWithConstraints {
        val columns=if(maxWidth>=680.dp && LocalDensity.current.fontScale<1.4f)3 else if(maxWidth>=310.dp && LocalDensity.current.fontScale<1.4f)2 else 1
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            entries.chunked(columns).forEach {group->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                group.forEach {entry->content(entry,Modifier.weight(1f))};repeat(columns-group.size){Spacer(Modifier.weight(1f))}
            }}
        }
    }
}
