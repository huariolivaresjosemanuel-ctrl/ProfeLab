# SDD MAESTRO — ProfeLab V2 de especificación (objetivo de producto V1.0)

**Fecha:** 2026-10-29. **Estado:** requisitos consolidados para desarrollar y comprobar; NO significa APK existente ni pruebas ejecutadas. **Fuente visual superior:** `01_REFERENCIAS_UI/01_INICIO.png` a `05_BIBLIOTECA.png` (imágenes exactas aprobadas). **Procedencia:** conserva íntegra documentación V1.1 y añade acuerdos posteriores. **Idioma:** español de Perú. **Público:** estudiantes de Educación Primaria, practicantes y docentes.

## 0. Orden de autoridad y cómo resolver contradicciones

1. Los acuerdos aprobados del usuario, materializados aquí, tienen prioridad.
2. Cinco capturas ProfeLab en `01_REFERENCIAS_UI` son la **única referencia visual aprobada**. Las imágenes antiguas `11_HISTORICO_REFERENCIAS/` son archivo histórico y no deben imponerse.
3. Este SDD maestro y sus anexos V2 gobiernan funciones, derechos, contratos y datos; la V1.1 se conserva para trazabilidad, pero una cláusula incompatible de V1.1 queda sustituida por la V2 más específica.
4. Los PDF oficiales no incluidos NO pueden reemplazarse por documentos inventados, y lo mostrado en mockups nunca prueba autoría/licencia/año/páginas.
5. Si un requisito es incompatible con Android, seguridad, licencia o funcionamiento offline, reportar el conflicto con alternativas comprobables. No eliminar funciones silenciosamente.

## 1. Objetivo y resultado exigido

Una única aplicación Android **offline-first** para preparar y organizar sesiones de Primaria, crear materiales y experimentos con inventario, tomar apuntes, documentar prácticas, consultar PDFs oficiales/importados, leer y anotar PDF, extraer texto con OCR local, registrar evidencias y compartir/exportar trabajo. La app no es MINEDU ni genera documentos oficialmente validados. **Un botón visible debe tener acción verificable, deshabilitarse de forma explícita o eliminarse de la pantalla.** No simular estadísticas, catálogo cargado o archivos descargados.

### 1.1 Usuarios
- Estudiante de Educación Primaria: registrar `cicloAcademico` en un campo independiente de `gradoEscolar` de la sesión. No confundir 6.º ciclo universitario con 6.º de Primaria.
- Practicante: sesiones, rúbricas, escuela opcional, evidencias y reflexiones.
- Docente: organización y material reutilizable.
- Datos de niños: **mínimos**, anónimos por defecto, sin inferencia clínica ni perfilado psicológico.

### 1.2 Decisiones ya aprobadas
- Navegación inferior de 5 pestañas: **Inicio · Taller · Sesiones · Cuaderno · Biblioteca** (exactamente como las imágenes).
- Diseño adulto, cálido, escolar, tarjetas crema/verde, composición limpia, responsive para teléfono y tableta, lápiz opcional.
- `Recibir PDF=both`: manejar **Abrir con ProfeLab** (`ACTION_VIEW`), **Compartir a ProfeLab** (`ACTION_SEND`) y **Importar PDF** desde la app; preguntar `Leer ahora` o `Guardar en mi Biblioteca`, preservando origen.
- `Lector=full`: resaltado, subrayado, lápiz, borrador, notas, marcadores, búsqueda, reanudar, exportar copia anotada y compartir.
- `Taller=full`: materiales, experimentos completos y Mis ideas desde cero, inventario, checklist, proyecto propio, observaciones y evidencias.
- `MINEDU=official`: catálogo de fichas oficiales; **solo empaquetar binarios PDF si el archivo concreto y derechos de distribución están verificados**. Algunos accesos están restringidos. No suprimir documentos del índice por carecer de PDF.
- `Pedagogía=ambos`: modo práctico y académico por actividad, con límites de la evidencia.
- QR: lector opcional para enlaces a recursos; no indispensable para las cinco pestañas. Nunca prometer OCR como lectura QR automática.

## 2. Arquitectura modular obligatoria

Una APK, features desacopladas por Gradle, contratos explícitos y bibliotecas núcleo reutilizables. No amontonar todo en `MainActivity.kt` o `TallerViewModel.kt`.

```text
:app                              # navegación y ensamblaje
:core:model  :core:database        # DTO/domain, Room, migraciones
:core:ui     :core:files           # design system, SAF, Storage, URI
:core:content :core:security       # packs, verificación, políticas
:feature:home                     # Inicio/pendientes
:feature:taller                   # catálogo, proyectos, experimentos, inventario
:feature:sessions                 # planificación y exportaciones
:feature:notebook                 # apuntes, prácticas, portafolio
:feature:library                  # catálogo MINEDU, importación y favoritos
:feature:pdf                      # lector, anotaciones, guardado/exportación
:feature:capture                  # cámara, flash, enfoque y recorte
:feature:ocr                      # ML Kit bundled, revisión del texto
:feature:profile                  # perfil, permisos, preferencias, respaldo
```

Módulos dependen de `:core:*` y APIs de otros módulos, jamás implementaciones UI de otra feature ni llamadas cíclicas. Integraciones por IDs, casos de uso e interfaces (`OpenDocument`, `AttachResource`, `AddObservation`, `FindMaterialProjects`). Transacciones para compartir recursos entre features. Usar DI y `ViewModel` con estado persistente, repositorios testeables; elegir versiones Android/Gradle verificadas al comenzar. GitHub del usuario **solo lectura**. Entregar nuevos/alterados ZIP con rutas exactas; compilación reproducible por GitHub Actions.

## 3. Inicio (P0)

- Header logo ProfeLab, avatar opcional/monograma, notificaciones reales, saludo contextual basado en hora local sin depender de internet.
- Ciclo académico de universidad, progreso semanal calculado de tareas/actividades propias; si no existen, mostrar estado vacío transparente.
- Próximas entregas, accesos Taller/Sesiones/Prácticas/Biblioteca, actividad reciente y pendientes con check.
- Crear/editar tareas, plazos, prioridad, origen (`sessionId` opcional) y recordatorios locales, sin servicios remotos obligatorios.
- Estados vacío, error, sin permisos, migrando y modo avión. Nada de estadísticas falsas como `3/5` fijas en producción.

## 4. Taller (P0, submódulos independientes)

### 4.1 Materiales didácticos
Catálogo filtrable por grado 1–6, área, tiempo, dificultad, materiales, seguridad, favoritos y tipo. Guía: portada original/imagen editorial, objetivo, preparación exclusiva de adulto, cantidad y unidad de materiales, pasos checklist, modos pedagógicos, adaptación, evidencias, fuentes y exportación. Paso completado persistente por instancia del usuario, no editar plantilla pública directamente.

### 4.2 Experimentos y proyectos de indagación
Ficha distinta del material: pregunta, hipótesis propuesta/editable, variable independiente y dependiente, condiciones controladas, lista de materiales/instrumentos, procedimientos con seguridad, tabla de observaciones fechadas, fotos opcionales, análisis, límites, conclusión y reporte PDF. Datos reales del usuario no se sustituyen por conclusiones predeterminadas. Experimentos seguros, supervisión y clasificación; impedir activar como recomendado un experimento cuya seguridad no fue validada.

### 4.3 Mis ideas y creaciones
Editor desde cero con título, tipo (material/experimento/idea libre), imágenes propias, notas, boceto, materiales, unidades, pasos reordenables, anexos, objetivo, grado, pregunta, fuentes propias, modo pedagógico, seguridad y exportación. Autosave/resume; duplicar crea nuevo ID; etiquetas de procedencia, historial simple por versión y borrador. El usuario puede guardar una idea incompleta. Importar proyectos por pack no altera sus ideas.

### 4.4 Motor recomendador de inventario
Inspirado en Mi Despensa: solo reglas locales deterministas. Inventario con cantidades, unidad, alternativas, materiales requeridos y herramientas, distinción de material consumible/reutilizable, reserva por proyecto y faltantes. Priorizar `puedo hacer`, `falta poco`, `preparación adulta`, excluir `inseguro`; no marcar como disponible si faltan unidades. Generar compras consolidada; herramienta ausente también es faltante. Guardar inventario sin red.

### 4.5 Pedagogía práctica/académica
Modo práctico: ¿para qué sirve?, cómo aplicar, qué observar, consejos, variaciones. Modo académico: competencia/capacidad/desempeño **solo si cotejada contra referencia**, procesos cognitivos/socioemocionales posibles sin efecto causal garantizado, adaptaciones inclusivas, fuentes por afirmación, límites y estado de validación. Alternar sin perder progreso. No dar diagnósticos o valoraciones clínicas.

### 4.6 Observación de aprendizajes
Antes/durante/después, lista de cotejo, rúbrica editable y registro anecdótico. Observaciones de comportamiento descriptivas y anónimas por defecto; portafolio y sesiones enlazan IDs. Evitar recopilación innecesaria de datos de menores. Solicitar permiso para fotos cuando aplica y previa revisión antes de compartir.

### 4.7 Inclusión y seguridad
Adaptaciones basadas en barreras (presentación, lenguaje, acceso, motricidad, tiempo, expresión), no etiquetas clínicas. Riesgos por nivel (bajo supervisado, preparación de adulto, no permitido en catálogo infantil); información de higiene, procedimientos de adultos y materiales sustitutos. NO combustión, red eléctrica, sustancias peligrosas, cortopunzantes manejados por niños ni ingestión de agua filtrada.

## 5. Sesiones (P0)

- Planificación 1.º–6.º: área, competencia, capacidades, desempeños **validados**, propósito, evidencia, criterios, instrumentos de evaluación, inicio/desarrollo/cierre con tiempos, materiales, adecuaciones y observación posterior.
- Plantillas offline y sesiones guardadas; duplicar, recuperar tras cierre, versionar, búsqueda, filtros por área/grado/estado y exportar PDF imprimible con marca *Documento elaborado por el usuario* (no oficial).
- Vincular material del Taller, nota del Cuaderno y páginas del PDF sin duplicar datos; eliminar sesión no elimina su fuente o proyecto. Comprobación de distribución de minutos y advertencia si excede.
- Las sugerencias generadas por reglas editoriales requieren revisión; no fingir alineación MINEDU cuando la fuente no se ha cotejado.

## 6. Cuaderno (P0)

- Apuntes, ideas, prácticas, reflexión y evidencias; editor rich-text mínimo seguro y búsqueda. Etiquetas, fechas, enlaces a sesión/proyecto/PDF, anexos e historial.
- Entrada rápida: Nueva nota, Foto/OCR, Grabar voz. Grabación local con permiso de micrófono en uso; **transcripción voz→texto offline no comprometida** si no hay motor instalado/probado. Grabación sí es requisito si módulo aprobado; indicar que la transcripción es fase futura condicional.
- Portafolio: agrupación de notas y evidencias, PDF exportado con previsualización, anonimización/advertencia de privacidad.
- Sin eliminación implícita de notas al eliminar vínculo de un proyecto.

## 7. Biblioteca (P0)

- 12 registros de fuente oficial MINEDU en `06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json` con título, autor/año, enlace oficial, categoría, estatus licencia, disponibilidad local y error si falta PDF.
- Abrir documentos propios y oficiales importados desde `ACTION_OPEN_DOCUMENT`/SAF. Biblioteca `Guardado offline` vs `Solo referencia` señalados con claridad.
- Filtros MINEDU, Currículo, PDF propios, Favoritos, ordenar, búsqueda por metadata y texto **solo en documentos indexados**; la portada del mockup es referencial y no debe copiarse como contenido oficial.
- Última página leída, progreso, marcapáginas, notas y documento destacado de verdad, nunca fechas/páginas/licencias del mockup inventadas.
- Importar ZIP packs de contenidos con validación, derechos, checksum y protección de trabajos personales.

## 8. PDF lector avanzado (P0)

### 8.1 Entrada Android
- `ACTION_VIEW` con `application/pdf` y `content://` para **Abrir con ProfeLab**; `ACTION_SEND` para Compartir hacia ProfeLab; `ACTION_OPEN_DOCUMENT` desde la app. `ACTION_SEND_MULTIPLE` opcional si se valida; no reclamar ser visor universal de otros MIME.
- Lectura de URI `ContentResolver`, sin rutas absolutas, permisos temporales y copia privada si se opta por guardar. MIME/cabecera `%PDF-`, tamaño, páginas, errores, cifrado, documentos dañados y descompresión segura.
- Entrada externa: elección `Leer ahora` o `Guardar en Biblioteca`; nunca copia silenciosa de contenido privado. `Leer ahora` usa almacenamiento temporal y muestra limitaciones de persistencia; al anotar, guardar copia privada o mostrar advertencia explícita.

### 8.2 Anotación y navegación
- Renderizado eficiente (PdfRenderer como base), páginas continuas, saltar a página, miniaturas, doble toque/pinch zoom, orientación y tableta; mantener páginas pesadas fuera de memoria.
- Selección semántica sobre PDF que tiene capa de texto: resaltado/subrayado asociado a text bounds, notas, búsqueda con coincidencias y citas a `docId:pageId:rect`.
- PDF escaneado: resaltado/lápiz por coordenadas manuales; OCR local permite obtener texto revisado e índice por página, sin inventar sincronización semántica perfecta.
- Lápiz táctil y dedo, goma, deshacer/rehacer, colores, grosor, comentarios, marcapáginas; conservar la coordenada normalizada y la transformación a PDF (rotación/cropbox/zoom). Comprobar stroke sobre páginas rotadas.
- Almacenamiento de anotaciones independiente del PDF original (Room), versionado; exportación de **copia PDF anotada** con aplastado o anotaciones persistentes según soporte, documentar modalidad. Nunca sobrescribir automáticamente el PDF original.
- Cerrar/reabrir, girar, actualizar app, copiar PDF: persistencia por ID+hash y no solo `uri.hashCode()`; mantener anotaciones, página y favoritos.
- Compartir copia sin información privada no consentida y con previsualización. Si exportación falla, conservar anotaciones locales y reportar error.

### 8.3 Arquitectura y opciones estudiadas
- Candidato: `PdfRenderer` (visualización), PDFBox-Android (guardar PDF anotado) con pruebas de rendimiento/compatibilidad; referencia de implementación `codegeasse1/docreader` (MIT, Kotlin/Compose, PdfRenderer+PDFBox-Android) sin incorporar su firma/keystore/flujo inseguro.
- Otras opciones `androidx.pdf` solo si cumple mínimos SDK/extensiones y resultados de QA; PDF.js depende de motor web y licencias. No copiar código de proyectos no autorizados.

## 9. Escáner y OCR offline (P0)

- Herramienta **compartida**, no pestaña obligatoria. Desde Cuaderno, Biblioteca, Sesiones y Taller. Cámara con CameraX, enfoque, captura a alta resolución, recorte/rotación, ayuda ante foto borrosa; selector de galería vía Photo Picker/SAF.
- `CAMERA` solo al abrir escáner y cuando procede; botón **flash/linterna** con `hasFlashUnit()` y `enableTorch()` (NO pedir permiso ficticio de flash). Estado se apaga al cerrar/cambiar cámara y maneja hardware sin flash o permiso denegado.
- OCR en español con ML Kit text-recognition bundled para Latin; funciona offline desde primera ejecución, después de instalación; texto extraído pasa siempre a pantalla de **revisión y corrección** al lado de original, guardado explícito. Manuscritos complejos o baja iluminación requieren aviso de baja confianza.
- PDF escaneado: render por página y OCR con límites de tamaño, cola cancelable y proceso incremental. No bloquear UI ni descargar modelo en segundo plano sin autorización.
- Referencia ScanPro amigo: repositorio público sin LICENSE declarado; solo estudiar ideas técnicas y rutas, no copiar código, firma ni assets sin autorización escrita y auditoría legal.

## 10. Permisos, Android y privacidad (P0)

- Definir `minSdk` real tras pruebas de lector y OCR (objetivo inicial API 26 o superior, sujeto a validación, no garantizado). Android 7+ si se demuestra compatibilidad; no copiar sin verificar el valor `minSdk` de un repositorio ajeno.
- Solicitar cámara/micrófono/notificaciones **al entrar en su flujo**, con explicación previa si es necesario. `POST_NOTIFICATIONS` solo Android 13+ si el usuario activa recordatorios. SAF para PDF; sin permisos globales innecesarios. `FileProvider` para compartir, URIs temporales con `FLAG_GRANT_READ_URI_PERMISSION`.
- Sin cuentas obligatorias, sin servidores ni anuncios por defecto. Datos de menores: minimización, seudónimos, controles de exportación; cifrado de respaldo opcional pero recomendable con recuperación segura, nunca incluir credenciales en fuentes.
- Abrir archivo externo no implica publicarlo o transmitirlo. Exportación con vista previa de privacidad y destino elegido por usuario.

## 11. Datos y paquetes (P0)

- Room con entidades IDs UUID y migraciones verificadas para `Profile, Task, Session, Step, Material, InventoryEntry, MaterialRequirement, ProjectTemplate, UserProject, ExperimentRun, Observation, Note, Evidence, Document, DocumentSource, PdfAnnotation, Bookmark, DocumentProgress, OcrDraft, ContentPack, SourceClaim, BackupJob`.
- Plantillas de catálogo son **inmutables**; edición crea `UserProject` clon mediante `forkedFromTemplateId`, así una actualización no altera una creación personal.
- Importar packs en segundo plano transaccional, JSON schema validado, tamaño y rutas seguras, SHA-256 real, versiones semánticas/compatibilidad y rollback, sin descargar automáticamente PDFs no autorizados. Evitar zip-slip, zip bombs, sobreescrituras, entradas faltantes y duplicados.
- Borrado confirmatorio; backup exportado local con manifiesto, medios privados solo con autorización, restauración con preview/merge/no-loss y pruebas de migración. Evitar archivos > límite de Android con procesamiento streaming.

## 12. Diseño UI/UX

- Las 5 capturas son **imágenes de referencia aprobadas**, no layouts editables. No incrustar la imagen de un teléfono como fondo: reconstruir interfaz real con Compose, responsiva, accesible y con estados vacíos. No usar los años o cifras ficticias de los mockups como datos reales.
- Color/espaciado/iconos se estiman de las capturas y se aprueban en QA comparativa; solo el diseñador determina ajustes. En tablet usar dos paneles donde aporta valor (PDF + notas, OCR + texto, sesiones + recursos); navegación adaptativa sin perder el orden de cinco pestañas.
- Para pantallas derivadas, `02_DOCUMENTACION/MATRIZ_PANTALLAS_ACCIONES_V2.csv` y `01_REFERENCIAS_UI/MOCKUP_GALERIA_APROBADA.html` sirven de documentación; no están visualmente aprobadas como las cinco capturas.

## 13. Fuentes y licencias

- Fuentes de MINEDU con registro verificable, derechos separados de acceso; documentos con prohibición o conflicto de aviso NO dentro del APK comercial. Nunca inventar páginas, licencias o contenido extractado; atribuir PDF y afirmaciones pedagógicas individualmente.
- Imágenes de 12 proyectos son **ilustraciones originales conceptuales**, no fotos de resultado, no oficiales.
- ScanPro no muestra LICENSE público; la amistad con autor no autoriza a copiar. DocReader MIT y PDFBox-Android Apache 2.0 ofrecen referencia técnica, pero importar código implica auditoría y conservar avisos; no copiar claves de firma de ningún repositorio.

## 14. Aceptación por hitos / pruebas

- H0: arquitectura, contrato por feature, matríz de trazabilidad, riesgo/atribución revisada y pantalla base real.
- H1: app instalable, tema y navegación, Inicio, tareas/perfil, persistencia y GitHub Actions.
- H2: Taller + inventario, packs, modos práctico/académico, editor de ideas, seguridad/inclusión.
- H3: Sesiones y vínculos estables.
- H4: Cuaderno + portafolio y adjuntos.
- H5: Biblioteca + `ACTION_VIEW`/`SEND` + visor PDF + anotaciones y exportación.
- H6: Cámara, flash, OCR bundled y revisión; búsquedas PDF escaneado.
- H7: pruebas cruzadas, tablet, rendimiento, migraciones, backup, auditoría de licencias y accesibilidad.

Cada hito: código fuente ZIP con rutas exactas, inventario de cambios, instrucciones, reporte QA distinguido `PASS/FAIL/BLOCKED/NOT_RUN`, artefacto compilar mediante Actions. No declarar completado nada que no se ejecutó. Matríz V1.1 de 134 casos se conserva, V2 añade nuevos casos.

## 15. Conflictos conocidos que no se deben ocultar

1. PDF MINEDU: 12 referencias, 0 binarios oficiales incluidos. No simular PDFs locales; habilitar importación legal. Catálogo con licencia CC no significa licencia aprobada para copiar PDF sin revisar avisos internos.
2. SDD vs mockup: imágenes muestran portada/fotos de MINEDU, fechas, páginas y CC probablemente ilustrativos; en producción llenar con datos verificados o estado vacío.
3. Selección de texto en escaneo requiere OCR/rectángulos y revisión; la función de anotación manual no equivale a selección semántica.
4. Transcripción de audio offline requiere motor distinto del OCR; grabación y adjunto pueden ser V1, transcripción texto condicional a prueba real.
5. Experimentos incluidos son propuestas editoriales preliminares, no procedimientos avalados formalmente por MINEDU. Validación previa a publicación infantil.
6. `Abrir con` depende del proveedor de URI y Android; la app debe manejar permisos temporales/expirados sin perder anotaciones.
7. Android 7 y APIs de PDF modernas pueden diferir; establecer y documentar `minSdk` final por pruebas, no prometer compatibilidad universal.
8. No confundir manuales oficiales con resultados creados por ProfeLab; atribución visible y licencia separada.
