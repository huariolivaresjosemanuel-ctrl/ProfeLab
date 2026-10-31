package pe.profelab.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class InventoryMigrationTest {
    @Test fun schemaTwoMigratesWithoutRewritingAnyHistoricalStockOrProgress():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="qa-two-three-${newId()}.db"
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("pe.profelab.core.database.ProfeDatabase/2.json").bufferedReader().use {it.readText()}).getJSONObject("database")
        val oldJson=JSONObject().put("quantity","14.00").put("reserved","10.00").put("reservationProjectId","run").put("unit","unidad").put("materialKind","CONSUMABLE").put("materialId","ligas").put("legacyMaterialsOriginal","conservar").toString()
        val projectJson="{\"steps\":[{\"id\":\"stable\",\"done\":true}],\"teacherGuide\":{\"version\":1,\"introShort\":\"Mi guion\"}}"
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object:SupportSQLiteOpenHelper.Callback(2){
            override fun onCreate(db:SupportSQLiteDatabase){
                val entities=schema.getJSONArray("entities")
                for(i in 0 until entities.length()){val e=entities.getJSONObject(i);val table=e.getString("tableName");db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table));val indices=e.getJSONArray("indices");for(n in 0 until indices.length())db.execSQL(indices.getJSONObject(n).getString("createSql").replace("\${TABLE_NAME}",table))}
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");db.execSQL("INSERT OR REPLACE INTO room_master_table VALUES (42,?)",arrayOf(schema.getString("identityHash")))
                db.execSQL("INSERT INTO workspace_records VALUES ('stock','inventory','Ligas',?,7,9,0)",arrayOf(oldJson))
                db.execSQL("INSERT INTO workspace_records VALUES ('run','project','Mi creación',?,3,4,0)",arrayOf(projectJson))
                db.execSQL("INSERT INTO profiles VALUES (1,'p','Perfil previo',6,'Centro')")
                db.execSQL("INSERT INTO drafts VALUES ('ocr-review','{\"text\":\"Conservar\"}',1)")
            }
            override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int){}
        }).build())
        var current:ProfeDatabase?=null
        try {
            helper.writableDatabase;helper.close()
            val db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).addMigrations(INVENTORY_MIGRATION_2_3).build();current=db
            assertEquals(3,db.openHelper.writableDatabase.version)
            val store=WorkspaceRepository(db);val old=requireNotNull(store.record("stock"))
            assertEquals(oldJson,old.json);assertEquals(9,old.revision);assertEquals(projectJson,store.record("run")?.json)
            assertEquals("Perfil previo",LocalRepository(db).observeProfile().first()?.name)
            assertNotNull(db.dao().draft("ocr-review"))
            store.prepareConsumption(ConsumptionRequest("after-migration","run","Uso real",listOf(ActualUsage("stock","8"))))
            store.confirmInventoryOperation("after-migration")
            assertEquals("6",JSONObject(requireNotNull(store.record("stock")).json).getString("quantity"));assertEquals(projectJson,store.record("run")?.json)
            db.close();current=null
            val reopened=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build();current=reopened
            WorkspaceRepository(reopened).confirmInventoryOperation("after-migration");assertEquals("6",JSONObject(requireNotNull(WorkspaceRepository(reopened).record("stock")).json).getString("quantity"))
        }finally{current?.close();helper.close();context.deleteDatabase(name)}
    }
}
