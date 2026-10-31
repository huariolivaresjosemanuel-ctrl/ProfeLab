# Implementación propuesta por fases independientes

T0 está entregado como documentación y pruebas de base. Ninguna de las fases siguientes está implementada en este ZIP. Confirmar main y diferencias en cada inicio; integrar preservando F0, contratos y tests. Ver ARCHIVOS_PLANIFICADOS.csv para rutas completas propuestas.

| Fase | Entrega concreta | Reutilizar | Puerta de salida |
|---|---|---|---|
| T1 | Dominio de disponibilidad de4 estados, comandos durables, ledger/reservas y adaptadores; migración y backups coherentes | InventoryRules, WorkspaceStore, WorkshopCodec, compra acquire/receipts y copias | Pruebas bloqueantes de datos/migración/reapertura/backup e idempotencia antes de activar. No exige aprobar fotos. |
| T2 | Portal A0, catálogo A1 y ficha A2; estado de navegación por IDs, catálogos activos/futuros, favoritos y Mis ideas | WorkshopScreen/Catalog/Guide, caché WorkshopImages, F0 tags y semilla20 | Captura Android individual por ventana, A0/A2 contra su PNG; A1 y ubicación Arte/Tecnología candidatas a revisión. Móvil360dp/tablet/200%/TalkBack; fallback honesto de medios. |
| T3 | Materiales A3, disponibilidad real, confirmación de uso, copiar/compartir compras | T1 transaccional, WorkshopInventory/Shopping/ShoppingData | Clipboard offline, Sharesheet text/plain, con/sin WhatsApp; sin contactos, teléfono o APIs. Compartir no compra ni altera stock. |
| T4 | Propósito A4, paso guiado/lista A5 y recuperación por stepId | steps.done/id, TeacherGuides, guías/experimentos, evidencias y seguridad | Estado común entre lista/pager, navegación sin marcar, reanudar tras cierre/giro; fotos por paso revisadas o fallback. Separar hipótesis/observación/resultado real. |
| T5 | Presentación A6, guion breve y reflexión formativa; contrato futuro | TeacherGuides y copia editable, exports existentes | Criterios estables, guion preservado tras pack, sin alumnos ficticios ni tablas privadas; captura A6 frente al PNG con excepciones obligatorias documentadas. |
| T6 | Regresión integral, accesibilidad, rendimiento y cierre | Matriz51 y suites actuales/F0 | API26/36, cinco tabs, OCR/PDF/Flow/sesiones/cuaderno/perfil, upgrade firmado y backup, capturas1:1, datos vacíos/moderados/abundantes, resultados honestos. |

## Responsabilidades y navegación

Mantener app/core/feature. app solo integra dependencias, rutas externas y acciones entre features. feature:taller conserva navegación interna y componentes pequeños: WorkshopHub, SectorCatalog, ActivityDetail, MaterialsChecklist, PurposePanel, GuidedStepPager/StepList y TeacherClassroomPanel. No duplicar ViewModels de datos ni concentrar todo en WorkshopScreen. Repositorio e interfaces de operación en core; contenido/validadores en core:content; tokens/mascota en core:ui.

ActivityViewState es proyección inmutable de WorkRecord + catálogos + disponibilidad + preferencias de visita. Mantener activityId/contentVersion/perspective separados de window A0–A6. Entrar no crea copia; Comenzar explícito conserva el patrón de UUID nuevo. Perspectivas no son tres flujos obligatorios ni cambian progress.done.

Primera consulta A0→A1→A2, luego acceso A3/A4/A5/A6 por atajo. Copia en progreso puede retomar último paso real, con resumen accesible. A5 usa lastVisitedStepId para navegación y steps.done para completado; siguiente/anterior no marcan. Guardar errores conserva borrador/reintento. Volver no debe consumir, crear otro run o sobrescribir cambios posteriores.

SectorId es taxonomía de navegación, distinto de categoryId del contenido y de WorkRecord.kind. Mapeo propuesto: Taller agrupa12 originales y categorías existentes; Experimentos agrupa8; Mis ideas muestra copias. Cocina/Danza/Tradiciones/Arte/Tecnología son capacidades futuras salvo que haya un pack compatible ya instalado, que debe seguir accesible. No renombrar categoryId dance/heritage ni mover arte-001 a un sector vacío. FOOD requiere validador especializado antes de activarse.

No añadir frameworks, módulos Gradle, red ni datos nominales. Nuevos packs serán datos versionados con namespaces, hashes, licencias y revisión, no código ejecutable. Mantener snapshots personales y guion al actualizar; rechazar colisiones/retrocesos y medios sin asignación aprobada.
