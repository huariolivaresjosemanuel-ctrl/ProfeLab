package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

internal fun ComposeTestRule.openWorkshopDefinition(id:String="mat-001",sector:String="taller"){
    onNodeWithTag("nav_taller").performClick()
    waitUntil(15000){onAllNodesWithTag("workshop-sector:$sector").fetchSemanticsNodes().isNotEmpty()}
    onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-sector:$sector"))
    onNodeWithTag("workshop-sector:$sector").performClick()
    waitUntil(15000){onAllNodesWithTag("workshop-catalog-ready").fetchSemanticsNodes().isNotEmpty()}
    onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-open:$id"))
    onNodeWithTag("workshop-open:$id").assertIsDisplayed().performClick()
    waitUntil(15000){onAllNodesWithTag("workshop-guide:$id").fetchSemanticsNodes().isNotEmpty()}
}
internal fun ComposeTestRule.workshopAction(tag:String){
    onNodeWithTag("workshop-page").performScrollToNode(hasTestTag(tag))
    onNodeWithTag(tag).performClick()
    waitForIdle()
}
