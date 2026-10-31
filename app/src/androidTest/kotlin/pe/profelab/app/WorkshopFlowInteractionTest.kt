package pe.profelab.app

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkshopFlowInteractionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}
    @Test fun realCopyButtonWritesClipboardWithoutCreatingStock(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val before=runBlocking {store.observeRecords().first().associate {it.id to it.json}}
        compose.openWorkshopDefinition("mat-003");compose.workshopAction("workshop-to-materials");compose.workshopAction("workshop-copy-shopping")
        val clipboard=compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Assert.assertTrue(clipboard.primaryClip?.getItemAt(0)?.text.toString().contains("Materiales faltantes para:"))
        Assert.assertEquals(before,runBlocking {store.observeRecords().first().associate {it.id to it.json}})
    }
    @Test fun cookingWithoutCompatiblePackIsDisabled(){
        compose.onNodeWithTag("nav_taller").performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-sector:cooking"))
        compose.onNodeWithTag("workshop-sector:cooking").assertIsNotEnabled()
    }
}
