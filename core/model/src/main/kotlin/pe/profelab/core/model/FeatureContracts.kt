package pe.profelab.core.model

/** Contracts for H2-H7. These are not registered as available services in H1. */
data class ProjectFilter(val grade: Int? = null, val area: String? = null, val text: String = "")
data class ProjectSummary(val id: String, val title: String, val reviewStatus: String)
data class MaterialShortage(val materialId: String, val quantityDecimal: String, val unit: String)
data class AvailabilityResult(val canBuild: Boolean, val missing: List<MaterialShortage>, val safetyReviewed: Boolean)
interface ProjectCatalog { suspend fun query(filter: ProjectFilter): Outcome<List<ProjectSummary>> }
interface FindMaterialProjects { suspend fun find(inventoryId: String): Outcome<List<ProjectSummary>> }
interface InventoryResolver { suspend fun evaluate(projectId: String): Outcome<AvailabilityResult> }
data class DocumentRef(val id: String, val sha256: String, val pageCount: Int?)
enum class OpenMode { READ_TEMPORARY, KEEP_PRIVATE_COPY }
interface OpenDocument { suspend fun open(contentUri: String, mode: OpenMode): Outcome<DocumentRef> }
interface AttachResource { suspend fun attach(owner: EntityRef, resource: EntityRef): Outcome<Unit> }
interface AddObservation { suspend fun add(owner: EntityRef, anonymousText: String): Outcome<String> }
data class NormalizedPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) }
}
enum class AnnotationKind { HIGHLIGHT, UNDERLINE, PEN, NOTE }
enum class AnnotationOrigin { TEXT_LAYER, MANUAL, OCR_REVIEWED }
data class PdfAnnotation(
    val id: String, val documentId: String, val sourceSha256: String, val pageIndex: Int,
    val kind: AnnotationKind, val points: List<NormalizedPoint>, val colorArgb: Long,
    val rotation: Int, val origin: AnnotationOrigin, val comment: String = "", val revision: Int = 1,
) { init { require(pageIndex >= 0 && rotation in setOf(0, 90, 180, 270) && revision > 0) } }
interface PdfAnnotationRepository { suspend fun add(annotation: PdfAnnotation): Outcome<Unit> }
data class OcrDraft(val id: String, val sourceUri: String, val rawText: String, val reviewedText: String?, val engine: String)
interface OcrEngine { suspend fun recognize(localImageUri: String): Outcome<OcrDraft> }
interface DocumentCapture { suspend fun capture(): Outcome<String> }
data class PackPreview(val packId: String, val version: String, val additions: Int, val conflicts: List<String>)
interface ContentPackInstaller {
    suspend fun preview(contentUri: String): Outcome<PackPreview>
    suspend fun confirm(preview: PackPreview): Outcome<Unit>
}
interface BackupService {
    suspend fun exportTo(contentUri: String, includeMedia: Boolean): Outcome<Unit>
    suspend fun previewRestore(contentUri: String): Outcome<PackPreview>
}
