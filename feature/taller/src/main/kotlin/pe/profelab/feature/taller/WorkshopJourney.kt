package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Route position, not completion: reading a page never marks a step or consumes stock. */
@Composable
internal fun WorkshopJourney(phase:Int){
    val labels=listOf("Preparar","Realizar","Observar","Finalizar")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        labels.forEachIndexed {i,label->
            Surface(color=if(i==phase)WorkshopColors.Mint else WorkshopColors.Paper,shape=MaterialTheme.shapes.medium){
                Text("${i+1} · $label",Modifier.padding(10.dp).semantics {stateDescription=if(i==phase)"Etapa actual"else "Etapa del recorrido"},style=MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun OptionalWorkshopSection(id:String,title:String,description:String="",content:@Composable ColumnScope.()->Unit){
    var expanded by rememberSaveable(id){mutableStateOf(false)}
    FlowCard(color=WorkshopColors.Paper){
        TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag(id).semantics {stateDescription=if(expanded)"Expandido"else "Contraído"}){
            Text((if(expanded)"− "else "+ ")+title,Modifier.weight(1f))
        }
        if(description.isNotBlank())Text(description,style=MaterialTheme.typography.bodySmall)
        if(expanded)content()
    }
}
