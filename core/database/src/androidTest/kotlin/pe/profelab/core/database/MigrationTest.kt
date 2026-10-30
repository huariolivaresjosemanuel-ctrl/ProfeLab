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

@RunWith(AndroidJUnit4::class)
class MigrationTest {
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
