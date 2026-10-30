package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*

object TeacherGuides {
    data class Field(val key:String,val label:String,val multiple:Boolean=false)
    val fields=listOf(
        Field("introShort","Presentación oral breve · 20–60 segundos"),Field("introExtended","Guion desarrollado"),
        Field("purpose","Propósito de enseñanza"),Field("motivation","Motivación y contexto"),Field("gradeRange","Grado y contexto del grupo"),
        Field("priorKnowledgeQuestions","Saberes previos",true),Field("teachingSequence","Secuencia de enseñanza",true),
        Field("guidedDemonstration","Demostración guiada",true),Field("participation","Participación y organización"),
        Field("reflectiveQuestions","Preguntas para pensar",true),Field("commonDifficulties","Dificultades y errores frecuentes",true),
        Field("grade1to2","Adaptación propuesta · 1.º–2.º"),Field("grade3to4","Adaptación propuesta · 3.º–4.º"),Field("grade5to6","Adaptación propuesta · 5.º–6.º"),
        Field("inclusion","Atención a la diversidad"),Field("formativeEvidence","Evidencias y evaluación formativa",true),
        Field("feedback","Retroalimentación"),Field("close","Cierre y reflexión"),Field("safety","Preparación y seguridad del adulto"),Field("sources","Fuentes del guion",true)
    )
    fun validate(j:JSONObject){
        require(j.optInt("version",1)==1)
        fields.forEach {f->if(j.has(f.key)){
            val value=j.get(f.key)
            if(f.multiple){require(value is JSONArray && value.length()<=40);for(i in 0 until value.length())require(value.get(i) is String && value.getString(i).length<=10000)}
            else require(value is String && value.length<=20000)
        }}
        require(j.toString().length<=150000)
    }
    fun defaultFor(record:WorkRecord):JSONObject {
        val data=JSONObject(record.json);val experiment=record.kind.contains("experiment") || data.optString("categoryId")=="experiment"
        val title=record.title.ifBlank {"esta actividad"}
        val practical=data.optJSONObject("pedagogy")?.optJSONObject("practical")
        val purpose=data.optString("objectiveSuggested").ifBlank {practical?.optString("purpose").orEmpty()}.ifBlank {"Define qué podrá hacer o explicar el grupo después de la actividad."}
        val brief=if(experiment)"Hoy investigaremos: $title Antes de comenzar, compartiremos nuestras ideas y formularemos una hipótesis. Observaremos qué cambia y qué mantenemos igual. Cada equipo registrará lo que realmente ocurra, con dibujos, medidas o fotografías autorizadas. Al final compararemos las evidencias y explicaremos qué podemos concluir y qué preguntas siguen abiertas. No necesitamos adivinar una respuesta correcta; necesitamos observar con cuidado y trabajar con seguridad."
            else "Hoy trabajaremos con $title. Primero observaremos el recurso y compartiremos lo que ya sabemos. Después les mostraré una acción sencilla y tendrán tiempo para probar, explicar y comparar sus ideas. Podemos participar hablando, dibujando o trabajando con otra persona. Al terminar, cada equipo mostrará algo que pudo hacer y explicará cómo lo hizo. Antes de usar los materiales revisaremos juntos las indicaciones de seguridad y qué acciones corresponden únicamente al adulto."
        val j=JSONObject().put("version",1).put("status","EDITORIAL_PROPOSAL").put("introShort",brief).put("purpose",purpose)
            .put("introExtended","Presenta el propósito: $purpose\nInvita a observar el recurso sin anticipar respuestas. Recupera sus explicaciones, demuestra una acción segura y ofrece tiempo para ensayar. Acompaña con preguntas y recoge evidencias antes del cierre.")
            .put("motivation","Relaciona la propuesta con una situación cercana y permite preguntas del grupo.")
            .put("gradeRange",WorkshopCodec.strings(data.opt("grades")).joinToString(", ").let {if(it.isBlank())"Adaptar al grupo; sin correspondencia curricular acreditada"else "Propuesta para $it de Primaria; adaptar al contexto"})
            .put("priorKnowledgeQuestions",JSONArray(listOf("¿Qué conocemos de esta situación?","¿Qué nos gustaría averiguar o aprender a hacer?")))
            .put("teachingSequence",JSONArray(if(experiment)listOf("Plantear una pregunta investigable sin adelantar la conclusión","Recoger hipótesis y acordar qué observar o medir","Realizar el procedimiento seguro y registrar datos propios","Comparar evidencias, reconocer límites y comunicar una conclusión")else listOf("Presentar el recurso y recuperar saberes previos","Demostrar una acción segura con explicación breve","Proponer un reto y acompañar la participación","Compartir estrategias, observar evidencias y cerrar")))
            .put("guidedDemonstration",JSONArray(listOf("Prepara y revisa previamente materiales, herramientas y riesgos.","Muestra una acción del procedimiento y pide que expliquen qué observaron.","Invita a participar solo en las acciones seguras para el grupo.")))
            .put("participation","Acuerda turnos y roles flexibles; permite participar en pareja, de forma oral, gráfica o con apoyos.")
            .put("reflectiveQuestions",JSONArray(listOf(if(experiment)"¿Qué dato apoya esa explicación?"else "¿Cómo lo hiciste y cómo puedes explicarlo?","¿Qué cambió? ¿Qué otra estrategia podríamos probar?")))
            .put("commonDifficulties",JSONArray(listOf("Si la consigna no se comprende, muestra una sola acción y pide que la expliquen con sus palabras.","Si hay respuestas distintas, pide ejemplos y evidencias antes de corregir.")))
            .put("grade1to2","Reduce el número de acciones; usa demostración concreta, comparación oral y registro gráfico acompañado.")
            .put("grade3to4","Propón comparar estrategias o datos y organizar una explicación con dibujos, tablas o ejemplos.")
            .put("grade5to6","Invita a justificar decisiones, revisar variables o condiciones y reconocer límites de sus conclusiones.")
            .put("inclusion","Ofrece materiales de mayor agarre, apoyos visuales, más tiempo y diferentes formas de responder, sin exigir diagnósticos.")
            .put("formativeEvidence",JSONArray(listOf(practical?.optString("whatToObserve").orEmpty().ifBlank {"Describe una acción realizada y presenta una evidencia observable."},"Escucha una explicación y registra qué apoyo necesita para continuar.")))
            .put("feedback","Describe lo observado y plantea una pregunta que ayude a revisar la estrategia; evita comparar personas o prometer resultados.")
            .put("close",if(experiment)"¿Qué observamos realmente, qué podemos concluir y qué necesitaríamos investigar después?"else "¿Qué aprendimos a hacer, cómo lo explicamos y dónde podríamos usarlo?")
            .put("safety","Revisa las restricciones de la ficha. El adulto prepara herramientas y acciones restringidas; una guía editorial no sustituye esa revisión.")
            .put("sources",JSONArray())
        return j
    }
    /** Existing user values, including intentionally blank fields, take precedence over fallback. */
    fun resolve(record:WorkRecord):JSONObject {
        val defaults=defaultFor(record);val existing=JSONObject(record.json).optJSONObject("teacherGuide")
        existing?.keys()?.forEach {key->defaults.put(key,existing.get(key))}
        return defaults
    }
    fun read(record:WorkRecord):TeachingGuide {
        val data=resolve(record)
        return TeachingGuide(data.optInt("version",1),data.optString("status","USER_DRAFT"),fields.map {f->
            TeachingBlock(f.key,f.label,if(f.multiple)WorkshopCodec.strings(data.opt(f.key))else listOf(data.optString(f.key)).filter {it.isNotBlank()},f.multiple)
        })
    }
}
