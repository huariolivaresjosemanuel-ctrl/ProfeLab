# Taller 3.0 — implementación incremental

Base: main 51764a0365f44eac2b6793265ab2c70d0dc6eb97. Fecha: 2026-11-03. Se continúa el proyecto existente, sin reaplicar T0/T1 ni reconstruir la aplicación. Estado: código implementado; compilación, aceptación Android y comparación visual ejecutada pendientes.

## Siete ventanas en código Compose

| Referencia aprobada | Componente independiente | Cambio funcional |
|---|---|---|
| W00 Mi Taller | WorkshopHub | Seis sectores, recientes y accesos a inventario/compras/herramientas. Categorías incompatibles conservan su bloqueo. |
| W01 Catálogo | SectorCatalog | Rejilla adaptativa de dos columnas o una con poco ancho/fuente grande; búsqueda, filtros y favoritos; portada 3:2. |
| W02 Ficha | ActivityDetail | Hero 16:9, tres perspectivas, datos reales y acceso a materiales/procedimiento/resultados/guía. Consultar no crea una ejecución. |
| W03 Materiales | MaterialsChecklist | Lista compacta, existencias físicas, reservas propias/ajenas y disponibilidad; preparación personal separada del stock, compras y compartir. |
| W04 Procedimiento | GuidedStepPager | Un paso ilustrado a la vez; N pasos por ID, siguiente separado de completar, notas propias y lista secundaria. |
| W05 Resultados | ActivityResultsScreen | Registro fechado, evidencias privadas, resultados, análisis, limitaciones y conclusión sobre los campos históricos existentes. |
| W06 Guía docente | TeacherClassroomPanel | Presentación, evaluación y adaptación; preguntas, criterios y reflexión sin estudiantes ficticios. |

Propósito pedagógico ampliado y guía/editor avanzados siguen como accesos secundarios. PURPOSE no sustituye W05 ni añade otra ventana al recorrido principal. Las cinco pestañas raíz se conservan.

La interfaz sigue las siete referencias verticales del encargo. No se ha ejecutado la comparación de capturas reales del parche con esos PNG: no afirmar fidelidad visual aprobada ni ausencia total de regresiones a partir de una revisión de fuentes. WorkshopFlowCaptureTest prepara los siete archivos w00–w06 sin escribir datos personales y sin esperar fotos inexistentes.

## Datos y conservación

Se reutilizan WorkRecord, la cola de guardado, los campos JSON históricos, Room3, T1 y BackupTransfer. No cambia ninguna entidad, SQL, esquema exportado o número de base: no requiere migración de Room ni borrado de datos. No se añade fallbackToDestructiveMigration. El roundtrip de respaldo, reapertura y confirmación idempotente tiene pruebas instrumentadas nuevas, pendientes de ejecución.

Nueva ejecución/copia: debe empezar sin registros, adjuntos, resultados, notas, check de pasos, preparación ni reflexión propios del original. Mantiene su definición y recursos editoriales. El registro original no se modifica. Completar o visitar pasos no descuenta materiales: el uso real requiere el panel T1 y su confirmación explícita.

ActivityResultsData modifica únicamente los cuatro campos permitidos de una ejecución personal activa, conservando extensiones desconocidas. WorkshopEvidenceData distingue evidencias por attachments y MIME, valida medidas acotadas y elimina observaciones mediante ID/snapshot para evitar borrar otra fila tras cambios concurrentes.

El procedimiento lee cantidades variables de pasos para proyectos, experimentos y futuras categorías compatibles; no depende de cuatro pasos ni de experimentos. El catálogo original de 12 proyectos y 8 experimentos conserva sus bytes y 80 pasos. El validador existente limita los nuevos contenidos a 100 pasos. El motor reutilizable no activa Cocina/FOOD ni contratos aún no compatibles.

## Medios preparados, sin fotos ficticias

WorkVisualAdapter convierte explícitamente un lote editorial validado al importador actual. WorkshopMediaBindings vincula por identidad pública/namespace, versión, rol y stepId/materialId, con huella del texto de cada paso. Los overlays catalog_media y archivos privados respaldables no clonan actividades ni modifican inventario, progreso u observaciones. El registro de slots tiene resources vacío. Ver MEDIOS_WORK_Y_COMPATIBILIDAD.md para contrato, aprobación, límites y orden de integración.

## Validación honesta

La entrega contiene registros de las verificaciones locales Python, léxicas y de conservación en 04_QA_Y_EVIDENCIAS. Las comprobaciones léxicas no compilan Kotlin. Las pruebas de firma y orquestación utilizan stubs, no certifican una APK. La validación SQL local no equivale a ejecutar Room en API26/API36. Las declaraciones instrumentadas anteriores se conservan; las nuevas y las siete capturas requieren Android.

No se instalaron Java, Gradle, Android SDK o Node; no se generó APK ni se inició Actions. El usuario debe aplicar el incremental y ejecutar manualmente ProfeLab Android con run_emulator=true. Un run histórico de la base tuvo build/API26/API36 correctos pero fallo en configuración de firma permanente: no valida este parche ni autoriza declarar resuelta la firma instalada. Ninguna clave o configuración de firma se cambia aquí.
