package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*
import pe.profelab.core.ui.RecordLinks
import pe.profelab.core.ui.JsonFields
import pe.profelab.core.ui.Field

@Composable
internal fun WorkshopEditor(record:WorkRecord,all:List<WorkRecord>,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,back:()->Unit,duplicate:()->Unit,capture:(String)->Unit,initialTab:String="Presentación"){
    val data=WorkshopCodec.normalize(JSONObject(record.json),catalog)
    val title=record.title
    var tab by rememberSaveable(record.id,initialTab){mutableStateOf(initialTab)};var restore by remember {mutableStateOf<WorkRecord?>(null)}
    fun persist(next:JSONObject=data,name:String=title){vm.edit(record,next,name)}
    WorkshopPage("Editar mi creación","Borradores permitidos · comprueba el indicador de guardado",back){
        item("editor-tabs"){WorkshopChoices(listOf("Presentación","Materiales","Pasos","Docente","Indagación","Pedagogía","Evidencias","Vínculos y versiones"),tab){tab=it}}
        when(tab){
            "Presentación"->{
                item("title"){WorkshopCard("Mi proyecto",WorkshopColors.Mint){OutlinedTextField(title,{persist(name=it.take(160))},label={Text("Título")},modifier=Modifier.fillMaxWidth());JsonFields(data,listOf(Field("objectiveSuggested","Objetivo educativo"),Field("notes","Descripción y notas",3),Field("estimatedMinutes","Tiempo estimado en minutos",1))){persist(it)}
                    val minutes=data.optString("estimatedMinutes");if(minutes.isNotBlank() && (minutes.toIntOrNull()?.let {it<=0 || it>10000}!=false))Text("Revisa el tiempo; puedes mantener este borrador.",color=MaterialTheme.colorScheme.error)
                    Text("Área");WorkshopChoices(listOf("Matemática","Comunicación","Ciencia y Tecnología","Personal Social","Arte","Tutoría"),data.optString("area")){persist(JSONObject(data.toString()).put("area",it))}
                    Text("Grados sugeridos de Primaria (independientes del ciclo universitario)")
                    val grades=WorkshopCodec.strings(data.opt("grades")).toSet()
                    for(g in listOf("1","2","3","4","5","6")){Row {Checkbox(g in grades,{checked->val selected=if(checked)grades+g else grades-g;persist(JSONObject(data.toString()).put("grades",JSONArray(selected.sorted())))});Text("$g.º de Primaria")}}
                    Text("Dificultad");WorkshopChoices(listOf("Fácil","Media","Alta"),data.optString("difficulty")){persist(JSONObject(data.toString()).put("difficulty",it))}
                    Text("Diseño de la guía");val designs=linkedMapOf("Material didáctico" to "DIDACTIC","Construcción" to "CONSTRUCTION","Experimento" to "EXPERIMENT","Creativo" to "CREATIVE","Académico" to "ACADEMIC")
                    WorkshopChoices(designs.keys.toList(),designs.entries.firstOrNull {it.value==data.optString("visualTemplate") }?.key.orEmpty()){persist(JSONObject(data.toString()).put("visualTemplate",designs.getValue(it)).removeSectionOrder())}
                    Text("Estado");WorkshopChoices(listOf("BORRADOR","EN_PROGRESO"),data.optString("status","BORRADOR")){persist(JSONObject(data.toString()).put("status",it))}
                }}
            }
            "Materiales"->item("materials"){WorkshopCard("Qué necesitas",WorkshopColors.Blue){MaterialLinesEditor(data,catalog){persist(it)}}}
            "Docente"->item("teacher-guide"){WorkshopCard("Cómo enseñarlo · mi guion",WorkshopColors.Lavender){TeacherGuideEditor(vm.current(record),vm)}}
            "Pasos"->item("steps"){WorkshopCard("Guía de construcción o procedimiento",WorkshopColors.Paper){WorkshopStepsEditor(data){persist(it)}}}
            "Indagación"->item("inquiry"){WorkshopCard("Pregunta, variables y resultados",WorkshopColors.Yellow){
                Text("Registra hipótesis y resultados propios. No se rellenan conclusiones automáticamente.",style=MaterialTheme.typography.bodySmall)
                JsonFields(data,listOf(Field("researchQuestion","Pregunta investigable"),Field("hypothesis","Mi hipótesis"),Field("independentVariable","Variable independiente"),Field("dependentVariable","Variable dependiente"),Field("controls","Condiciones controladas"),Field("results","Resultados reales",3),Field("analysis","Interpretación",3),Field("limitations","Limitaciones"),Field("conclusion","Mi conclusión",3))){persist(it)}
            }}
            "Pedagogía"->item("pedagogy"){WorkshopCard("Uso educativo y fuentes",WorkshopColors.Lavender){
                Text("Estos textos son elaboración del usuario; no acreditan una asociación curricular oficial.",style=MaterialTheme.typography.bodySmall)
                JsonFields(data,listOf(Field("classroomUse","Cómo utilizarlo en el aula"),Field("tips","Consejos y errores frecuentes documentados"),Field("variants","Variantes"),Field("alternatives","Alternativas propuestas y justificación; requieren comprobación"),Field("adaptations","Adaptaciones por barreras"),Field("academicFoundation","Fundamentación propia con límites"),Field("before","Observación antes"),Field("during","Observación durante"),Field("after","Observación después"),Field("rubric","Criterios y niveles de rúbrica"),Field("observationChecklist","Lista de cotejo"),Field("sources","Referencias y ubicación verificables"))){persist(it)}
                val safety=data.optJSONObject("safety") ?: JSONObject()
                Row {Checkbox(safety.optBoolean("adultRequired"),{checked->persist(JSONObject(data.toString()).put("safety",JSONObject(safety.toString()).put("adultRequired",checked)))});Text("Requiere preparación adulta")}
            }}
            "Evidencias"->item("evidence"){WorkshopCard("Mi trabajo, fotos y registros",WorkshopColors.Mint){WorkshopObservations(vm.current(record),vm);WorkshopEvidence(vm.current(record),vm,capture)}}
            "Vínculos y versiones"->item("links"){WorkshopCard("Recursos compartidos",WorkshopColors.Paper){
                RecordLinks(all.filter {it.id!=record.id},data){persist(it)}
                Text("Duplicar conserva la estructura y crea otro ID; no transfiere avances, resultados ni evidencias a una nueva ejecución.",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick=duplicate){Text("Duplicar estructura como nuevo borrador")}
                Button(onClick={vm.save(record.copy(id="history:${record.id}:${newId()}",kind="revision",title=title,json=data.toString(),archived=true))}){Text("Guardar una versión recuperable")}
                all.filter {it.kind=="revision" && it.id.startsWith("history:${record.id}:")}.sortedByDescending {it.updatedAt}.take(20).forEach {old->TextButton(onClick={restore=old}){Text("Recuperar versión · ${java.time.Instant.ofEpochMilli(old.updatedAt)}")}}
            }}
        }
        item("done"){Button(onClick=back,modifier=Modifier.fillMaxWidth()){Text("Volver a mi guía")}}
    }
    restore?.let {old->AlertDialog(onDismissRequest={restore=null},title={Text("¿Recuperar esta versión?")},text={Text("Primero se conserva una versión del estado actual; luego se recupera el contenido elegido.")},confirmButton={TextButton(onClick={vm.save(record.copy(id="history:${record.id}:${newId()}",kind="revision",json=data.toString(),title=title,archived=true));persist(WorkshopCodec.normalize(JSONObject(old.json),catalog),old.title);restore=null}){Text("Recuperar")}},dismissButton={TextButton(onClick={restore=null}){Text("Cancelar")}})}
}

private fun JSONObject.removeSectionOrder():JSONObject {remove("sectionOrder");return this}
