package pe.profelab.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val name="profelab-persistence-test.db"
    private lateinit var db: ProfeDatabase
    private fun open()=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build()
    @Before fun before() { context.deleteDatabase(name); db=open() }
    @After fun after() { db.close(); context.deleteDatabase(name) }

    @Test fun taskAndProfileSurviveDatabaseReopen() = runBlocking {
        val repo=LocalRepository(db)
        val task=Task(newId(),"Planificar clase",dueAt=1000,createdAt=1,updatedAt=1)
        repo.saveTask(task)
        repo.setCompleted(task.id,true,2000)
        repo.saveProfile(Profile(newId(),"Profe",6,"Instituto"))
        db.close(); db=open()
        val restored=LocalRepository(db)
        assertTrue(requireNotNull(restored.findTask(task.id)).completed)
        assertEquals(6,restored.observeProfile().first()?.academicCycle)
        restored.setCompleted(task.id,false,3000)
        assertFalse(requireNotNull(restored.findTask(task.id)).completed)
    }
    @Test fun draftSurvivesReopenAndDeletePreservesActivity() = runBlocking {
        val repo=LocalRepository(db)
        val id=newId()
        repo.writeDraft(Draft("task:$id","borrador sin confirmar",20))
        db.close(); db=open()
        val restored=LocalRepository(db)
        assertEquals("borrador sin confirmar",restored.readDraft("task:$id")?.payload)
        restored.saveTask(Task(id,"Práctica",createdAt=20,updatedAt=20))
        assertNull(restored.readDraft("task:$id"))
        restored.deleteTask(id)
        assertNull(restored.findTask(id))
        assertEquals(2,restored.observeActivity().first().size)
    }
    @Test fun editingDoesNotUndoConcurrentCompletion() = runBlocking {
        val repo=LocalRepository(db)
        val original=Task(newId(),"Tarea",createdAt=1,updatedAt=1)
        repo.saveTask(original)
        repo.setCompleted(original.id,true,2)
        repo.saveTask(original.copy(title="Editada",updatedAt=3))
        assertTrue(requireNotNull(repo.findTask(original.id)).completed)
    }
    @Test fun reminderDeliveryIsBoundToDueTime() = runBlocking {
        val repo=LocalRepository(db)
        val t=Task(newId(),"Entrega",dueAt=100,reminderEnabled=true,createdAt=1,updatedAt=1)
        repo.saveTask(t); repo.markReminderDelivered(t.id,100)
        assertEquals(100L,repo.findTask(t.id)?.reminderDeliveredFor)
        repo.saveTask(t.copy(dueAt=200,updatedAt=2))
        assertNull(repo.findTask(t.id)?.reminderDeliveredFor)
    }
}
