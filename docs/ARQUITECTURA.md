# Arquitectura integrada ProfeLab V1.0

Una APK,16 módulos Gradle. AppGraph inyecta repositorios y servicios por constructor/callback. Las features no importan otras features ni core/database. Core/model define datos/reglas/contratos; WorkspaceStore y LocalTransfer amplían los contratos existentes sin sustituir los de tareas/perfil. No existe servicio vacío que devuelva éxito.

| Módulo | Responsabilidad |
|---|---|
| app | Application, recepción intents, navegación, AppGraph, WorkManager, revisión de exportaciones y herramientas de datos |
| core:model | Task/Profile/EntityRef, WorkRecord/WorkspaceStore/LocalTransfer, reglas Inicio e inventario |
| core:database | Room2, cuatro tablas H1 +workspace_records, repositorios, DataStore, respaldo/fusión |
| core:content | Catálogo MINEDU, semillas Taller, instalación de packs |
| core:files | Copia privada, UUID/hash/límites, informes PDF/FileProvider |
| core:security | Validación de dominios y rutas |
| core:ui | Tema/arte vectorial, componentes, editor común, vínculos, WorkspaceViewModel |
| feature:home | Inicio, tareas/edición/borradores y actividad |
| feature:profile | Perfil, preferencias, herramientas/Acerca de |
| feature:taller | Catálogo, copias, inventario, compras, experimentos y observación |
| feature:sessions | Planificación, evaluación, tiempos, recursos/versiones |
| feature:notebook | Notas/prácticas/ideas, voz y portafolio |
| feature:library | Referencias, favoritos, documentos y continuar lectura |
| feature:pdf | Render, texto, anotaciones, persistencia vía callbacks, exportación |
| feature:capture | CameraX/linterna/galería, sin pestaña nueva |
| feature:ocr | Latin bundled, revisión/draft, lote incremental, sin pestaña nueva |

App ensambla todos. Features →core:ui/core:model (PDF además usa core:files); core:ui→core:files/core:model; database/content/files/security→core:model. El grafo exacto se incluye en `docs/completo/GRAFO_MODULOS.csv`; `verify_project.py` comprueba ausencia de ciclos. PDFBox/CameraX/MLKit quedan en sus módulos salvo inicialización PDFBox en app.

Cinco rutas raíz únicamente: home/taller/sessions/notebook/library. Prácticas, PDF, captura, OCR, exportación y datos son destinos derivados. Vínculos por ID; citas por documento/página y opcional annotationIds. Un documento está ligado a su hash; al abrir se comprueba contra el archivo privado. La rotación se toma del PDF inmutable para transformar coordenadas normalizadas, sin reescribir el original.

Persistencia y decisiones distintas de las tablas previstas en SDD: `docs/completo/MIGRACIONES.md`. No se vende esta versión como arquitectura final para miles de documentos: Flow carga registros completos y varios payloads son JSON; tipado/índices/rendimiento requieren seguimiento. El cuerpo de ProfeApp conserva callbacks de integración; motores y SQL permanecen en módulos propios.
