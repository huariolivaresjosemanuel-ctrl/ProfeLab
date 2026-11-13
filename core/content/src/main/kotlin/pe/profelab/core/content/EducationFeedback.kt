package pe.profelab.core.content

/** Deterministic wording from saved records only; never diagnoses, grades or extrapolates a group result. */
object EducationFeedback {
    fun summary(run:EduRun,target:EduTarget,observations:List<EduObservation>):String {
        val valid=when(target.kind){EduTargetKind.STUDENT->target.id in run.participantIds;EduTargetKind.TEAM->run.teams.any {it.teamId==target.id};EduTargetKind.ACTIVITY->target.id==null}
        if(!valid)return "La selección no pertenece a esta ejecución. Revisa el destinatario."
        val rows=observations.filter {it.runId==run.id && it.classroomId==run.classroomId && it.target==target && !it.archived}.sortedBy {it.at}
        if(rows.isEmpty())return "No hay observaciones registradas para esta selección."
        val name=when(target.kind){EduTargetKind.STUDENT->run.participants.first {it.studentId==target.id}.displayName;EduTargetKind.TEAM->"Equipo: "+run.teams.first {it.teamId==target.id}.name;EduTargetKind.ACTIVITY->"Actividad"}
        return buildString {
            append(name).append(" · ").append(run.activityTitle.take(300)).append('\n')
            if(target.kind==EduTargetKind.TEAM)append("Observación del equipo; no atribuye el mismo desempeño a cada integrante.\n")
            val recent=rows.takeLast(30)
            if(recent.size<rows.size)append("Se muestran los últimos ").append(recent.size).append(" registros de ").append(rows.size).append(". Consulta el historial para revisar los anteriores.\n")
            recent.forEach {row->
                append("• ").append(row.criterion.label.take(180)).append(": ").append(row.status.label).append('.')
                if(row.note.isNotBlank())append(' ').append(row.note.take(400))
                if(row.note.length>400 || row.criterion.label.length>180)append(" (extracto; revisa el registro completo)")
                append('\n')
            }
            append("Síntesis de registros de esta ejecución; no constituye una calificación oficial ni una conclusión definitiva.")
        }
    }
}
