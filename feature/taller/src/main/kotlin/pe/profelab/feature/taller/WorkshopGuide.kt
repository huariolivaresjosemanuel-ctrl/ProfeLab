package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

@Composable
internal fun WorkshopGuide(record:WorkRecord,all:List<WorkRecord>,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,
    back:()->Unit,start:()->Unit,edit:()->Unit,export:(WorkRecord)->Unit,capture:(String)->Unit,inventory:()->Unit,shopping:()->Unit,favorite:()->Unit,editTeacher:()->Unit=edit,requestedMode:String?=null,modeConsumed:()->Unit={}){
    val j=JSONObject(record.json);val template=record.kind.contains("template") || record.kind.startsWith("catalog_")
    val assessment=WorkshopCodec.assess(record,all,catalog);val scope=rememberCoroutineScope()
    val visual=j.optString("visualTemplate",if(record.kind.contains("experiment"))"EXPERIMENT"else "DIDACTIC")
    var mode by rememberSaveable(record.id){mutableStateOf(if(visual=="ACADEMIC")"Académico"else "Práctico")};var review by remember {mutableStateOf(false)}
    LaunchedEffect(requestedMode){if(requestedMode in setOf("Práctico","Docente","Académico")){mode=requireNotNull(requestedMode);modeConsumed()}}
    var remove by remember {mutableStateOf(false)}
    val steps=j.optJSONArray("steps") ?: JSONArray();val done=if(template)0 else (0 until steps.length()).count {steps.optJSONObject(it)?.optBoolean("done")==true}
    val selected=j.optJSONObject("selectedAlternatives") ?: JSONObject()
    val configured=WorkshopCodec.strings(j.opt("sectionOrder")).filter {it in WorkshopCodec.blocks}
    val defaults=when(visual){"CONSTRUCTION"->listOf("purpose","materials","preparation","steps","classroom","variants","safety","sources","evidence");"EXPERIMENT"->listOf("purpose","inquiry","materials","preparation","steps","evidence","classroom","variants","safety","sources");"CREATIVE"->listOf("purpose","materials","steps","variants","classroom","preparation","safety","sources","evidence");"ACADEMIC"->listOf("purpose","sources","classroom","materials","steps","variants","preparation","safety","evidence");else->WorkshopCodec.blocks}
    val blocks=(if(configured.isEmpty())defaults else configured+listOf("materials","steps","safety","sources","evidence")).distinct()
    val images=projectImages(record)
    WorkshopPage(record.title.ifBlank {"Mi proyecto"},if(template)"Guía editorial · crea tu copia para guardar avances"else "Mi creación · cambios locales",back,screenTag="workshop-guide:${record.id}"){
        item("presentation"){WorkshopCard("",WorkshopColors.Paper){
            BoxWithConstraints {
                val picture:@Composable (Modifier)->Unit={modifier->WorkshopPicture(images.firstOrNull(),modifier.height(250.dp),large=true,missingMessage=j.optString("imageMissingDescription","Sin imagen disponible"))}
                val metadata:@Composable ColumnScope.()->Unit={Text(when(visual){"CONSTRUCTION"->"Guía de construcción";"EXPERIMENT"->"Experimento de indagación";"CREATIVE"->"Proyecto creativo";"ACADEMIC"->"Actividad académica";else->"Material didáctico"},style=MaterialTheme.typography.labelLarge);ProjectMetadata(j);Text(j.optString("objectiveSuggested",j.optString("researchQuestion")).ifBlank {"Objetivo por completar"})}
                if(maxWidth>=650.dp)Row(horizontalArrangement=Arrangement.spacedBy(20.dp)){picture(Modifier.weight(1f));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),content=metadata)}else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){picture(Modifier.fillMaxWidth());metadata()}
            }
            CoverageBadge(assessment)
            Text("Seguridad: ${assessment.safety.label}",style=MaterialTheme.typography.bodySmall)
            if(template)Button(onClick=start,enabled=assessment.safety!=SafetyStatus.FORBIDDEN,modifier=Modifier.fillMaxWidth()){Text(if(record.kind.contains("experiment"))"Iniciar mi experimento"else "Comenzar mi proyecto")}
            else {Text("Estado: ${j.optString("status","BORRADOR")}");Button(onClick=edit,modifier=Modifier.fillMaxWidth()){Text("Editar mi proyecto")}}
            TextButton(onClick=favorite){Text(if(all.any {it.id=="favorite:${record.id}" && !it.archived})"Quitar favorito"else "Guardar favorito")}
        }}
        if(record.kind.contains("experiment"))item("experiment-stages"){WorkshopCard("Investiga y experimenta",WorkshopColors.Mint){
            listOf(
                Triple("1 · Pregunta e hipótesis",j.optString("researchQuestion").ifBlank {"Plantea tu pregunta de investigación"},WorkshopColors.Yellow),
                Triple("2 · Pasos y materiales","${steps.length()} pasos · ${assessment.coverage.size} requisitos por revisar",WorkshopColors.Blue),
                Triple("3 · Observa resultados","${j.optJSONArray("records")?.length() ?: 0} registros propios · añade fechas, medidas y fotografías",WorkshopColors.Peach),
                Triple("4 · Inclusión y seguridad",assessment.safety.label,WorkshopColors.Lavender)
            ).forEach {(title,text,color)->WorkshopCard(title,color){Text(text)}}
        }}
        if(images.size>1)item("gallery"){WorkshopCard("Galería",WorkshopColors.Mint){androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(count=images.size-1,key={images[it+1].id}){i->WorkshopPicture(images[i+1],Modifier.width(180.dp).height(170.dp))}}}}
        item("modes"){WorkshopChoices(listOf("Práctico","Docente","Académico"),mode){mode=it}}
        if(mode=="Académico")item("academic"){AcademicBlocks(j)}
        if(mode=="Docente")teachingItems(record,template,start,editTeacher)
        (if(mode=="Docente")blocks.filter {it in setOf("materials","safety","sources","evidence")}else blocks).forEach {block->when(block){
            "purpose"->item(block){WorkshopCard("¿Para qué sirve?",WorkshopColors.Mint){val practical=j.optJSONObject("pedagogy")?.optJSONObject("practical");Text(practical?.optString("purpose").orEmpty().ifBlank {j.optString("objectiveSuggested").ifBlank {"Define el propósito de tu creación en Editar."}});if(j.optString("notes").isNotBlank())Text(j.optString("notes"))}}
            "inquiry"->if(record.kind.contains("experiment") || j.optString("visualTemplate")=="EXPERIMENT")item(block){WorkshopCard("1 · Pregunta e hipótesis",WorkshopColors.Yellow){
                Text(j.optString("researchQuestion").ifBlank {"Pregunta por completar"});Text("Hipótesis del usuario: ${j.optString("hypothesis").ifBlank {"sin registrar"}}")
                if(template && j.optString("hypothesisExample").isNotBlank())Text("Ejemplo orientativo, no resultado: ${j.optString("hypothesisExample")}",style=MaterialTheme.typography.bodySmall)
                listOf("independentVariable" to "Variable independiente","dependentVariable" to "Variable dependiente","controls" to "Condiciones controladas").forEach {(key,label)->Text("$label: ${j.optString(key).ifBlank {if(key=="controls")WorkshopCodec.text(j.opt("controlConditions"))else "por completar"}}")}
                if(!template)TextButton(onClick=edit){Text("Editar pregunta, hipótesis y variables")}
            }}
            "materials"->item(block){WorkshopCard("Materiales y herramientas",WorkshopColors.Blue){
                if(assessment.coverage.isEmpty())Text("Añade materiales con nombre, cantidad, unidad y tipo desde Editar.")
                listOf("Consumibles" to {s:Supply->!s.tool && !s.reusable},"Reutilizables" to {s:Supply->s.reusable},"Herramientas" to {s:Supply->s.tool}).forEach {(label,filter)->val lines=assessment.coverage.filter {filter(it.requirement)};if(lines.isNotEmpty()){Text(label,style=MaterialTheme.typography.titleSmall);lines.forEach {RequirementRow(it)}}}
                assessment.coverage.flatMap {c->catalog.find {it.id==c.requirement.materialId}?.warnings.orEmpty()}.distinct().forEach {Text("Advertencia del material: $it",style=MaterialTheme.typography.bodySmall)}
                WorkshopCodec.materials(j,catalog).forEach {line->line.alternatives.filter {it.verified && it.source.isNotBlank() && it.reason.isNotBlank()}.forEach {alternative->
                    Text("Alternativa documentada para ${line.name}: ${alternative.name}. ${alternative.reason} · Fuente: ${alternative.source}",style=MaterialTheme.typography.bodySmall)
                    if(!template)TextButton(onClick={vm.patch(record){data->val choices=data.optJSONObject("selectedAlternatives") ?: JSONObject();if(choices.optString(line.id)==alternative.materialId)choices.remove(line.id)else choices.put(line.id,alternative.materialId);data.put("selectedAlternatives",choices)}}){Text(if(selected.optString(line.id)==alternative.materialId)"Usar material original"else "Evaluar esta alternativa")}
                }}
                OutlinedButton(onClick=inventory,modifier=Modifier.fillMaxWidth()){Text("Registrar o revisar mis existencias")}
                Button(onClick={vm.addShopping(record,catalog);shopping()},modifier=Modifier.fillMaxWidth()){Text("Añadir faltantes a compras")}
            }}
            "preparation"->item(block){WorkshopCard("Preparación previa del adulto",WorkshopColors.Peach){
                val warnings=WorkshopCodec.strings(j.optJSONObject("safety")?.opt("warnings"));if(warnings.isEmpty())Text("Revisa materiales, herramientas y adecuación al grupo antes de comenzar.")else warnings.forEach {Text("• $it")}
                if(j.optString("adultPreparation").isNotBlank())Text(j.optString("adultPreparation"))
            }}
            "steps"->item(block){WorkshopCard("Paso a paso · $done/${steps.length()}",WorkshopColors.Paper){
                if(steps.length()>0)LinearProgressIndicator(progress={done.toFloat()/steps.length()},modifier=Modifier.fillMaxWidth())
                if(template)Text("Comienza una copia personal para marcar los pasos. La plantilla no se modifica.",style=MaterialTheme.typography.bodySmall)
                for(i in 0 until steps.length()){val raw=steps.get(i);val s=if(raw is JSONObject)raw else JSONObject().put("title",raw.toString())
                    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(!template && s.optBoolean("done"),{checked->vm.patch(record){data->val a=JSONArray((data.optJSONArray("steps") ?: JSONArray()).toString());val old=a.optJSONObject(i) ?: JSONObject().put("title",s.optString("title"));a.put(i,JSONObject(old.toString()).put("done",checked));data.put("steps",a);val count=(0 until a.length()).count {a.optJSONObject(it)?.optBoolean("done")==true};data.put("status",if(count==a.length() && count>0)"COMPLETADO"else "EN_PROGRESO")}},enabled=!template,modifier=Modifier.testTag("workshop-step-$i"));Text("${i+1}. ${s.optString("title")}",modifier=Modifier.weight(1f))}
                    WorkshopCodec.strings(s.opt("imageFileIds")).forEach {WorkshopPicture(installedWorkshopImage(j,it),Modifier.fillMaxWidth().height(180.dp))}
                    if(s.optString("materialsUsed").isNotBlank())Text("Usa: ${safeMaterialText(s.optString("materialsUsed"))}",style=MaterialTheme.typography.bodySmall)
                    if(s.optString("instructionNote").isNotBlank())Text("Orientación: ${s.optString("instructionNote")}",style=MaterialTheme.typography.bodySmall)
                    if(s.optString("notes").isNotBlank())Text("Observación: ${s.optString("notes")}",style=MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                if(steps.length()==0)Text("Sin pasos todavía. Puedes añadirlos en Editar.")
                Text("Marcar pasos no descuenta existencias. El consumo se confirma en Mi inventario.",style=MaterialTheme.typography.bodySmall)
            }}
            "classroom"->item(block){WorkshopCard("Aplicación en el aula",WorkshopColors.Mint){
                val p=j.optJSONObject("pedagogy")?.optJSONObject("practical")
                Text(j.optString("classroomUse").ifBlank {p?.optString("howToUse").orEmpty().ifBlank {"Añade tu estrategia de aplicación según el contexto del aula."}})
                val observe=p?.optString("whatToObserve").orEmpty();if(observe.isNotBlank())Text("Qué observar: $observe")
                if(j.optString("tips").isNotBlank())Text(j.optString("tips"))
            }}
            "variants"->item(block){WorkshopCard("Variantes y adaptaciones",WorkshopColors.Lavender){
                Text(WorkshopCodec.text(j.opt("variants")).ifBlank {"No se han documentado variantes adicionales."})
                val inclusion=j.optJSONObject("inclusion");(WorkshopCodec.strings(inclusion?.opt("suggestedAdjustments"))+WorkshopCodec.strings(inclusion?.opt("strategies"))).distinct().forEach {Text("• $it")}
                if(j.optString("adaptations").isNotBlank())Text(j.optString("adaptations"))
                if(WorkshopCodec.text(j.opt("alternatives")).isNotBlank())Text("Propuestas del usuario; no sustituyen requisitos automáticamente: ${WorkshopCodec.text(j.opt("alternatives"))}")
                Text("Adaptaciones por barreras de acceso, sin exigir diagnósticos.",style=MaterialTheme.typography.bodySmall)
            }}
            "safety"->item(block){WorkshopCard("Seguridad e inclusión",WorkshopColors.Peach){
                Text(assessment.safety.label)
                val safety=j.optJSONObject("safety");WorkshopCodec.strings(safety?.opt("warnings")).forEach {Text("• $it")}
                WorkshopCodec.strings(safety?.opt("childrenForbiddenActions")).forEach {Text("Restringido al adulto; no para el alumnado: $it")}
                WorkshopCodec.strings(safety?.opt("prohibited")).forEach {Text("No realizar: $it")}
                Text("El contenido editorial está pendiente de validación docente. La revisión de una copia personal no acredita publicación infantil ni aval MINEDU.",style=MaterialTheme.typography.bodySmall)
                if(!template)OutlinedButton(onClick={review=true},enabled=assessment.safety!=SafetyStatus.FORBIDDEN){Text(if(j.optBoolean("safetyReviewedByUser"))"Revisar mi confirmación de seguridad"else "Registrar mi revisión de seguridad")}
            }}
            "sources"->item(block){WorkshopCard("Fuentes y límites",WorkshopColors.Paper){Text(j.optString("source","Creación del usuario"));if(j.optString("sources").isNotBlank())Text(j.optString("sources"));val credits=j.optJSONArray("resourceCredits") ?: JSONArray();for(i in 0 until credits.length()){val c=credits.getJSONObject(i);Text("Imagen: ${c.optString("source")} · licencia declarada ${c.optString("license")}",style=MaterialTheme.typography.bodySmall)};Text("Propuesta editorial o elaboración personal. No se inventan páginas ni asociaciones curriculares oficiales.",style=MaterialTheme.typography.bodySmall)}}
            "evidence"->item(block){WorkshopCard(if(record.kind.contains("experiment"))"3 · Observa resultados"else "Evidencias y observaciones",WorkshopColors.Mint){
                if(template)Text("Inicia tu copia para registrar fotos, observaciones y resultados reales.")
                else {WorkshopObservations(record,vm);WorkshopEvidence(record,vm,capture)}
            }}
        }}
        if(!template && record.kind=="experiment")item("conclusions"){WorkshopCard("Interpretación y conclusiones",WorkshopColors.Yellow){listOf("results" to "Resultados","analysis" to "Interpretación","limitations" to "Limitaciones","conclusion" to "Conclusión del usuario").forEach {(key,title)->Text("$title: ${j.optString(key).ifBlank {"sin registrar"}}")};TextButton(onClick=edit){Text("Escribir mi análisis")}}}
        if(!template)item("export"){Button(onClick={scope.launch {if(vm.flush()){val latest=vm.current(record);export(latest.copy(json=WorkshopCodec.normalize(JSONObject(latest.json),catalog).put("teacherGuide",pe.profelab.core.content.TeacherGuides.resolve(latest)).toString()))}}},modifier=Modifier.fillMaxWidth()){Text("Vista previa / exportar PDF")}}
        if(!template)item("inventory-operations"){InventoryOperationPanel(record,all,vm)}
        if(!template)item("archive"){TextButton(onClick={remove=true}){Text("Archivar mi creación")}}
    }
    if(review)AlertDialog(onDismissRequest={review=false},title={Text("Revisión personal de seguridad")},text={Text("Confirma que revisaste riesgos, preparación adulta y adaptaciones. Esta declaración solo afecta tu copia; no valida el catálogo ni elimina restricciones.")},confirmButton={TextButton(onClick={vm.patch(record){it.put("safetyReviewedByUser",true)};review=false}){Text("He revisado mi preparación")}},dismissButton={TextButton(onClick={vm.patch(record){it.put("safetyReviewedByUser",false)};review=false}){Text("Mantener pendiente")}})
    if(remove)AlertDialog(onDismissRequest={remove=false},title={Text("¿Archivar esta creación?")},text={Text("Se conservan materiales, archivos y vínculos. Puedes recuperarla desde Perfil.")},confirmButton={TextButton(onClick={vm.archive(record.id);remove=false;back()}){Text("Archivar")}},dismissButton={TextButton(onClick={remove=false}){Text("Cancelar")}})
}

@Composable
private fun AcademicBlocks(j:JSONObject){
    val academic=j.optJSONObject("pedagogy")?.optJSONObject("academic")
    WorkshopCard("Fundamentación académica",WorkshopColors.Lavender){
        Text("Área y grado sugeridos");ProjectMetadata(j)
        val processes=WorkshopCodec.text(academic?.opt("potentialProcesses"));if(processes.isNotBlank())Text("Posibilidades de práctica: $processes")
        Text(j.optString("academicFoundation").ifBlank {"Propuesta editorial sin asociación curricular cotejada para esta actividad. Competencias, capacidades y desempeños: pendientes de fuente verificable."})
        Text(academic?.optString("caution").orEmpty().ifBlank {"No garantiza efectos causales, no diagnostica ni evalúa perfiles psicológicos."})
        val claims=j.optJSONArray("curricularClaims") ?: JSONArray()
        for(i in 0 until claims.length()){val c=claims.getJSONObject(i);if(c.optBoolean("verified") && c.optString("source").isNotBlank()){Text("Asociación declarada en el contenido: ${c.optString("competency")}\n${c.optString("capacity")}\n${c.optString("performance")}");Text("Cotejar fuente: ${c.optString("source")} · ${c.optString("location")}",style=MaterialTheme.typography.bodySmall)}}
        Text("Evidencia observable: ${j.optJSONObject("pedagogy")?.optJSONObject("practical")?.optString("whatToObserve").orEmpty().ifBlank {"define tus criterios y recoge observaciones reales"}}")
        if(j.optString("rubric").isNotBlank())Text("Rúbrica del usuario: ${j.optString("rubric")}")
        if(j.optString("observationChecklist").isNotBlank())Text("Cotejo: ${j.optString("observationChecklist")}")
    }
}
