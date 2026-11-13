package pe.profelab.feature.students

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class EducationActions(private val scope:CoroutineScope) {
    var busy by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null)
    fun perform(block:suspend ()->Unit){
        if(busy)return;busy=true;error=null
        scope.launch {try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?.take(250) ?: "No se pudo guardar. Revisa los datos y vuelve a intentar."}finally{busy=false}}
    }
}
@Composable internal fun rememberEducationActions():EducationActions {val scope=rememberCoroutineScope();return remember(scope){EducationActions(scope)}}
@Composable internal fun EducationState(actions:EducationActions){if(actions.busy)LinearProgressIndicator(Modifier.fillMaxWidth());actions.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}
@Composable internal fun EducationCard(title:String,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,style=MaterialTheme.typography.titleMedium);content()}}}
@Composable internal fun EducationChoices(values:List<Pair<String,String>>,selected:String,change:(String)->Unit){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){values.forEach {(id,label)->FilterChip(selected==id,{change(id)},label={Text(label)})}}}
@Composable internal fun EducationDialog(title:String,dismiss:()->Unit,content:@Composable ColumnScope.()->Unit){Dialog(onDismissRequest=dismiss){Surface(shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().heightIn(max=580.dp).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.titleLarge);content();TextButton(onClick=dismiss){Text("Cerrar")}}}}}
