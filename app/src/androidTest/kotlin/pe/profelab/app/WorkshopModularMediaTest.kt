package pe.profelab.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import java.security.MessageDigest

/** Exercises the APK's real alias index and path opener. No filesystem migration is simulated. */
@RunWith(AndroidJUnit4::class)
class WorkshopModularMediaTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun index()=context.assets.open("profelab/media_index_v1.json").bufferedReader().use {JSONObject(it.readText())}
    private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}

    @Test fun allLegacyNamesOpenTheSameBytesAsTheirModularTargets(){
        val root=index();val aliases=root.getJSONObject("aliases");val resources=root.getJSONArray("resources")
        assertEquals(199,aliases.length());assertEquals(199,resources.length())
        for(i in 0 until resources.length()){
            val row=resources.getJSONObject(i);val old=row.getString("legacyAssetFilename");val target=row.getString("assetPath")
            assertTrue(WorkshopAssetPaths.legacyName(old));assertTrue(WorkshopAssetPaths.validModular(target))
            assertEquals(target,aliases.getString(old))
            val modern=WorkshopAssetPaths.open(context,target).use {it.readBytes()}
            val legacy=WorkshopAssetPaths.open(context,old).use {it.readBytes()}
            assertArrayEquals(legacy,modern);assertEquals(row.getString("sha256"),hash(modern))
        }
    }

    @Test fun invalidPathsCannotEscapeTheApprovedAssetRoots(){
        val invalid=listOf("../workshop/images/image.webp","/profelab/taller/image.webp","C:/private/image.webp",
            "profelab/../secret.webp","profelab/taller//image.webp","profelab/taller/./image.webp",
            "profelab/taller/%2e%2e/image.webp","profelab\\taller\\image.webp","documents/private-photo",
            "https://example.invalid/image.webp","file:///data/user/0/private.webp")
        invalid.forEach {path->
            assertFalse(path,WorkshopAssetPaths.validModular(path));assertFalse(path,WorkshopAssetPaths.legacyName(path))
            assertTrue("Invalid path must be rejected by the actual opener: $path",runCatching {WorkshopAssetPaths.open(context,path).use {it.read()}}.isFailure)
        }
        val unlisted="profelab/shared/materials/not-listed-in-the-index.webp"
        assertTrue(WorkshopAssetPaths.validModular(unlisted))
        assertTrue("A syntactically safe path still needs an index entry",runCatching {WorkshopAssetPaths.canonical(context,unlisted)}.isFailure)
    }

    @Test fun anOlderSavedBindingStillResolvesWithoutChangingItsPayload():Unit=runBlocking {
        val definition=WorkshopSeed(context).templates().single {it.id=="exp-001"}
        val data=JSONObject(definition.json);val refs=data.getJSONArray("editorialMediaBindings")
        val original=(0 until refs.length()).map {refs.getJSONObject(it)}.first {it.getString("role")=="step_photo"}
        val old=JSONObject(original.toString()).put("assetFilename",original.getString("legacyAssetFilename"))
        old.remove("assetPath")
        data.put("forkedFromTemplateId",definition.id).put("results","Conservar el resultado propio").put("editorialMediaBindings",JSONArray().put(old))
        val personal=definition.copy(id="legacy-saved-run",kind="experiment",json=data.toString());val before=personal.json
        val resolved=requireNotNull(WorkshopMediaBindings.resolve(personal,"step_photo",stepId=old.getString("stepId")))
        assertTrue(resolved.asset)
        assertEquals(old.getString("sha256"),hash(WorkshopAssetPaths.open(context,resolved.storageId).use {it.readBytes()}))
        assertEquals(before,personal.json)
        val mixed=JSONObject(old.toString()).put("fileId","a-private-document")
        assertNull(WorkshopMediaBindings.resolve(personal.copy(json=JSONObject(data.toString()).put("editorialMediaBindings",JSONArray().put(mixed)).toString()),"step_photo",stepId=old.getString("stepId")))
    }
}
