package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test

class AssistantPolicyTest {
    private val catalog=listOf(
        AssistantMessage("assistant.home.resume","home","Continuar trabajo","Continuar",AssistantAction.RECENT_WORK,true),
        AssistantMessage("assistant.taller.recommendations","taller","Comparar materiales","Ver",AssistantAction.RECOMMENDATIONS,true),
        AssistantMessage("assistant.taller.teacher","taller","Cómo enseñarlo","Docente",AssistantAction.TEACHER,false),
        AssistantMessage("assistant.global.no_inventory","global","Sin materiales registrados","Registrar",AssistantAction.INVENTORY,false),
        AssistantMessage("assistant.global.no_projects","global","Sin proyectos iniciados","Explorar",AssistantAction.TALLER,false)
    )
    @Test fun missingInventoryOffersRegistrationNotAnInventedRecommendation(){val result=AssistantPolicy.advice(catalog,"taller",AssistantFacts(),AssistantAction.entries.toSet());assertTrue(result.messages.any {it.action==AssistantAction.INVENTORY});assertFalse(result.messages.any {it.action==AssistantAction.RECOMMENDATIONS})}
    @Test fun actualInventoryAllowsExistingRecommendationHandler(){assertTrue(AssistantPolicy.advice(catalog,"taller",AssistantFacts(hasInventory=true),setOf(AssistantAction.RECOMMENDATIONS)).messages.any {it.action==AssistantAction.RECOMMENDATIONS})}
    @Test fun actionsWithoutHandlersAreNotRendered(){assertTrue(AssistantPolicy.advice(catalog,"taller",AssistantFacts(hasInventory=true),emptySet()).messages.isEmpty())}
    @Test fun currentProjectIsRequiredByTheRegisteredTeacherHandler(){assertFalse(AssistantPolicy.advice(catalog,"taller",AssistantFacts(),setOf(AssistantAction.INVENTORY)).messages.any {it.action==AssistantAction.TEACHER});assertTrue(AssistantPolicy.advice(catalog,"taller",AssistantFacts(hasCurrentProject=true),setOf(AssistantAction.TEACHER)).messages.any {it.action==AssistantAction.TEACHER})}
    @Test fun noPendingWorkDoesNotClaimThatCompletedProjectsNeverExisted(){val result=AssistantPolicy.advice(catalog,"home",AssistantFacts(hasProjects=true),AssistantAction.entries.toSet());assertFalse(result.messages.any {it.id=="assistant.global.no_projects"});assertTrue(result.notices.contains("No hay trabajos pendientes para continuar."))}
    @Test fun emptyDocumentsAndShoppingHaveExplicitNotices(){assertTrue(AssistantPolicy.advice(catalog,"library",AssistantFacts(),emptySet()).notices.any {it.contains("PDF propios")});assertTrue(AssistantPolicy.advice(catalog,"taller",AssistantFacts(),emptySet()).notices.any {it.contains("compras")})}
    @Test fun immersiveAndUnknownContextsHaveNoAssistant(){for(context in listOf("pdf","ocr","capture","global","unknown"))assertTrue(AssistantPolicy.advice(catalog,context,AssistantFacts(),AssistantAction.entries.toSet()).messages.isEmpty())}
}
