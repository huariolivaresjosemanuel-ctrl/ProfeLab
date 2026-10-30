package pe.profelab.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun historicalSchemaOneOpensWithRoomValidationAndReopensWithoutLoss():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="qa-migration-${newId()}.db"
        val schema=InstrumentationRegistry.getInstrumentation().context.assets.open("pe.profelab.core.database.ProfeDatabase/1.json").bufferedReader().use {JSONObject(it.readText()).getJSONObject("database")}
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object:SupportSQLiteOpenHelper.Callback(1){
            override fun onCreate(db:SupportSQLiteDatabase){
                val entities=schema.getJSONArray("entities")
                for(i in 0 until entities.length()){
                    val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                    val indices=entity.optJSONArray("indices") ?: JSONArray();for(n in 0 until indices.length())db.execSQL(indices.getJSONObject(n).getString("createSql").replace("\${TABLE_NAME}",table))
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES (42,?)",arrayOf(schema.getString("identityHash")))
                db.execSQL("INSERT INTO tasks VALUES ('old-task','Conservar tarea','Notas',null,'NORMAL',null,0,null,null,null,1,1)")
                db.execSQL("INSERT INTO profiles VALUES (1,'old-profile','Nombre previo',4,'Institución previa')")
                db.execSQL("INSERT INTO drafts VALUES ('old-draft','{}',1)")
                db.execSQL("INSERT INTO activity VALUES ('old-event','TASK','old-task','Actividad previa',1)")
            }
            override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int){}
        }).build())
        try {
            helper.writableDatabase;helper.close()
            var current=Room.databaseBuilder(context,ProfeDatabase::class.java,name).addMigrations(ProfeDatabase.MIGRATION_1_2).build()
            try {
                // Opening invokes Room's complete entity/index schema validation after the real migration.
                assertEquals(2,current.openHelper.writableDatabase.version)
                assertEquals("Conservar tarea",LocalRepository(current).findTask("old-task")?.title)
                assertEquals("Nombre previo",LocalRepository(current).observeProfile().first()?.name)
                assertNotNull(current.dao().draft("old-draft"))
                assertEquals("Actividad previa",current.dao().observeActivity().first().single().label)
                WorkspaceRepository(current).saveRecord(WorkRecord("new-work","project","Nuevo trabajo","{}",2))
            }finally{current.close()}
            current=Room.databaseBuilder(context,ProfeDatabase::class.java,name).addMigrations(ProfeDatabase.MIGRATION_1_2).build()
            try {assertNotNull(LocalRepository(current).findTask("old-task"));assertEquals("Nuevo trabajo",WorkspaceRepository(current).record("new-work")?.title)}finally{current.close()}
        }finally{helper.close();context.deleteDatabase(name)}
    }
    @Test fun migrationAddsWorkspaceWithoutRemovingExistingTables(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object:SupportSQLiteOpenHelper.Callback(1){
            override fun onCreate(db:SupportSQLiteDatabase){db.execSQL("CREATE TABLE sentinel (id TEXT PRIMARY KEY, value TEXT NOT NULL)");db.execSQL("INSERT INTO sentinel VALUES ('existing','preserved')")}
            override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int){}
        }).build())
        val db=helper.writableDatabase
        ProfeDatabase.MIGRATION_1_2.migrate(db)
        db.query("SELECT value FROM sentinel WHERE id='existing'").use {assertTrue(it.moveToFirst());assertEquals("preserved",it.getString(0))}
        db.query("SELECT * FROM workspace_records").use {assertEquals(0,it.count)}
        helper.close()
    }
}
