package pe.profelab.feature.taller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

/** Numbered navigation is independent from each step's explicit completion checkbox. */
@Composable
internal fun GuidedStepSelector(count:Int,current:Int,visit:(Int)->Unit){
    if(count<=0)return
    val state=rememberLazyListState()
    LaunchedEffect(current,count){if(current in 0 until count)state.animateScrollToItem(current)}
    LazyRow(Modifier.fillMaxWidth(),state=state,horizontalArrangement=Arrangement.Center){
        items((0 until count).toList(),key={it}){index->
            Row(verticalAlignment=Alignment.CenterVertically){
                OutlinedButton(onClick={visit(index)},modifier=Modifier.size(48.dp).testTag("workshop-visit-step-$index").semantics {contentDescription="Ir al paso ${index+1} de $count";selected=index==current},
                    shape=CircleShape,contentPadding=PaddingValues(0.dp),border=BorderStroke(2.dp,WorkshopColors.Green),
                    colors=ButtonDefaults.outlinedButtonColors(containerColor=if(index==current)WorkshopColors.Green else WorkshopColors.Paper,contentColor=if(index==current)Color.White else WorkshopColors.Deep)){
                    Text("${index+1}",style=MaterialTheme.typography.titleMedium)
                }
                if(index<count-1)Spacer(Modifier.width(20.dp).height(2.dp).background(WorkshopColors.Green.copy(alpha=.25f)))
            }
        }
    }
}

@Composable
internal fun GuidedStepNavigation(current:Int,last:Int,previous:()->Unit,next:()->Unit){
    val nextLabel=if(current<last)"Siguiente"else "Ver resultados y reflexión"
    BoxWithConstraints {
        val stack=maxWidth<300.dp || LocalDensity.current.fontScale>=1.4f
        if(stack)Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            FlowAction("Anterior","workshop-step-previous",previous,false,enabled=current>0)
            FlowAction(nextLabel,"workshop-step-next",next)
        }else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Box(Modifier.weight(1f)){FlowAction("Anterior","workshop-step-previous",previous,false,enabled=current>0)}
            Box(Modifier.weight(1f)){FlowAction(nextLabel,"workshop-step-next",next)}
        }
    }
}
