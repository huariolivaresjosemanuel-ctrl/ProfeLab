package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.TeacherGuides
import pe.profelab.core.model.WorkRecord

internal fun LazyListScope.teachingItems(record:WorkRecord,template:Boolean,start:()->Unit,edit:()->Unit){
    val guide=TeacherGuides.read(record)
    item("teacher-intro"){WorkshopCard("Docente · Cómo enseñarlo",WorkshopColors.Lavender){
        Text("Sugerencias para adaptar, no un guion para memorizar ni una asociación curricular oficial.")
        Text(if(template)"Crea tu copia para personalizar y guardar el guion."else "Tu guion se guarda con esta creación; actualizar el catálogo no lo reemplaza.",style=MaterialTheme.typography.bodySmall)
        Button(onClick=if(template)start else edit,modifier=Modifier.fillMaxWidth()){Text(if(template)"Crear mi guion docente"else "Editar mi guion docente")}
    }}
    item("teacher-script"){WorkshopCard("Presentación al grupo",WorkshopColors.Mint){
        var variant by rememberSaveable(record.id){mutableStateOf("Breve")}
        WorkshopChoices(listOf("Breve","Desarrollada"),variant){variant=it}
        val key=if(variant=="Breve")"introShort"else "introExtended"
        Text(guide.blocks.first {it.key==key}.texts.joinToString("\n").ifBlank {"Personaliza tu presentación desde Editar mi guion docente."})
    }}
    guide.blocks.filter {it.key !in setOf("introShort","introExtended")}.forEachIndexed {index,block->
        item("teacher:${block.key}"){WorkshopCard(block.title,listOf(WorkshopColors.Yellow,WorkshopColors.Blue,WorkshopColors.Peach,WorkshopColors.Mint)[index%4]){
            if(block.texts.isEmpty())Text(if(block.key=="sources")"Propuesta de enseñanza sin fuentes curriculares cotejadas para esta ficha. Puedes registrar tus referencias."else "Por personalizar")
            else block.texts.forEachIndexed {n,text->Text(if(block.multiple)"${n+1}. $text"else text)}
        }}
    }
}

@Composable
internal fun TeacherGuideEditor(record:WorkRecord,vm:WorkshopViewModel){
    val data=TeacherGuides.resolve(record)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Guion propio y opcional. Las sugerencias son editables y no certifican correspondencias MINEDU.")
        TeacherGuides.fields.forEach {field->
            val text=if(field.multiple)pe.profelab.core.content.WorkshopCodec.strings(data.opt(field.key)).joinToString("\n")else data.optString(field.key)
            OutlinedTextField(text,{value->vm.patch(record){j->
                val current=TeacherGuides.resolve(record.copy(json=j.toString()))
                current.put(field.key,if(field.multiple)JSONArray(value.take(20000).lines().take(40))else value.take(20000))
                current.put("version",1).put("status","USER_DRAFT");j.put("teacherGuide",current)
            }},label={Text(field.label)},supportingText={if(field.multiple)Text("Una pregunta o sugerencia por línea")},minLines=2,modifier=Modifier.fillMaxWidth())
        }
    }
}
