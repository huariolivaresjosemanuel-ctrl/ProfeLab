package pe.profelab.core.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.model.EducationalSourceRepository
import pe.profelab.core.model.SourceReference

/** Read-only metadata. Never downloads a PDF or derives rights from a mockup. */
class BundledSourceCatalog(private val context: Context) : EducationalSourceRepository {
    override suspend fun sources(): List<SourceReference> = withContext(Dispatchers.IO) {
        val data = context.assets.open("catalog/minedu_v2.json").bufferedReader().use { it.readText() }
        val root = JSONObject(data)
        require(root.getInt("catalog_schema_version") == 2)
        val docs = root.getJSONArray("docs")
        val result = (0 until docs.length()).map { i ->
            val d = docs.getJSONObject(i)
            require(!d.getBoolean("pdf_binary_included") && !d.getBoolean("distribution_approved"))
            SourceReference(d.getString("id"),d.getString("title"),d.getInt("year"),d.getString("author"),
                d.getString("area"),d.getString("official_page"),d.getString("description"),d.getString("rights_note"))
        }
        require(result.map { it.id }.distinct().size == result.size)
        result
    }
}
