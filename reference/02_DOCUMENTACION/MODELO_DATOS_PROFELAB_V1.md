# Modelo de datos ProfeLab V1.0 — contrato conceptual

**Persistencia propuesta:** Room (SQLite), repositorios, versiones de esquema/migraciones + archivos binarios en directorio privado. Nombres definitivos de tablas y columnas podrán modificarse en Codex preservando la semántica.

## 1. Entidades

| Entidad | Campos mínimos | Relaciones e invariantes |
|---|---|---|
| `Profile` | id, displayName?, academicCycle?, institution?, photoUri?, theme, createdAt | Único perfil local V1; ciclo académico no es grado de primaria |
| `Task` | id, title, notes?, dueAt?, status, priority, relatedType?, relatedId?, createdAt, completedAt? | Estado OPEN/DONE; con referencia validada; tareas sin fecha admitidas |
| `Session` | id, title, primaryGrade(1..6), area, date?, minutes, purpose, significantSituation?, competencyRefs[], evidence, assessmentInstrument?, status, updatedAt | Independiente de perfil académico; borrador aunque falte contenido |
| `SessionCriterion` | id, sessionId, text, position | Orden estable; eliminación vinculada solo con confirmación |
| `SessionMoment` | id, sessionId, kind(INICIO, DESARROLLO, CIERRE), minutes, activities, strategies?, resources?, sort | La suma de minutos puede diferir, genera advertencia |
| `SessionMaterialLink` | sessionId, materialId, note?, requiredCount? | FK; al desvincular no borrar material original |
| `CurricularReference` | id, sourceUri, sourceTitle, versionOrYear, type, grade?, area?, value, verifiedAt, origin | Solo etiquetar OFICIAL cuando verificado; en casos dudosos USER_PROVIDED |
| `MaterialProject` | id, title, objective, primaryGrades[], areas[], durationMinutes?, difficulty, steps[], description, status, sourceCredits, createdAt, updatedAt | Propiedad de usuario/plantilla autorizada; no duplicar datos sensibles |
| `ProjectStep` | id, projectId, position, instruction, checked, safetyNote? | Checklist persistente; no afecta inventario al marcar |
| `MaterialInventoryItem` | id, label, unit, quantityAvailable, storagePlace?, unitCost?, updatedAt | Cantidad >= 0 con decimales precisos; historial de ajustes |
| `ProjectRequirement` | id, projectId, itemId?, label, amount?, unit, haveState, buyState | Un requerimiento no borra ni consume inventario |
| `InventoryMovement` | id, itemId, delta, reason, createdAt, relatedProjectId? | Solo cambios confirmados; transacción y deshacer registrado |
| `Note` | id, title, body, type(APUNTE, IDEA, PRACTICA, REFLEXION), tags[], course?, academicCycle?, schoolGrade?, savedAt | Tipo no es tab exclusivo; `schoolGrade` solo cuando corresponda |
| `Practice` | id, noteId?, date, school?, grade?, observation, reflection, nextSteps, institution?, consentFlag? | Evitar datos personales de menores por defecto |
| `Creation` | id, kind, linkedEntityType, linkedEntityId, state, tags[], updatedAt | Composición de materiales/notas/sesiones/portafolio |
| `Portfolio` | id, title, description, createdAt, updatedAt | Contenedor ordenado de evidencias seleccionadas |
| `PortfolioItem` | portfolioId, entityType, entityId, position, includeAttachments | Vista previa antes de exportar |
| `LibraryDocument` | id, originalFilename, localStorageRef?, sha256?, title, issuer?, year?, sourceUrl?, licenseLabel?, licenseVerified, category, favorite, pages?, importedAt | Nunca afirmar licencias sin `licenseVerified`; PDFs importados pueden no tener texto |
| `ReadingPosition` | docId, pageNumber, updatedAt | 1 <= page <= pageCount, si PDF disponible |
| `DocumentAnnotation` | id, docId, pageNumber?, noteId, comment? | Al faltar PDF, preservar nota y mostrar origen no disponible |
| `Scan` | id, sourceKind, imageStorageRef, ocrRawText?, ocrReviewedText?, status, lang, engine, createdAt | STATUS CAPTURED/RECOGNIZED/REVIEWED; no guardar resultado final sin revisar |
| `ScanDestination` | scanId, entityType, entityId, createdAt | Enlace a nota/proyecto/sesión tras confirmación |
| `VoiceMemo` | id, noteId?, audioStorageRef, durationMs, transcript?, transcriptReviewed, createdAt | AUDIO_ONLY permitido; STT solo en dispositivo comprobado |
| `Attachment` | id, ownerType, ownerId, storageRef, mime, sizeBytes, hash, title? | No guardar URI temporal revocable como único respaldo |
| `NotificationSetting` | id, taskId?, enabled, scheduleRule?, lastScheduledAt | Gestionar permisos y deshacer; sin notificaciones ficticias |
| `ImportEvent` | id, sourceType, sourceHash?, importedAt, outcome, warnings[] | Solo metadatos técnicos; no loguear OCR personal |
| `BackupManifest` | schemaVersion, generatedAt, entitiesCount, filesCount, sha256Index, appVersion | No reemplazar datos sin preview/rollback |

## 2. Reglas de vinculación y operaciones

- Cada tipo usa UUID; referencias `entityType + entityId` se validan en capa repositorio y tests; preferir tablas puente con FK cuando los tipos sean conocidos.
- Crear enlace entre sesión y material: solo asignar reference; no duplicar proyecto salvo acción explícita `Hacer copia`.
- Borrar PDF: solicitar confirmar; notas vinculadas permanecen recuperables con estado `ORIGEN_NO_DISPONIBLE`.
- Borrar sesión: papelera temporal; tareas y materiales vinculados no desaparecen por cascada accidental.
- OCR: almacenar imagen original; resultado crudo inmutable; corrección separada; actor/fecha de confirmación; no persistir OCR erróneo como contenido final automáticamente.
- Progreso semanal: `DONE / total` para tareas con fecha dentro del intervalo lunes-domingo local configurado; tareas sin fecha no cuentan a menos que pertenezcan manualmente a esa semana. Explicar el cálculo UI.
- Inventario: cantidad y unidad coherentes; no restar por check de preparación, solo con `Consumir materiales` y confirmación.
- Versionar esquema Room con migraciones comprobables; jamás `fallbackToDestructiveMigration` en producción.
- Búsqueda: debounce para título/etiquetas; no ejecutar OCR masivo en cada búsqueda.
- Backup portable: JSON de entidades y manifest + binarios; checksum de cada entrada, export a SAF ZIP; import test de integridad antes de reemplazar.

## 3. Estados y enumeraciones

```
SessionStatus = BORRADOR | EN_PROGRESO | REVISADA | ARCHIVADA
ProjectStatus = IDEA | PREPARACION | EN_PROCESO | TERMINADO | ARCHIVADO
TaskStatus = PENDIENTE | COMPLETADA
ScanStatus = CAPTURADA | RECONOCIDA | EN_REVISION | CONFIRMADA | DESCARTADA
DocumentOrigin = USUARIO | MINEDU_VERIFICADO | OTRA_FUENTE
OfflineCapability = DISPONIBLE | REQUIERE_CONFIGURACION | NO_COMPATIBLE
```

No inventar categoría de identidad con el contenido de OCR. Compartir/exportar siempre acción explícita.

## 4. Datos de ejemplo

`05_DATOS_EJEMPLO/fixtures_profelab.json` es material ficticio para UI y QA. No contiene datos de alumnos reales ni documentos atribuidos falsamente a una fuente oficial. No insertar licencias oficiales inventadas para reproducir imágenes.

## 5. Contenido incremental y contratos entre módulos (anexo obligatorio)

- `ContentPack(packId, packVersion, schemaVersion, minAppVersion, title, origin, publisher, importedAt, status, manifestHash)`: versión y procedencia de un paquete local; admite actualización idempotente y rollback.
- `ContentPackItem(packId, itemId, itemVersion, entityType, entityId, storageRef?, sha256?, licenseStatus, sourceUrl?, isUserModified)`: cada idea/plantilla/PDF del paquete con origen y derechos declarados/verificados. No asignar un rótulo oficial sin evidencia.
- `PackImportEvent(id, packId, fromVersion?, toVersion, added, updated, conflicts, skipped, outcome, at)`: resumen técnico verificable, sin registrar texto personal completo.
- `EntityRef(entityType, entityId)` / `ScanDestination` conectan Cuaderno, Sesiones, Taller y Biblioteca sin copiar contenido y sin dependencias directas entre UI.
- Al editar un elemento de plantilla, crear copia de usuario o capa de modificaciones; actualizar el catálogo nunca sobrescribe creaciones ni las notas vinculadas.
- `LibraryDocument` debe permitir `originPackId?`, `originPackItemId?`, `sourceRightsVerified`, `localStorageRef`, `sha256` y metadatos revisables. Un PDF externo puede importarse por SAF sin integrar el PDF en el APK.
- Cualquier migración actualiza Room explícitamente y se prueba con snapshots anteriores; sin borrado completo de bases ni reinstalaciones como solución.
- El permiso de cámara/micrófono/no-flash es **estado de hardware o de sistema**, no una propiedad de la base de datos de usuario que habilite permanentemente funciones.
