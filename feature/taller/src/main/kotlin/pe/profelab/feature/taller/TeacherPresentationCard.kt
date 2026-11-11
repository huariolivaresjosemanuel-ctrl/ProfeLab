package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.WorkRecord

/** The approved visual accompanies the reusable script; it never becomes a class observation. */
@Composable
internal fun TeacherPresentationCard(record:WorkRecord,guide:JSONObject){
    FlowCard("Presentación en clase",WorkshopColors.Mint,Icons.Outlined.PresentToAll){
        BoxWithConstraints {
            val alongside=maxWidth>=320.dp && LocalDensity.current.fontScale<1.3f
            if(alongside)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                PresentationSequence(guide,Modifier.weight(1f))
                Column(Modifier.width(120.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    WorkshopMediaSlot(record,"teacher_reference",modifier=Modifier.fillMaxWidth())
                    EditorialTeachingCaption()
                }
            }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                WorkshopMediaSlot(record,"teacher_reference",modifier=Modifier.fillMaxWidth())
                EditorialTeachingCaption()
                PresentationSequence(guide)
            }
        }
    }
}

@Composable
private fun PresentationSequence(guide:JSONObject,modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Inicio: ${guide.optString("motivation")}")
        WorkshopCodec.strings(guide.opt("teachingSequence")).forEachIndexed {i,line->Text("${i+1}. $line")}
        Text("Cierre: ${guide.optString("close")}")
    }
}

@Composable
private fun EditorialTeachingCaption(){
    Text("Apoyo editorial para explicar la actividad; no es evidencia de una clase real.",style=MaterialTheme.typography.bodySmall)
}
