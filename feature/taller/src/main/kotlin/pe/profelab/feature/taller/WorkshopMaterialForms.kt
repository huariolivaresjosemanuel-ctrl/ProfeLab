package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

internal val materialCategories=listOf("Papel y cartón","Reciclados","Madera","Telas","Arte","Líquidos","Naturales","Herramientas","Otros")
internal val materialUnits=listOf("unidad","hoja","m","cm","g","kg","ml","l","paquete")
@Composable
internal fun MaterialForm(initial:MaterialLine,catalog:List<MaterialDefinition>,close:()->Unit,lockIdentity:Boolean=false,confirm:(MaterialLine)->Unit){
    var name by rememberSaveable(initial.id){mutableStateOf(initial.name)};var amount by rememberSaveable(initial.id){mutableStateOf(initial.amount)}
    var unit by rememberSaveable(initial.id){mutableStateOf(initial.unit)};var kind by rememberSaveable(initial.id){mutableStateOf(initial.kind.label)}
    var content by rememberSaveable(initial.id){mutableStateOf(initial.contentAmount)};var contentUnit by rememberSaveable(initial.id){mutableStateOf(initial.contentUnit)}
    var error by remember {mutableStateOf<String?>(null)}
    Dialog(onDismissRequest=close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.padding(16.dp).widthIn(max=720.dp).fillMaxWidth().fillMaxHeight(.92f).imePadding(),shape=MaterialTheme.shapes.large,color=WorkshopColors.Cream){
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Material o herramienta",style=MaterialTheme.typography.titleLarge)
                OutlinedTextField(name,{name=it.take(120)},label={Text("Nombre")},enabled=!lockIdentity,modifier=Modifier.fillMaxWidth())
                val matches=catalog.filter {(listOf(it.name)+it.synonyms).any {s->s.contains(name,true)}}.take(6)
                if(name.isNotBlank() && !lockIdentity)matches.forEach {definition->TextButton(onClick={name=definition.name;if(unit.isBlank())unit=definition.unit;kind=definition.kind.label}){Text("Usar ${definition.name}")}}
                OutlinedTextField(amount,{amount=it},label={Text("Cantidad (vacío si aún no se conoce)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(unit,{unit=it.take(80)},label={Text("Unidad")},enabled=!lockIdentity,modifier=Modifier.fillMaxWidth())
                if(!lockIdentity){WorkshopChoices(materialUnits,InventoryRules.unit(unit)){unit=it};Text("Tipo de uso");WorkshopChoices(MaterialKind.entries.map {it.label},kind){kind=it}}
                if(InventoryRules.unit(unit)=="paquete"){
                    Text("Solo se compara un paquete cuando su contenido es conocido.",style=MaterialTheme.typography.bodySmall)
                    OutlinedTextField(content,{content=it},label={Text("Contenido por paquete (cantidad)")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(contentUnit,{contentUnit=it},label={Text("Unidad del contenido, por ejemplo hoja")},modifier=Modifier.fillMaxWidth())
                }
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                Button(onClick={
                    val invalid=name.isBlank() || '|' in name || '|' in unit || (amount.isNotBlank() && InventoryRules.decimal(amount)?.signum()!=1) || (content.isNotBlank() && (InventoryRules.decimal(content)?.signum()!=1 || contentUnit.isBlank() || InventoryRules.unit(contentUnit)=="paquete"))
                    if(invalid)error="Revisa nombre, cantidad positiva, unidad y equivalencia. No utilices separadores de registros antiguos."
                    else {val definition=WorkshopCodec.resolve(name,catalog);confirm(initial.copy(name=name.trim(),materialId=definition?.id ?: if(name==initial.name)initial.materialId else legacyMaterialId(name),amount=amount,unit=unit.trim(),kind=MaterialKind.entries.first {it.label==kind},contentAmount=content,contentUnit=contentUnit,warning=""))}
                },modifier=Modifier.fillMaxWidth()){Text("Guardar material")}
                OutlinedButton(onClick=close,modifier=Modifier.fillMaxWidth()){Text("Cancelar")}
            }
        }
    }
}
@Composable
internal fun MaterialLinesEditor(data:JSONObject,catalog:List<MaterialDefinition>,changed:(JSONObject)->Unit){
    val lines=WorkshopCodec.materials(data,catalog);var editingId by rememberSaveable {mutableStateOf<String?>(null)};var remove by remember {mutableStateOf<String?>(null)}
    val editing=editingId?.let {id->lines.find {it.id==id} ?: MaterialLine(id,"","","","unidad",MaterialKind.CONSUMABLE)}
    fun update(next:List<MaterialLine>){val array=JSONArray();next.forEach {array.put(WorkshopCodec.materialJson(it))};changed(JSONObject(data.toString()).put("materials",array))}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        lines.forEach {line->key(line.id){WorkshopCard(line.name,WorkshopColors.Paper){
            Text("${line.amount.ifBlank {"Cantidad por indicar"}} ${safeMaterialText(line.unit)} · ${line.kind.label}")
            if(line.warning.isNotBlank())Text(line.warning,color=MaterialTheme.colorScheme.error)
            Row {TextButton(onClick={editingId=line.id}){Text("Editar")};TextButton(onClick={remove=line.id}){Text("Quitar de la lista")}}
        }}}
        Button(onClick={editingId=newId()}){Text("Añadir material o herramienta")}
    }
    editing?.let {line->MaterialForm(line,catalog,{editingId=null}){next->update(if(lines.any {it.id==next.id})lines.map {if(it.id==next.id)next else it}else lines+next);editingId=null}}
    remove?.let {id->AlertDialog(onDismissRequest={remove=null},title={Text("¿Quitar este requisito?")},text={Text("Esto no elimina existencias del inventario.")},confirmButton={TextButton(onClick={update(lines.filterNot {it.id==id});remove=null}){Text("Quitar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
@Composable
internal fun WorkshopStepsEditor(data:JSONObject,changed:(JSONObject)->Unit){
    val a=data.optJSONArray("steps") ?: JSONArray();var remove by remember {mutableStateOf<Int?>(null)}
    fun update(index:Int,key:String,value:Any){val next=JSONArray(a.toString());val step=next.optJSONObject(index) ?: JSONObject().put("id",newId());next.put(index,JSONObject(step.toString()).put(key,value));changed(JSONObject(data.toString()).put("steps",next))}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        for(i in 0 until a.length()){val s=a.getJSONObject(i);key(s.optString("id","step-$i")){
            WorkshopCard("Paso ${i+1}",WorkshopColors.Paper){
                OutlinedTextField(s.optString("title"),{update(i,"title",it.take(4000))},label={Text("Descripción")},minLines=2,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(s.optString("materialsUsed"),{update(i,"materialsUsed",it)},label={Text("Materiales usados en este paso (opcional)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(s.optString("notes"),{update(i,"notes",it)},label={Text("Mi observación de este paso")},modifier=Modifier.fillMaxWidth())
                Row {Checkbox(s.optBoolean("done"),{update(i,"done",it)});Text("Completado")}
                Row {TextButton(enabled=i>0,onClick={val next=JSONArray(a.toString());val previous=next.get(i-1);next.put(i-1,next.get(i));next.put(i,previous);changed(JSONObject(data.toString()).put("steps",next))}){Text("Subir")};TextButton(enabled=i<a.length()-1,onClick={val next=JSONArray(a.toString());val following=next.get(i+1);next.put(i+1,next.get(i));next.put(i,following);changed(JSONObject(data.toString()).put("steps",next))}){Text("Bajar")};TextButton(onClick={remove=i}){Text("Quitar")}}
            }
        }}
        Button(onClick={changed(JSONObject(data.toString()).put("steps",JSONArray(a.toString()).put(JSONObject().put("id",newId()).put("title","").put("done",false))))}){Text("Añadir paso")}
    }
    remove?.let {index->AlertDialog(onDismissRequest={remove=null},title={Text("¿Quitar este paso?")},confirmButton={TextButton(onClick={val next=JSONArray();for(i in 0 until a.length())if(i!=index)next.put(a.get(i));changed(JSONObject(data.toString()).put("steps",next));remove=null}){Text("Quitar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
