package pe.profelab.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.model.*
import pe.profelab.core.ui.ProfeTheme
import pe.profelab.feature.students.StudentsWorkspace

/** Isolated RAM fixtures: these tests never write to the user's application database. */
@RunWith(AndroidJUnit4::class)
class Education32StudentUiTest {
    @get:Rule val compose=createComposeRule()
    private class MemoryStore:WorkspaceStore {
        val rows=MutableStateFlow<List<WorkRecord>>(emptyList());private val mutex=Mutex()
        override fun observeRecords()=rows
        override suspend fun record(id:String)=rows.value.find {it.id==id}
        override suspend fun saveRecord(record:WorkRecord)=saveBatch(listOf(record))
        override suspend fun archiveRecord(id:String){editRecords {all->listOf(all.single {it.id==id}.copy(archived=true))}}
        override suspend fun saveBatch(records:List<WorkRecord>){editRecords {records}}
        override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>){mutex.withLock {val updates=change(rows.value);val ids=updates.map {it.id}.toSet();rows.value=rows.value.filter {it.id !in ids}+updates}}
    }
    private data class Fixture(val store:MemoryStore,val room:String,val a:String,val b:String,val guardianA:String)
    private fun fixture():Fixture=runBlocking {
        val store=MemoryStore();val old=EducationRepository(store);val repo=Education32Repository(store)
        val room=old.saveClassroom("Aula UI QA",2,"A",2027)
        val a=old.saveStudent(room,"Alias UI A");val b=old.saveStudent(room,"Alias UI B")
        repo.saveOffering(room,"Matemática")
        val guardian=repo.saveGuardian(a,"Contacto UI A","Tutor","","",EduFamilyChannel.EMAIL,false,"LEARNING_REPORT",0,Long.MAX_VALUE,false)
        Fixture(store,room,a,b,guardian)
    }
    private fun show(f:Fixture,student:State<String>?=null){compose.setContent {
        val all by f.store.rows.collectAsState()
        val current=student?.value ?: f.a
        ProfeTheme {key(current){StudentsWorkspace(all,f.store,{},initialClassroomId=f.room,initialStudentId=current)}}
    }}
    private fun click(text:String){compose.onNode(hasText(text) and hasClickAction()).performScrollTo().performClick()}
    private fun input(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}

    @Test fun academicAssessmentCanBeConfirmedWithoutAnyWorkshopAndKeepsOriginalNumericScale(){
        val f=fixture();show(f)
        click("Registrar evaluación")
        click("Matemática")
        input("Competencia o propósito","Propósito docente QA")
        input("Criterio evaluado","Explica su procedimiento")
        input("Periodo: bimestre, trimestre u otro","Primer bimestre")
        click("Tarea")
        click("Continuar con evidencia y valoración")
        click("Numérica 0–20 (escala elegida por el docente)")
        input("Valor original (0–20)","17.5")
        input("Evidencia: qué trabajo o desempeño revisaste","Revisión individual del cuaderno de práctica")
        click("Revisar")
        input("Docente que registra y confirma","Docente UI QA")
        compose.onNodeWithText("Confirmar y guardar evaluación").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("academic-review-confirmation").performScrollTo().performClick()
        click("Confirmar y guardar evaluación")
        compose.waitUntil(5000){Education32Data.academicAssessments(f.store.rows.value).size==1}
        val row=Education32Data.academicAssessments(f.store.rows.value).single()
        assertEquals(EduOriginalScale.NUMERIC_0_20,row.scale);assertEquals("17.5",row.value)
        assertEquals(EduAssessmentOrigin.MANUAL,row.origin);assertNull(row.sourceRecordId)
        assertTrue(EducationData.runs(f.store.rows.value).isEmpty())
        assertTrue(Education32Data.judgments(f.store.rows.value).isEmpty())
    }

    @Test fun familyPreviewHasNoSelectedRecipientAndCannotOpenExternalAppWithoutPermission(){
        val f=fixture();show(f)
        click("Familia");click("Preparar informe individual")
        compose.onNodeWithTag("family-contact:${f.guardianA}").performScrollTo().assertIsNotSelected()
        compose.onNodeWithText("Revisar y abrir aplicación externa").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("family-contact:${f.guardianA}").performScrollTo().performClick()
        click("Correo")
        compose.onAllNodesWithText("Falta verificar la autorización del contacto.").onFirst().assertExists()
        compose.onNodeWithText("Revisar y abrir aplicación externa").performScrollTo().assertIsNotEnabled()
        assertTrue(Education32Data.drafts(f.store.rows.value).isEmpty())
    }

    @Test fun switchingPermanentStudentIdentityDoesNotExposePreviousContactOrDraft(){
        val f=fixture();val current=mutableStateOf(f.a);show(f,current)
        click("Familia")
        compose.onNodeWithText("Contacto UI A · Tutor").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {current.value=f.b}
        click("Familia")
        compose.onNodeWithText("Contacto UI A · Tutor").assertDoesNotExist()
        compose.onNodeWithText("Sin contactos registrados. Teléfono y correo son opcionales al guardar.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preparar informe individual").performScrollTo().assertIsNotEnabled()
    }
}
