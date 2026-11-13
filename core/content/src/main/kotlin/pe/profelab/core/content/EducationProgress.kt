package pe.profelab.core.content

import org.json.JSONObject
import pe.profelab.core.model.*

/** Each classroom execution has progress of its own; the reusable preparation remains intact. */
object EducationProgress {
    fun hasSelection(record:WorkRecord):Boolean {
        val prep=JSONObject(record.json).optJSONObject("teacherPreparation") ?: return false
        return prep.optString("participation")=="STUDENTS" && prep.optString("educationRunId").isNotBlank()
    }
    fun view(record:WorkRecord,all:List<WorkRecord>):ActivityViewState {
        val base=WorkshopFlowAdapter.read(record);val run=EducationData.selectedRun(record,all) ?: return base
        val payload=JSONObject(all.single {it.id==run.id}.json);val progress=payload.optJSONObject("stepProgress") ?: JSONObject()
        return base.copy(steps=base.steps.map {step->val value=progress.optJSONObject(step.id);step.copy(done=value?.optBoolean("done")==true,notes=value?.optString("notes").orEmpty())},
            lastVisitedStepId=payload.optString("lastVisitedStepId").takeIf {it in run.stepIds})
    }
    fun selectedId(record:WorkRecord):String=JSONObject(record.json).optJSONObject("teacherPreparation")?.optString("educationRunId").orEmpty()
    suspend fun change(store:WorkspaceStore,projectId:String,stepId:String,done:Boolean?=null,note:String?=null,visit:Boolean=false,expectedRunId:String?=null){
        require(note==null || note.length<=4000)
        store.editRecords {all->
            val work=all.single {it.id==projectId};val run=requireNotNull(EducationData.selectedRun(work,all)){"Selecciona una ejecución activa para guardar su progreso"}
            require(expectedRunId==null || expectedRunId==run.id){"La ejecución cambió; revisa el aula activa antes de guardar"}
            require(stepId in run.stepIds && WorkshopFlowAdapter.read(work).steps.any {it.id==stepId}){"El procedimiento cambió; crea una ejecución compatible sin modificar el historial"}
            val row=all.single {it.id==run.id};val data=JSONObject(row.json);val progress=data.optJSONObject("stepProgress") ?: JSONObject();val step=progress.optJSONObject(stepId) ?: JSONObject()
            done?.let {step.put("done",it)};note?.let {step.put("notes",it)}
            if(done!=null || note!=null)progress.put(stepId,step)
            if(visit)data.put("lastVisitedStepId",stepId)
            listOf(row.copy(json=data.put("stepProgress",progress).toString(),updatedAt=System.currentTimeMillis()))
        }
    }
}
