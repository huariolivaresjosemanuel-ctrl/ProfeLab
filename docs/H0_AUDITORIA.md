# Auditoría H0 · 29 de octubre de 2026 (America/Lima)

## Evidencia revisada

Se inspeccionaron los 83 archivos del paquete extraído, incluido el ZIP histórico (entradas leídas y catalogadas), los cinco PNG aprobados individualmente y su galería, todos los documentos de especificación, matrices, scripts, JSON y recursos. `audit/package_review.json` registra tamaño, SHA-256, tipo de revisión, dimensiones, recuentos y estructura por archivo; `audit/historical_zip.json` registra el archivo histórico. Los 81 hashes suministrados coinciden. El inventario y SHA256SUMS no se validan recursivamente contra sí mismos.

Los 4 PDF se abrieron con pypdf; el protegido se leyó con la contraseña de QA documentada. La guía visual se renderizó con Poppler y se inspeccionó. Se verificaron los PNG y miniaturas; las galerías HTML y scripts se revisaron como texto, sin ejecutar descargadores históricos. Esta auditoría de archivos no es una prueba del lector Android.

Recuentos comprobados: 46 vistas/estados, 191 escenarios QA (los primeros 134 se conservan idénticos), 12 referencias MINEDU, 12 proyectos V1/V2 con los mismos IDs, 8 experimentos, 12 miniaturas y 3 PDFs originales de prueba. Las 191 filas originales siguen `NO_EJECUTADO`.

## Orden de autoridad aplicado

Solicitud actual del usuario → SDD maestro y anexos V2 → cinco referencias visuales aprobadas para composición → V1/histórico solo para continuidad. El README y el prompt V2 se leyeron primero en la conversación; la auditoría posterior amplió la revisión al resto. Los documentos externos aportan requisitos; no autorizan operaciones fuera de la solicitud actual.

## Conflictos resueltos

| Conflicto | Decisión aplicada |
|---|---|
| V1 permite paquetes por feature; V2 exige módulos Gradle | 16 módulos reales, con los nombres del SDD V2. |
| V1 ordena Sesiones antes de Taller e incluye backups básicos en H1 | Prevalece §14 V2: H1 Inicio/tareas/perfil/persistencia; H2 Taller, H3 Sesiones y backup en H7. |
| QA-115 y algunos anexos siguen diciendo 6 documentos | Catálogo V2 de 12; se preserva QA histórico sin alterarlo. El cambio de expectativa se documenta, no se elimina el caso. |
| Progreso, nombre, foto, tareas y textos de ejemplo del mockup | Estados vacíos y datos reales. Se conserva la composición; no se inyectan fixtures personales en la base. |
| Mockup de Cuaderno promete convertir voz en texto | No se anuncia transcripción. Grabación H4 y transcripción condicional a motor local probado. |
| Requisito PDF P0 completo frente al hito H1 | P0 de producto no significa entrega en H1. Contratos ahora; intents/lector/anotaciones/exportación en H5. Manifest H1 no reclama abrir PDFs. |
| Objetivo Android 7 frente a APIs y bibliotecas | Base API 26+; no se promete Android 7 ni compatibilidad física antes de pruebas. |
| Referencias abiertas frente a permiso de redistribuir PDF | Ningún binario MINEDU en APK. Catálogo y enlaces conservados con `distribution_approved=false`. No se emite dictamen jurídico. |
| `approvedForDistribution=true` editorial frente a seguridad pendiente | Los permisos editoriales no validan seguridad; no se publican como actividades recomendadas. Semilla íntegra fuera de APK, pendiente H2. |
| Experimentos con cantidad `1` y unidad `referencial: ajustar` | No convertir a stock disponible ni alimentar recomendador sin revisión y unidades normalizadas. Bloquea recomendaciones verificadas de esos casos, no H1. |
| `EJEMPLO_MANIFIESTO_PACK_V2.json` sin SHA final | Es ejemplo, no pack instalable. H2 deberá generar manifiesto real sobre bytes finales. |
| V1 `documents/officialUrl` frente a V2 `docs/official_page` | Lector de catálogo H1 usa exclusivamente esquema 2. Adaptadores de importación y migraciones de packs, pendientes H2. |
| SDD pide seguridad de catálogo mediante `safety.reviewStatus`, experimentos la ponen en raíz | H2 necesita un adaptador explícito conservador; falta de revisión siempre significa pendiente. |
| Firma debug frente a actualizaciones sin pérdida | La APK inicial usa firma debug. Antes de distribuir actualizaciones, configurar clave propia estable; nunca copiar JKS ajenos. |

## Viabilidad técnica

H1 puede implementarse con Compose, ViewModels/Flow, Room, DataStore y WorkManager sin backend. Las versiones seleccionadas existen en repositorios oficiales y los metadatos AAR inspeccionados admiten `compileSdk=36`/AGP 8.11.1. Eso acredita disponibilidad y requisitos declarados, **no** una compilación conjunta ejecutada.

Se eligió una línea estable compatible de AGP/Kotlin/Compose, sin versiones dinámicas ni previews; Room incorpora la corrección estable 2.8.5. No se adoptan automáticamente las versiones más recientes de todas las bibliotecas, porque pueden elevar requisitos o cambiar APIs. Ver `DEPENDENCIAS.md`.

H5: `PdfRenderer` base con otro motor para extracción de texto/exportación; en API 26 no equivale a selección semántica. PDFBox-Android es candidato sujeto a rendimiento, cifrado, licencias y coordenadas. DocReader solo referencia arquitectónica; no se integró código. Probar resaltado real, cropbox/rotación, original inmutable, exportación visible en otro lector y límites de memoria. OCR de un escaneo no garantiza alineación perfecta de texto.

H6: CameraX, comprobación de flash, permisos por acción y OCR Latin bundled. No se incluyeron dependencias de cámara/OCR todavía: se fijarán y probarán al implementar el módulo. No se adopta el código ni la firma de ScanPro; el paquete informa ausencia de licencia. Primer uso OCR en modo avión es condición de aceptación futura.

## Límites y bloqueos de aceptación

- No hay Java/Gradle disponibles en PATH, no se instaló toolchain ni se compiló en el equipo, conforme a la restricción del proyecto. No se inició ningún workflow remoto. APK, lint, pruebas JVM/instrumentación y prueba visual de la app: **NOT_RUN**.
- `minSdk=26` está justificado por APIs de fecha, notificaciones y baseline; aceptación real API 26/36 y Lenovo Tab Plus pendiente. Las funciones de cámara/stylus requieren hardware físico.
- No hay esquema Room anterior publicado. Se inicia versión 1 con exportación de schema por KSP; CI debe entregar y el usuario debe conservar `1.json` antes de evolucionar. No se inventa una migración 1→2 ni se usa borrado destructivo.
- Respaldo/restauración todavía no disponible. Auto Backup está desactivado y se excluye transferencia para evitar copias implícitas de datos; la app lo informa. Android/OEM puede imponer reglas de transferencia adicionales: revisar en H7.
- Los borradores se escriben en cola al cambiar campos y se drenan al salir por Volver. Un corte abrupto durante una escritura no garantiza conservar teclas aún no confirmadas: solo se marca guardado al terminar. Probar cierre forzado y almacenamiento lleno.
- Avisos con WorkManager son aproximados y requieren permiso/canal habilitado. No usar como alarma exacta. Pruebas de reinicio, revocación y ahorro de batería pendientes.
- Las composiciones de Inicio se adaptan: con poco ancho o fuente grande las tarjetas apilan contenido; actividad y pendientes pasan a una columna en móvil pequeño. Fidelidad visual final necesita captura real y comparación, no se declara aprobada.
- Los contratos de H2–H7 están definidos pero no son implementaciones de esas funciones. Biblioteca H1 no importa PDF; hay consulta local y enlace explícito. H1 no satisface todo el producto P0.

## Resultado H0

Arquitectura y decisiones registradas, código base H1 escrito, matriz de cobertura de 46 filas y plan QA preparados. No hay conflicto que impida entregar fuentes H1; su aceptación como app instalable queda condicionada a la compilación y pruebas Android, que deben ejecutarse tras la subida manual del usuario.
