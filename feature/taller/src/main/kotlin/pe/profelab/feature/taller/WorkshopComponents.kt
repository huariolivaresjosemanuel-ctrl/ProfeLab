package pe.profelab.feature.taller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import pe.profelab.core.model.*

internal object WorkshopColors {
    val Cream=Color(0xFFF8F5EE);val Green=Color(0xFF135B46);val Deep=Color(0xFF153E35)
    val Mint=Color(0xFFE7EEE3);val Yellow=Color(0xFFFFF0B5);val Blue=Color(0xFFD9EAF2)
    val Peach=Color(0xFFF4D5C9);val Lavender=Color(0xFFEEE4F6);val Paper=Color(0xFFFFFBF5)
}
@Composable
internal fun WorkshopPage(title:String,subtitle:String="",back:(()->Unit)?=null,listState:LazyListState=rememberLazyListState(),screenTag:String?=null,trailing:@Composable ()->Unit={},content:LazyListScope.()->Unit){
    val pageModifier=Modifier.fillMaxSize().background(WorkshopColors.Cream)
    Box(if(screenTag==null)pageModifier else pageModifier.testTag(screenTag),contentAlignment=Alignment.TopCenter){
        LazyColumn(state=listState,modifier=Modifier.widthIn(max=1100.dp).fillMaxSize().testTag("workshop-page"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item("page-title"){BoxWithConstraints {val stacked=maxWidth<320.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.4f;Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Row(verticalAlignment=Alignment.CenterVertically){back?.let {IconButton(onClick=it){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver")}};Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=WorkshopColors.Deep,modifier=Modifier.weight(1f));if(!stacked)trailing()};if(stacked)trailing();if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
            content()
            item("bottom-space"){Spacer(Modifier.height(8.dp))}
        }
    }
}
@Composable
internal fun WorkshopCard(title:String,color:Color=WorkshopColors.Mint,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=color)){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(title.isNotBlank())Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=WorkshopColors.Deep)
            content()
        }
    }
}
@Composable
internal fun WorkshopChoices(options:List<String>,selected:String,choose:(String)->Unit){
    WorkshopChoicesRow(options,selected,Modifier,choose)
}
@Composable
internal fun WorkshopChoicesRow(options:List<String>,selected:String,modifier:Modifier,choose:(String)->Unit){
    LazyRow(modifier=modifier,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        items(options,key={it}){option->FilterChip(selected=option==selected,onClick={choose(option)},label={Text(option)},modifier=Modifier.heightIn(min=48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=WorkshopColors.Green,selectedLabelColor=Color.White))}
    }
}
@Composable
internal fun WorkshopHeading(title:String,action:String="",onAction:()->Unit={}){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));if(action.isNotBlank())TextButton(onClick=onAction){Text(action)}}
}
internal fun displayNumber(number:java.math.BigDecimal?)=number?.stripTrailingZeros()?.toPlainString() ?: "por revisar"
internal fun safeMaterialText(text:String)=text.replace('|','·')
@Composable
internal fun CoverageBadge(result:WorkshopAssessment){
    val color=when{result.safety==SafetyStatus.FORBIDDEN->WorkshopColors.Peach;result.safety==SafetyStatus.PENDING->WorkshopColors.Lavender;result.complete->WorkshopColors.Mint;else->WorkshopColors.Yellow}
    Surface(color=color,shape=RoundedCornerShape(10.dp)){Column(Modifier.padding(10.dp)){Text(result.recommendation,style=MaterialTheme.typography.labelLarge,color=WorkshopColors.Deep);Text("Materiales: ${result.covered}/${result.coverage.size} requisitos cubiertos",style=MaterialTheme.typography.bodySmall)}}
}
@Composable
internal fun MaterialGlyph(category:String,tool:Boolean=false,modifier:Modifier=Modifier){
    val icon=if(tool)Icons.Outlined.Build else when(InventoryRules.canonical(category)){"papel y carton"->Icons.Outlined.Note;"arte"->Icons.Outlined.Palette;"liquidos"->Icons.Outlined.WaterDrop;"naturales"->Icons.Outlined.LocalFlorist;else->Icons.Outlined.Inventory2}
    Surface(modifier,shape=RoundedCornerShape(12.dp),color=WorkshopColors.Paper){Icon(icon,null,Modifier.padding(10.dp).size(28.dp),tint=WorkshopColors.Green)}
}
@Composable
internal fun RequirementRow(coverage:RequirementCoverage){
    val r=coverage.requirement
    Surface(color=if(coverage.covered)WorkshopColors.Paper else WorkshopColors.Yellow,shape=RoundedCornerShape(12.dp)){
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(if(coverage.covered)Icons.Outlined.CheckCircle else Icons.Outlined.Info,null,Modifier.size(20.dp),tint=WorkshopColors.Green);Spacer(Modifier.width(8.dp));Text(safeMaterialText(r.name),fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))}
            Text("Necesitas ${displayNumber(r.quantity)} ${safeMaterialText(r.unit)} · ${when{r.tool->"Herramienta";r.reusable->"Reutilizable";else->"Consumible"}}",style=MaterialTheme.typography.bodyMedium)
            Text(if(coverage.review.isNotBlank())coverage.review else "Disponible: ${displayNumber(coverage.available)} · Falta: ${displayNumber(coverage.missing)} ${safeMaterialText(r.unit)}",style=MaterialTheme.typography.bodySmall)
        }
    }
}
