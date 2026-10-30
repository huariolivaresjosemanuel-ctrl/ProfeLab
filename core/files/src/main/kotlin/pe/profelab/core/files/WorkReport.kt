package pe.profelab.core.files

import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.WorkRecord

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
        "variants" to "Variantes","alternatives" to "Alternativas y justificación")
    fun render(record:WorkRecord,titles:Map<String,String>):String {
        val data=JSONObject(record.json);val out=StringBuilder(record.title+"\n")
        labels.forEach {(key,label)->val value=data.opt(key);if(value!=null && value!=JSONObject.NULL && value.toString().isNotBlank())out.append("\n$label\n$value\n")}
        if(record.kind=="session")out.append("\nCotejo curricular: ${if(data.optBoolean("curriculumVerifiedByUser"))"declarado por el usuario; ProfeLab no lo acredita" else "pendiente"}. Elaboración personal, no documento oficial.\n")
        val materials=data.opt("materials")
        if(materials is JSONArray){out.append("\nMateriales y herramientas\n");for(i in 0 until materials.length()){val m=materials.getJSONObject(i);out.append("• ${m.optString("name")}: ${m.optString("amount")} ${m.optString("unit")}\n")}}
        else if(materials is String && materials.isNotBlank())out.append("\nMateriales\n$materials\n")
        val steps=data.optJSONArray("steps") ?: JSONArray();if(steps.length()>0)out.append("\nProcedimiento\n")
        for(i in 0 until steps.length()){val s=steps.getJSONObject(i);out.append("${i+1}. ${s.optString("title")} [${if(s.optBoolean("done"))"completado" else "pendiente"}]\n")}
        val warnings=data.optJSONObject("safety")?.optJSONArray("warnings") ?: JSONArray();if(warnings.length()>0)out.append("\nSeguridad y preparación\n")
        for(i in 0 until warnings.length())out.append("• ${warnings.getString(i)}\n")
        if(record.kind in setOf("project","experiment"))out.append("\nActividad editorial o personal, no aval MINEDU. Revisar seguridad y adecuación docente.\n")
        val records=data.optJSONArray("records") ?: JSONArray();for(i in 0 until records.length()){val r=records.getJSONObject(i);out.append("\n${r.optString("at")} · ${r.optString("observation")}\n")}
        val citations=data.optJSONArray("citations") ?: JSONArray();for(i in 0 until citations.length()){val c=citations.getJSONObject(i);out.append("\nReferencia: ${titles[c.optString("documentId")] ?: c.optString("documentId")}, página ${c.optInt("pageIndex")+1}\n")}
        return out.toString()
    }
}
