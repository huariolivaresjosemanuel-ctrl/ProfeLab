# CODEX — PROFELAB COMPLETO V1.0: IMPLEMENTAR TODO EL PROYECTO ORIGINAL EN UNA SOLA ENTREGA

## INSTRUCCIÓN PRINCIPAL (LEER PRIMERO)

Recibes el archivo `PROFELAB_PAQUETE_UNICO_CODEX_PROYECTO_COMPLETO.zip` con el paquete maestro original V2, las cinco referencias visuales aprobadas, contenido educativo semilla, imágenes originales, 191 escenarios QA, PDFs de prueba y expedientes técnicos de reutilización. **Tu encargo es terminar el PROYECTO COMPLETO de ProfeLab, no solo H2 ni un prototipo, y entregar UNA VERSIÓN INTEGRAL DE TODOS LOS MÓDULOS en un ZIP único de código fuente listo para subir manualmente a GitHub.** No te detengas después de cada hito a pedir aprobación: H1.1/H2/H3/H4/H5/H6/H7 son etapas INTERNAS de implementación y validación, no entregas parciales obligatorias. Corrige errores dentro del mismo encargo. Si una restricción técnica o de permisos impide completar algo, detalla el bloqueo real y entrega el máximo proyecto funcional; nunca inventes un PASS ni simules una capacidad.

**Repositorio actual de base — SOLO LECTURA:** https://github.com/huariolivaresjosemanuel-ctrl/ProfeLab (rama `main`). Trabaja en una copia. Es una sola APK Android con applicationId `pe.profelab.app`, 16 módulos Gradle, Kotlin + Jetpack Compose, Room, DataStore, y Android API26+ mientras no exista una razón verificada de incompatibilidad. No inicies otro repositorio ni rehagas H1 que ya existe. Incorpora correcciones desde el estado ACTUAL de `main`, no desde el respaldo H1 antiguo.

**PROHIBIDO:** cambiar los cinco mockups originales, agregar pestañas principales, agregar IA/chat/mascota/suscripciones/cuentas/nube/anuncios u otras funciones no aprobadas; alterar firmas de otras apps; hacer push/commit/PR o modificar GitHub; utilizar secretos dentro del código; borrar datos para resolver migraciones; afirmar que un módulo está listo solo porque tiene UI; presentar falsos PDFs MINEDU descargados o pruebas no ejecutadas.

## ORDEN DE AUTORIDAD Y LECTURA EFICIENTE

1. Estas instrucciones de entrega COMPLETA y los acuerdos ya aprobados por el usuario.
2. `01_ORIGINAL_MAESTRO_V2/01_REFERENCIAS_UI/01_INICIO.png` a `05_BIBLIOTECA.png`: **única autoridad VISUAL** para las cinco pestañas.
3. `01_ORIGINAL_MAESTRO_V2/02_DOCUMENTACION/SDD_MAESTRO_PROFELAB_V2.md` y anexos V2 de UI/UX, Taller, lector PDF, datos, permisos, licencias y matriz de acciones.
4. Contenidos semilla, catálogo MINEDU, imágenes, PDFs QA y matriz de **191 casos**.
5. Arquitectura actual de GitHub y contratos de `core/*` y `feature/*`.
6. V1/V1.1/históricos exclusivamente para continuidad; no sustituyen decisiones V2. Los prompts antiguos del paquete original NO sustituyen este encargo integral.

**Eficiencia:** lee una vez el SDD y crea internamente una matriz requisito → implementación actual → archivo fuente → prueba. Busca en el índice del paquete antes de navegar por internet. Los ZIPs de `03_MODULOS_ZIP/` contienen COPIAS organizadas: no son seis proyectos separados ni debes duplicarlos en el resultado. No repitas investigaciones ya incluidas; verifica información externa solo donde la exactitud/licencia actual lo requiera.

## DISEÑO: EXACTAMENTE CINCO PESTAÑAS

**Inicio | Taller | Sesiones | Cuaderno | Biblioteca**, en ese orden. Reconstruye fielmente con Compose los cinco mockups, sus tarjetas, estilo marfil/verde petróleo/menta/pasteles, iconos, espaciados, tipografía y composición; no incrustes las capturas como pantallas bitmap. Estética adulta cálida y educativa, no infantilizada. Móvil y Lenovo Tab Plus: responsive, orientación, ventanas/insets, fuentes grandes y stylus donde corresponde. Las vistas derivadas y herramientas están DENTRO de estas secciones; **PDF, Cámara, OCR y Portafolio NO son pestañas sexta/séptima**. Conserva Inicio, tareas, perfil y navegación ya existentes.

## FUNCIONES OBLIGATORIAS — COMPLETARLAS TODAS

### A. Inicio / H1
Saludo local, perfil y ciclo universitario (NO confundir con grado escolar), tareas completas con prioridad, notas, fecha, filtros, borradores, vencimientos, recordatorios contextuales opcionales, progreso semanal basado en datos reales, actividad reciente, pendientes y accesos a las demás secciones. Respetar datos anteriores y estados vacíos; nada de ejemplos ficticios en la base real.

### B. Taller / H2
Usar físicamente los **12 proyectos originales enriquecidos + 12 miniaturas**, **8 experimentos originales** y sus manifiestos de `08_CONTENIDOS_SEMILLA/` y `10_ASSETS_PROYECTOS_ORIGINALES/`. Subapartados INTERNOS: `Materiales`, `Experimentos`, `Mis ideas`.
- Catálogo real: búsqueda y filtros por grado 1–6, área, duración, dificultad, recursos, categoría, favoritos y seguridad; fichas con objetivo, cantidad/unidad, herramientas, preparación adulta, checklist persistente, pasos, variantes, evidencias y fuentes.
- Inventario offline tipo Mi Despensa: registrar cantidad/unidad, herramientas, consumibles/reutilizables, alternativas justificadas, reservas, clasificación `Puedo hacerlo` / `Me falta` / `Preparación adulta` / `No recomendado`, cobertura real por cantidades y lista de compras consolidada editable. Jamás suponer disponibilidad sin cantidad/unidad.
- Experimentos/indagación: pregunta, hipótesis editable, variables, controles, procedimiento, fecha, observaciones reales, imágenes opcionales, análisis, conclusiones del usuario, informe PDF. **Los ocho experimentos están pendientes de revisión pedagógica/seguridad; NO publicitarlos como aprobados**.
- Mis ideas: autoría desde cero, notas, fotos, materiales, boceto/anexos, pasos reordenables, edición, duplicado con nuevo ID, autosave, restauración, versión/exportación.
- Modo práctico y modo académico por actividad: competencias/capacidades SOLO cotejadas, citas, limitaciones, adaptaciones e inclusión no diagnóstica, listas de cotejo/rúbricas/registro anecdótico, observaciones antes/durante/después. Plantillas inmutables; obras del usuario no se sobrescriben al actualizar contenido.

### C. Sesiones / H3
Seguir estrictamente `03_SESIONES.png`: editor real de sesiones 1.º–6.º de Primaria con grado, área, competencia/capacidad/desempeño con fuente cotejada, propósito, evidencia, criterios, instrumentos de evaluación, inicio/desarrollo/cierre con minutos validados, recursos, adaptación y reflexión/observación; plantillas offline, búsqueda, filtros, estados, borradores recuperables, editar, duplicar, versionar, exportar PDF **identificado como elaboración del usuario, no documento oficial**. Vínculos por ID a material del Taller, experimento, nota de Cuaderno y PDF/página de Biblioteca; eliminar vínculo no elimina fuentes.

### D. Cuaderno / H4
Seguir `04_CUADERNO.png`: subapartados INTERNOS Apuntes, Prácticas e Ideas; editor de notas reales con búsqueda, etiquetas, fechas, adjuntos, recuperación, evidencias, reflexión, portafolio por proyecto/sesión, exportación PDF con vista previa y tratamiento de datos personales. Entrada rápida: nueva nota, foto+OCR (herramienta compartida), grabación de audio LOCAL con permiso contextual; **no fingir transcripción voz→texto** sin motor probado y aprobado.

### E. Biblioteca MINEDU + lector PDF / H5
Seguir `05_BIBLIOTECA.png`; catálogo de **12 referencias oficiales verificadas**, etiquetas de derechos y disponibilidad real. No existen 12 PDFs oficiales binarios autorizados en el paquete. Incluir búsqueda/filtros MINEDU/Currículo/PDF propios/Favoritos, historial, continuar lectura, importar PDF mediante SAF; marcar claramente `Solo referencia` o `Guardado offline`.
- `ACTION_VIEW`, `ACTION_SEND`, `ACTION_OPEN_DOCUMENT`, MIME `application/pdf` y URI `content://`; desde Archivos/Descargas/Compartir. Preguntar `Leer ahora` temporal vs `Guardar en Biblioteca` privado; copiar a almacenamiento privado solo tras elección; gestionar expiración de permisos, tamaño y cifrado sin `MANAGE_EXTERNAL_STORAGE`.
- Lector offline real: render progresivo, navegación y página, zoom, miniaturas, orientación, reanudar progreso, marcadores, búsqueda en PDF con capa de texto, manejo de documento pesado, cifrado y errores.
- **Anotaciones reales y persistentes**: resaltado y subrayado semántico cuando existe capa de texto, trazo manual y stylus para escaneados, colores/grosor, borrador, deshacer/rehacer, comentarios, notas ancladas a documento+página+coordenadas/rotación, guardar en Room fuera del archivo original. La selección semántica NO equivale a dibujar rectángulos; si algo no funciona, declararlo.
- Exportar COPIA anotada PDF (visible al reabrir en otro lector, incluso impresa), compartir mediante FileProvider y preview; no modificar PDF original ni perder marcas tras cierre/rotación/actualización.
- Vínculos a Sesiones, Cuaderno y Taller por IDs y página. Fixtures QA incluidas: PDF seleccionable, PDF escaneado, PDF con contraseña, imagen OCR.

### F. Cámara / OCR / H6
Herramienta compartida desde módulos existentes, NUNCA pestaña adicional. CameraX con enfoque, recorte/rotación, selección de imagen mediante Photo Picker/SAF, torch si `hasFlashUnit()` y apagar al salir; permiso CAMERA solo al usar. OCR con **ML Kit Text Recognition Latin BUNDLED** sin descarga en primer uso modo avión, proceso cancelable y procesamiento incremental de PDFs escaneados, original visible + texto reconocido corregible, guardar explícitamente el texto revisado y enviarlo a destino. Avisar límites de manuscrito/fotos malas, no inventar exactitud. Grabación de voz ≠ OCR.

### G. H7 final
Integración cruzada, importación/validación de packs con checksum, antizip-slip/rollback, backups locales con vista previa y restauración controlada, exportaciones, migraciones Room versión por versión sin `fallbackToDestructiveMigration`, persistencia de tareas/ideas/notas/sesiones/PDF/anotaciones/evidencias al actualizar, accesibilidad y rendimiento, API 26 y 36, móvil y tablet, modo avión y permisos denegados, 191 casos QA. No instalar datos privados ni inventar capturas de emulador.

## REUTILIZACIÓN DE FUENTES PARA AHORRAR TIEMPO/TOKENS

- **Primero reutiliza TODO lo ya incluido**: contratos Core/Feature del repo actual, SDD, matrices, contenidos semilla, mockups, JSON, fotos editoriales originales y PDF de prueba. No reescribas historias, experimentos o requisitos desde cero si ya están estructurados.
- **DocReader MIT**: referencia permitida y candidato a adaptar para H5. En `02_REPOSITORIOS_EXTERNOS/DOCREADER_MIT` hay LICENCIA MIT, inventario de archivos y un `DESCARGAR_DOCREADER_SANITIZADO.ps1` que obtiene el repositorio fijado al commit `092d7fd48eae18d2409d8c97149620218db99657` y elimina claves/workflows ajenos. **El código de DocReader NO está físicamente empaquetado**: si tu entorno tiene acceso a GitHub, usa directamente la URL fijada UNA SOLA VEZ; si no, implementa sobre APIs oficiales. No paralices todo el producto por la descarga. Adapta componentes permitidos a `:feature:pdf`, Room, FileProvider y UI ProfeLab; conserva avisos MIT en el resultado. El código fuente externo NO es reemplazo completo de la app.
- **ScanPro SIN LICENSE público**: expediente técnico en `02_REPOSITORIOS_EXTERNOS/SCANPRO_SIN_LICENCIA` y el análisis original. No distribuir/copiar/adaptar su código ni claves hasta permiso escrito verificable para uso previsto; usar CameraX + ML Kit bundled en implementación propia. Prohibido incluir `scanpro.jks` o claves de DocReader.
- PDFBox-Android / Android PdfRenderer: evaluar versiones, licencias, API26, memoria, anotación vectorial/semántica y exportación; no declarar prestaciones por README.
- MINEDU: catálogo y links verificados en documentación; **sin PDF oficial binario en el paquete** y licencias concretas no aprobadas. No fabricar PDFs oficiales ni distribuir documentos sin permiso.

## FIRMA PERMANENTE Y ACTUALIZACIÓN SIN PÉRDIDA

Conserva `pe.profelab.app` y la infraestructura H1.1 ya incorporada en `app/build.gradle.kts`, `scripts/signing.py` y `.github/workflows/android.yml`; revisa estado actual de GitHub. Configura flujo de firma **estable, bajo control del usuario**, con `PROFELAB_KEYSTORE_BASE64`, `PROFELAB_KEYSTORE_PASSWORD`, `PROFELAB_KEY_ALIAS`, `PROFELAB_KEY_PASSWORD` como secretos y huella pública verificable. No incluir keystore, contraseñas, certificados privados, claves externas ni secretos en ZIP/GitHub. Proveer instrucciones claras para crear/guardar la clave fuera del repo y configurar secretos por el usuario. Producir artefactos debug y release distinguibles y prueba de firma/`versionCode` mayor. **H1 debug posiblemente tiene otra firma; NO prometer actualización in situ si el certificado anterior no coincide ni pedir desinstalar con datos sin respaldo**. Probar actualización real de dos APK firmadas igual cuando las claves y el dispositivo estén disponibles, o declarar BLOCKED.

## GITHUB ACTIONS: SOLO MANUAL

`.github/workflows/android.yml` DEBE quedar con `on: workflow_dispatch` exclusivamente. Eliminar `on: push`, `pull_request`, `schedule` y cualquier otro disparador que compile al subir `.md`, `.yml`, `.gitignore` o código. Conservar opciones manuales de emulador API26/API36 y `signed_release`; no lanzar Actions desde Codex. Tests y release solo bajo ejecución explícita del usuario. Compilación reproducible sin instalar toolchain local en el PC del usuario.

## PRIVACIDAD, INTEGRIDAD Y CALIDAD

Offline-first, sin red requerida para funciones esenciales, sin cuenta, sin datos de menores innecesarios, notas anónimas por defecto, preview antes de exportar. Solicitar permisos al usar la acción. IDs UUID/referencias por ID; Flow, repositorios y contratos entre módulos; no monolito en MainActivity. Cada botón debe funcionar, quedar explícitamente deshabilitado o retirarse; no falsos estados de éxito. Reglas de seguridad física en experimentos y atribución de fuentes y licencias. Validar la UI contra los 5 PNG con capturas reales si tu entorno dispone de emulador; de lo contrario declarar NOT_RUN sin inventarlas.

## ENTREGA ÚNICA REQUERIDA (NO SOLO UN PARCHE H2)

Entrega al FINAL un archivo **`PROFELAB_COMPLETO_V1_0_FUENTES.zip`** con el **proyecto Android COMPLETO** como raíz (todos los archivos necesarios para clonar/subir y compilar, incluidos `app/`, `core/`, `feature/`, `gradle/`, `gradlew`, `gradlew.bat`, archivos `.kts`, recursos, tests, `.github/workflows/android.yml`, `.gitignore`, licencias y documentación). No solamente los cambios ni un listado de tareas: entregable íntegro listo para colocar en el repositorio, sin llaves privadas, builds ni cachés. Incluir los recursos propios aprobados empaquetados cuando corresponda; no fusionar códigos fuente externos enteros si solo usas algunos componentes licenciados.

Dentro o junto al ZIP incluye:
- `LEER_PRIMERO_SUBIDA_MANUAL.md`: qué subir y cómo; confirmar que la raíz del ZIP es raíz del repositorio.
- `INVENTARIO_COMPLETO.csv` con ruta/tipo/módulo/hash y origen.
- `MATRIZ_COBERTURA_REQUISITOS.csv` con cada requisito del SDD y 191 QA enlazados a código/estado.
- `REPORTE_QA_REAL.md` con PASS/FAIL/BLOCKED/NOT_RUN por compilación, lint, JVM, instrumentadas, UI, PDF, OCR, migración y actualizaciones, más enlaces a evidencias si existen.
- `INFORME_FIDELIDAD_UI.md` comparando los cinco mockups con interfaz resultante y diferencias justificadas; capturas REALES si existen.
- `FUENTES_Y_LICENCIAS.md` con atribución DocReader si reutilizado, licencias de bibliotecas y derechos/estado MINEDU.
- `GUIA_FIRMA_Y_ACTUALIZACION.md` con keystore seguro y transición H1 debug → firma permanente sin prometer preservación de datos si incompatibles.
- `PENDIENTES_REALES.md` SOLO para problemas genuinamente no resueltos y su efecto; no esconder requisitos incompletos.

Entrega el trabajo completo en una sola respuesta final con ZIP y resumen de cobertura. No me entregues únicamente un prompt, un scaffold o siete ZIP parciales ni te detengas a pedirme permiso para seguir entre módulos. Puedes hacer pruebas internas progresivas, pero el objetivo es **una app integral única fiel al diseño y especificaciones originales**.

Si el entorno no permite compilar/probar, no declares PASS: deja Actions manual listo, registra las pruebas no ejecutadas y entrega todo el código desarrollado. Si una función requiere acceso físico, clave privada o autorización de tercero no disponibles, implementa lo técnicamente posible sin fingir esa validación y reporta el bloqueo con instrucciones de aceptación.

**EMPIEZA AHORA.** Primero audita brevemente H1.1 y el material del ZIP; después implementa H2 a H7 consecutivamente en el mismo proyecto y entrega `PROFELAB_COMPLETO_V1_0_FUENTES.zip` al concluir. La prioridad es fidelidad a lo aprobado, no inventar funciones, preservar datos, reutilizar el material provisto y obtener una única entrega final funcional.
