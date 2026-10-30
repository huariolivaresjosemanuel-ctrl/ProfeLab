package pe.profelab.core.files

import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.LegacyMaterialParser

object WorkReport {
    private val labels=linkedMapOf("date" to "Fecha","grade" to "Grado de Primaria","area" to "Área","status" to "Estado",
        "body" to "Texto","notes" to "Notas","tags" to "Etiquetas","objectiveSuggested" to "Objetivo",
        "researchQuestion" to "Pregunta de indagación","hypothesis" to "Hipótesis del usuario","independentVariable" to "Variable independiente",
        "dependentVariable" to "Variable dependiente","controls" to "Condiciones controladas","results" to "Resultados reales",
        "analysis" to "Interpretación y límites","conclusion" to "Conclusión del usuario","purpose" to "Propósito",
        "evidence" to "Evidencia de aprendizaje","criteria" to "Criterios de evaluación","instrument" to "Instrumento",
        "competencyManual" to "Competencia introducida por el usuario","capacityManual" to "Capacidades introducidas por el usuario",
        "performanceManual" to "Desempeños introducidos por el usuario (cotejo pendiente)","curriculumSource" to "Fuente curricular para cotejo",
        "totalMinutes" to "Tiempo total (minutos)","start" to "Inicio","startMinutes" to "Tiempo de inicio",
        "development" to "Desarrollo","developmentMinutes" to "Tiempo de desarrollo","close" to "Cierre","closeMinutes" to "Tiempo de cierre",
        "adaptations" to "Adaptaciones por barreras","before" to "Antes","during" to "Durante","after" to "Después",
        "rubric" to "Rúbrica","observationChecklist" to "Lista de cotejo","observation" to "Observación anónima",
        "reflection" to "Reflexión","nextSteps" to "Próximos pasos","sources" to "Fuentes introducidas por el usuario",
        "source" to "Procedencia editorial","school" to "Institución (dato opcional)","schoolGrade" to "Grado de práctica",
        "variants" to "Variantes","alternatives" to "Alternativas y justificación","classroomUse" to "Aplicación en el aula","tips" to "Consejos documentados","academicFoundation" to "Fundamentación del usuario","limitations" to "Limitaciones")
    fun render(record:WorkRecord,titles:Map<String,String>):String {
        val data=JSONObject(record.json);val out=StringBuilder(record.title+"\n")
        labels.forEach {(key,label)->val value=data.opt(key);if(value!=null && value!=JSONObject.NULL && value.toString().isNotBlank())out.append("\n$label\n$value\n")}
        if(record.kind=="session")out.append("\nCotejo curricular: ${if(data.optBoolean("curriculumVerifiedByUser"))"declarado por el usuario; ProfeLab no lo acredita" else "pendiente"}. Elaboración personal, no documento oficial.\n")
        val materials=data.opt("materials")
        fun legacy(text:String){LegacyMaterialParser.parse(text).forEach {m->out.append("• ${m.name.replace('|','·')}: ${if(m.issue.isBlank())m.amount else "cantidad por revisar"} ${m.unit.replace('|','·')} · ${m.kind.label}\n")}}
        if(materials is JSONArray){out.append("\nMateriales y herramientas\n");for(i in 0 until materials.length()){
            val m=materials.optJSONObject(i)
            if(m==null){legacy(materials.optString(i));continue}
            val type=when{m.optBoolean("tool") || m.optString("materialKind")=="TOOL"->"herramienta";m.optBoolean("reusable") || m.optString("materialKind")=="REUSABLE"->"reutilizable";else->"consumible"}
            out.append("• ${m.optString("name").replace('|','·')}: ${m.optString("amount").takeIf {m.optString("legacyIssue").isBlank()}?.ifBlank {"cantidad por revisar"} ?: "cantidad por revisar"} ${m.optString("unit").replace('|','·')} · $type\n")
        }}else if(materials is String && materials.isNotBlank()){out.append("\nMateriales\n");if('|' in materials)legacy(materials)else out.append(materials+"\n")}
        data.optJSONObject("teacherGuide")?.let {guide->
            out.append("\nDocente · Cómo enseñarlo\nGuion editable; no acredita una asociación curricular oficial.\n")
            val sections=linkedMapOf("introShort" to "Presentación breve","introExtended" to "Guion desarrollado","purpose" to "Propósito de enseñanza","motivation" to "Motivación","gradeRange" to "Grupo y grado","priorKnowledgeQuestions" to "Saberes previos","teachingSequence" to "Secuencia de enseñanza","guidedDemonstration" to "Demostración","participation" to "Participación","reflectiveQuestions" to "Preguntas","commonDifficulties" to "Dificultades frecuentes","grade1to2" to "Adaptación 1.º–2.º","grade3to4" to "Adaptación 3.º–4.º","grade5to6" to "Adaptación 5.º–6.º","inclusion" to "Diversidad","formativeEvidence" to "Evaluación formativa","feedback" to "Retroalimentación","close" to "Cierre","safety" to "Seguridad docente","sources" to "Fuentes del guion")
            sections.forEach {(key,label)->val value=guide.opt(key);val text=if(value is JSONArray)(0 until value.length()).map {value.optString(it)}.filter {it.isNotBlank()}.joinToString("\n• ",prefix="• ")else if(value is String)value else "";if(text.isNotBlank() && text!="• ")out.append("\n$label\n$text\n")}
        }
        val steps=data.optJSONArray("steps") ?: JSONArray();if(steps.length()>0)out.append("\nProcedimiento\n")
        for(i in 0 until steps.length()){val s=steps.optJSONObject(i) ?: JSONObject().put("title",steps.optString(i));out.append("${i+1}. ${s.optString("title")} [${if(s.optBoolean("done"))"completado" else "pendiente"}]\n");if(s.optString("materialsUsed").isNotBlank())out.append("Usa: ${s.optString("materialsUsed")}\n");if(s.optString("instructionNote").isNotBlank())out.append("Orientación: ${s.optString("instructionNote")}\n");if(s.optString("notes").isNotBlank())out.append("Observación: ${s.optString("notes")}\n")}
        val warnings=data.optJSONObject("safety")?.optJSONArray("warnings") ?: JSONArray();if(warnings.length()>0)out.append("\nSeguridad y preparación\n")
        for(i in 0 until warnings.length())out.append("• ${warnings.getString(i)}\n")
        if(record.kind in setOf("project","experiment"))out.append("\nActividad editorial o personal, no aval MINEDU. Revisar seguridad y adecuación docente.\n")
        val records=data.optJSONArray("records") ?: JSONArray();for(i in 0 until records.length()){val r=records.getJSONObject(i);out.append("\n${r.optString("date",r.optString("at"))} · ${r.optString("moment")} · ${r.optString("observation")}\n");if(r.optString("measurement").isNotBlank())out.append("Medida: ${r.optString("measurement")} ${r.optString("measurementUnit")}\n")}
        val links=data.optJSONArray("links") ?: JSONArray();if(links.length()>0)out.append("\nRecursos vinculados\n");for(i in 0 until links.length())out.append("• ${titles[links.optString(i)] ?: "Recurso no disponible"}\n")
        val citations=data.optJSONArray("citations") ?: JSONArray();for(i in 0 until citations.length()){val c=citations.getJSONObject(i);out.append("\nReferencia: ${titles[c.optString("documentId")] ?: c.optString("documentId")}, página ${c.optInt("pageIndex")+1}\n")}
        return out.toString()
    }
}
