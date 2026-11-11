package pe.profelab.app

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import java.security.MessageDigest

/** Actual bundled Work resources, not synthetic photos or screenshot fixtures. Requires Android. */
@RunWith(AndroidJUnit4::class)
class WorkshopVisualBankTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun registry():JSONArray=context.assets.open("workshop/media_slots_v1.json").bufferedReader().use {JSONObject(it.readText()).getJSONArray("resources")}
    private fun rows(a:JSONArray)=(0 until a.length()).map {a.getJSONObject(it)}
    private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}

    @Test fun allTwentyDefinitionsResolveTheirActualCoverMaterialsAndEightySteps():Unit=runBlocking {
        val seed=WorkshopSeed(context);val definitions=seed.templates();val resources=rows(registry());val catalog=seed.materials()
        assertEquals(20,definitions.size);assertEquals(229,resources.size)
        assertEquals(80,resources.count {it.getString("role")=="step_photo"})
        assertEquals(45,resources.filter {it.getString("role")=="material_icon"}.map {it.getString("materialId")}.toSet().size)
        for(definition in definitions){
            val before=definition.json;val selected=resources.filter {it.getString("contentId")==definition.id}
            listOf("catalog_cover","detail_hero","outcome_reference","teacher_reference").forEach {role->assertEquals(1,selected.count {it.getString("role")==role})}
            assertEquals(WorkshopFlowAdapter.read(definition).steps.map {it.id}.toSet(),selected.filter {it.getString("role")=="step_photo"}.map {it.getString("stepId")}.toSet())
            assertEquals(WorkshopCodec.materials(JSONObject(definition.json),catalog).map {it.materialId}.toSet(),selected.filter {it.getString("role")=="material_icon"}.map {it.getString("materialId")}.toSet())
            for(row in selected){
                val resolved=requireNotNull(WorkshopMediaBindings.resolve(definition,row.getString("role"),row.optString("stepId").takeIf {it.isNotBlank()},row.optString("materialId").takeIf {it.isNotBlank()})){"Unresolved approved slot: $row"}
                assertTrue(resolved.asset);assertEquals(row.getString("mediaId"),resolved.mediaId)
                assertEquals(row.getString("assetFilename"),resolved.storageId);assertEquals(row.getString("sha256"),resolved.sha256)
            }
            assertEquals("Reading slots must not modify the public definition",before,definition.json)
        }
    }

    @Test fun everyBundledOptimizedFileMatchesItsHashAndDecodesOnAndroid(){
        val helper=WorkshopBundledMedia(context)
        for(row in rows(registry()).distinctBy {it.getString("assetFilename")}){
            assertTrue("Rejected bundled file: ${row.getString("assetFilename")}",helper.verifiesAsset(row))
            assertTrue(row.getString("assetFilename").endsWith(".webp"))
            val bytes=context.assets.open("workshop/images/${row.getString("assetFilename")}").use {it.readBytes()}
            assertEquals(row.getString("sha256"),hash(bytes))
            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            assertEquals(row.getInt("width"),bounds.outWidth);assertEquals(row.getInt("height"),bounds.outHeight);assertEquals("image/webp",bounds.outMimeType)
            val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply {inSampleSize=4})
            assertNotNull("Header alone must not accept an undecodable file",bitmap);requireNotNull(bitmap).recycle()
        }
    }

    @Test fun missingCorruptMetadataAndUnsafePathsStayRejectedAfterTheValidFileWasCached(){
        val helper=WorkshopBundledMedia(context);val valid=registry().getJSONObject(0)
        assertTrue(helper.verifiesAsset(valid))
        listOf("sha256" to "0".repeat(64),"width" to (valid.getInt("width")+1),"mime" to "image/png",
            "assetFilename" to "missing-work-file.webp","assetFilename" to "../media_slots_v1.json").forEach {(key,value)->
            assertFalse("Cached file must not validate a different declaration: $key",helper.verifiesAsset(JSONObject(valid.toString()).put(key,value)))
        }
        assertTrue(helper.verifiesAsset(valid))
    }

    @Test fun readOnlyOverlaysKeepPersonalResultsAndRejectAChangedProcedure():Unit=runBlocking {
        val definitions=WorkshopSeed(context).templates();val definition=definitions.single {it.id=="exp-001"}
        val overlays=WorkshopBundledMedia.overlays(definitions)
        val json=JSONObject(definition.json).also {it.remove("editorialMediaBindings")}
            .put("forkedFromTemplateId",definition.id).put("results","Observación propia").put("conclusion","Conclusión propia")
            .put("attachments",JSONArray().put(JSONObject().put("id","private-photo").put("mime","image/png")))
            .put("workshopFlow",JSONObject().put("lastVisitedStepId","step-1").put("preferredCoverFileId","private-photo"))
        val personal=WorkRecord("personal-existing-run","experiment","Mi registro",json.toString(),1);val before=personal.json
        assertNotNull(WorkshopMediaBindings.resolve(personal,"step_photo","step-0",overlays=overlays))
        assertEquals("private-photo",WorkshopMediaBindings.preferredPrivateCover(personal));assertEquals(before,personal.json)
        val changed=JSONObject(personal.json);changed.getJSONArray("steps").getJSONObject(0).put("title","Un procedimiento distinto")
        assertNull(WorkshopMediaBindings.resolve(personal.copy(json=changed.toString()),"step_photo","step-0",overlays=overlays))
        assertNull(WorkshopMediaBindings.resolve(personal,"step_photo","unknown-step",overlays=overlays))
        assertNull(WorkshopMediaBindings.resolve(personal.copy(json=JSONObject(personal.json).put("contentVersion","legacy").toString()),"step_photo","step-0",overlays=overlays))
        assertEquals(before,personal.json)
    }
}
