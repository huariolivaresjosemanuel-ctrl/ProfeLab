package pe.profelab.core.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord

class WorkshopSeed(private val context:Context) {
    suspend fun templates():List<WorkRecord> = withContext(Dispatchers.IO) {
        listOf("workshop/projects.json" to "project_template","workshop/experiments.json" to "experiment_template").flatMap { (path,kind)->
            val pack=context.assets.open(path).bufferedReader().use {JSONObject(it.readText())}
            val projects=pack.getJSONArray("projects")
            (0 until projects.length()).map {i->val j=projects.getJSONObject(i)
                WorkRecord(j.getString("id"),kind,j.getString("title"),j.toString(),0)
            }
        }
    }
}
