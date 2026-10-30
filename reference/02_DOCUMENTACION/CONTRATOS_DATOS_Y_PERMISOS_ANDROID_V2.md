# Contratos de datos, permisos y evolución

## 1. Esquema de alto nivel

| Entidad | ID/relaciones | Propiedades críticas |
|---|---|---|
| Profile | profileId | nombre opcional, carrera, cicloAcademico, accesibilidad |
| Task | taskId, relatedEntityId? | título, vencimiento, completedAt, alarmScheduled |
| CurriculumReference | sourceId | año, autor, URL, rightsStatus, verifiedAt |
| Session | sessionId, profileId | gradoEscolar (1..6), área, estado, propósito, tiempos y versión |
| SessionResource | sessionId + resourceId | tipo project/document/note, origin, page?
| ContentPack | packId+version | schema, rights, manifestHash, installedAt, status |
| ProjectTemplate | packId+projectId | datos fuente inmutables y `reviewStatus` |
| UserProject | userProjectId | forkedFrom, draft, tipo, pasos, fotos, versión personal |
| InventoryEntry | entryId | materialCanonicalId, quantityDecimal, unit, updatedAt |
| ExperimentRun | runId, projectId | pregunta, hipótesis, variables, controles, fecha |
| Observation | observationId, sessionId?, runId? | before/during/after, noStudentIdentifiers por defecto |
| Note | noteId, links[] | texto, tipo, etiquetas, adjuntos y borrador |
| Document | documentId, sourceId? | uri privado, sha256, title, mime, pageCount, rightsStatus |
| PdfAnnotation | annotationId, documentId | página, coordenadas normalizadas, tipo, color, comentario |
| DocumentProgress | documentId | última página, offset, lastOpenedAt |
| OcrDraft | draftId, sourceDocumentId? | OCR original, corrección, idioma, estado, destino |
| Attachment | attachmentId | URI privado, tipo, integridad, privacidad |
| ClaimCitation | claimId, sourceId | tipo y ubicación verificada o null, licencias |

No hacer joins por el título de una actividad o nombre de archivo; IDs estables y relaciones con política ON DELETE restrict/reparent. Campos de autoría/medios controlados. Se recomienda usar Room con validaciones e integridad referencial y Flow.

## 2. Contratos entre features, ejemplos

```kotlin
interface ProjectCatalog { suspend fun query(filters: ProjectFilter): List<ProjectSummary> }
interface InventoryResolver { suspend fun evaluate(projectId: String): AvailabilityResult }
interface DocumentStore { suspend fun import(uri: Uri, keepPrivateCopy: Boolean): DocumentRef }
interface PdfAnnotationRepository { suspend fun add(annotation: PdfAnnotation): Result<Unit> }
interface OcrEngine { suspend fun recognize(source: ImageSource): OcrDraft }
interface EducationalSourceRepository { suspend fun source(sourceId: String): SourceAttribution? }
```

Pseudocódigo de contrato, no implementación compilada. API estable y sin dependencia inversa; los módulos consumidores acceden a interfaz de `:core:model`/`:core:files` y usan IDs.

## 3. Permisos por necesidad

| Función | Permiso / API | Cuándo y qué hacer si se deniega |
|---|---|---|
| Escáner | `android.permission.CAMERA` | Al abrir captura; ofrecer importar desde galería si denegado |
| Flash | `CameraControl.enableTorch` y `hasFlashUnit` | No pedir permiso separado; desactivar botón sin hardware/permiso de cámara |
| Grabación local | `RECORD_AUDIO` | Al grabar; permitir notas de texto cuando deniega |
| Recordatorios | `POST_NOTIFICATIONS` Android 13+ y calendario local | Cuando usuario habilite notificaciones; mostrar tareas sin avisos si niega |
| Importar PDF | `ACTION_OPEN_DOCUMENT` | Selector SAF, sin permiso global de archivos |
| Capturar foto existente | Android Photo Picker / SAF | Solo archivo elegido, no permiso masivo |
| Exportar/copiar | `ACTION_CREATE_DOCUMENT` / `FileProvider` | Usuario selecciona destino, URI con permiso temporal |
| Internet | No requerido para la funcionalidad esencial | Solo permitir abrir navegador a fuente oficial si usuario decide |

Verificar versiones de API/Play Store al implementar, especialmente políticas de permisos en Android 13–16. Pruebas tras denegar/revocar cada permiso, memoria baja, actualización y rotación. No pedir ubicación ni lectura de contactos.

## 4. Importación/actualización packs

Validación: firma opcional/sha256 obligatorio cuando se obtiene el archivo, `packId`, `semver`, schema, `minAppSchema`, contenido/licencias, lista de archivos, tamaño total, paths normalizados; staging aislado; transacción; rollback si error. Descartar `../`, rutas absolutas, duplicados de ID, archivos de tamaño excesivo, JSON inválido y zip-bombs. Los contenidos editoriales se versionan por pack, las **copias del usuario** nunca se sobreescriben. La actualización de PDF con hash distinto debe conservar anotaciones y pedir confirmación de reanclaje.

## 5. Copias y restauración

Backups sin secretos, con versión de esquema, datos Room exportados de forma consistente y adjuntos seleccionados; confirmación de contenido y privacidad, cifrado opcional con recuperación explícita, importación con merge o reemplazo confirmados, tratamiento de errores y rollback. Automatizar tests de migraciones sucesivas y restauración de pack de versión anterior.
