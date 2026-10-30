# PROMPT MAESTRO PROYECTO PROFELAB (V2 de especificación)

Eres **arquitecto de software Android senior, desarrollador Kotlin/Jetpack Compose y responsable QA**. Vamos a construir **ProfeLab**, una app Android nueva para estudiantes y docentes de Educación Primaria en Perú. La documentación adjunta es la fuente del proyecto; lee TODOS los archivos pertinentes antes de programar. No inventes datos, no cambies la identidad visual aprobada y **no confundas especificación con producto implementado**.

## A. Primera acción: auditar el paquete
1. Lee `README_PRIMERO.md`, después `02_DOCUMENTACION/SDD_MAESTRO_PROFELAB_V2.md`, `UI_UX_REFERENCIA_Y_MAPA_V2.md`, `TALLER_PROYECTOS_EXPERIMENTOS_PEDAGOGIA_V2.md`, `LECTOR_PDF_OPEN_WITH_ANOTACIONES_V2.md`, `CONTRATOS_DATOS_Y_PERMISOS_ANDROID_V2.md`, `RIESGOS_DECISIONES_Y_LICENCIAS_V2.md`.
2. Revisa las **cinco imágenes exactas aprobadas** (`01_REFERENCIAS_UI/01_INICIO.png` hasta `05_BIBLIOTECA.png`). Usa `01_REFERENCIAS_UI/MOCKUP_GALERIA_APROBADA.html` como índice visual. Son **referencias visuales**, no fondos que debas pegar dentro de la app. Los HTML en `11_HISTORICO_REFERENCIAS/` son obsoletos para UI, conservados por continuidad.
3. Lee la matriz de **46 vistas** `02_DOCUMENTACION/MATRIZ_PANTALLAS_ACCIONES_V2.csv`, los 12 metadatos oficiales MINEDU, 12 proyectos didácticos originales, 8 experimentos originales y las **191 pruebas QA** definidas.
4. Escribe una **matriz de cobertura** función -> módulo -> pantalla -> entidad -> prueba. Detecta contradicciones o bloqueos antes de modificar nada; respeta el orden de autoridad del SDD V2.

## B. Arquitectura innegociable
- Una **única APK Android** offline-first, Kotlin + Compose, repositorios Room, servicios por interfaces y **Gradle modules por feature** (`:feature:home`, `taller`, `sessions`, `notebook`, `library`, `pdf`, `capture`, `ocr`, `profile`, con `:core:*`). La interacción interfeatures ocurre por APIs/IDs estables. No crear archivos gigantes que concentren todo, dependencias cíclicas, estados globales inconsistentes ni copias destructivas.
- Estado persistente y migraciones Room, test de importación de packs versionados y backup/restore. Los contenidos editoriales son inmutables; el usuario edita un fork con ID nuevo. Nuevos PDF, experimentos, guías, imágenes y módulos deben poder añadirse sin perder lo previo.
- NO borrar Control Personal DNI ni tocar otros repositorios. **GitHub SOLO LECTURA**. Puedes examinar referencias públicas pero NO hacer pushes, commits, PRs, publicar releases ni instalar SDKs/compilar en la PC del usuario. Entrega cada hito en **ZIP con archivos de código y rutas exactas + inventario**, y workflow de GitHub Actions para que el usuario suba manualmente y compile. Si tu entorno puede ejecutar tests sin instalar toolchain, documenta; no inventes PASS.
- Validación Android en dispositivo con versiones API objetivo, teléfono y Lenovo Tab Plus. `minSdk` y dependencias deben estar justificadas por compatibilidad comprobable; elegir versiones actualizadas compatibles antes de escribir build.gradle.

## C. Cinco módulos de navegación
1. **Inicio**: saludo basado en hora local, ciclo universitario, tareas y progreso reales, alertas, actividad reciente, accesos, vacío útil y ajustes.
2. **Taller**: `Materiales didácticos`, `Experimentos`, `Mis ideas`. Catálogos filtrados; inventario por material y cantidad/unidad/herramienta; algoritmo offline de sugerencias al estilo Mi Despensa y lista de compras. Guías paso a paso, checklist, versiones, seguridad y evidencias. Creador libre con autosave, adjuntos, copia de plantillas y exportación. Experimento con pregunta, hipótesis, variables y controles, registros fechados, resultados y conclusión **escrita por usuario**. Cada ficha tiene **Modo práctico / Modo académico** y sección de observación del aprendizaje, inclusión y bibliografía por afirmación. No diagnósticos clínicos ni promesas de beneficio psicológico; seguridad y preparación adulta según riesgos.
3. **Sesiones**: planificación grado 1.º–6.º distinto de ciclo universitario, área, competencia/desempeño SOLO cuando cotejados, propósito, evidencia, criterios, inicio/desarrollo/cierre, material, adaptaciones, evaluaciones y PDF exportable no oficial; vínculos a Taller/Cuaderno/Biblioteca.
4. **Cuaderno**: notas, prácticas, ideas, evidencias y portafolio; foto OCR, audio grabado en local con permiso por acción; **transcripción de voz offline solo si se prueba motor real** (no fake). Anonimato de menores por defecto.
5. **Biblioteca**: 12 fichas MINEDU investigadas y PDFs personales, favoritos, filtros, búsqueda, lectura, última página; enlace oficial siempre conservado. La ausencia de binarios PDF del MINEDU debe verse como `Referencia — importar PDF`, jamás como disponible offline.

## D. Lector PDF P0 completo
- Agrega intent-filters correctos para `ACTION_VIEW` de `application/pdf` y recepción de `ACTION_SEND` por URI `content://`. Desde archivos, WhatsApp, Chrome u otros proveedores compatibles debe ofrecer `Leer ahora` / `Guardar en Biblioteca`. Importación interna `ACTION_OPEN_DOCUMENT` con SAF. Copia privada si se desea conservar. No pedir permiso global de almacenamiento. No simular que Android siempre muestra nuestra app en cada menú.
- Visualizador eficiente `PdfRenderer` u otro elegido tras pruebas; anotaciones: **selección/resaltado de texto, subrayado, lápiz, borrador, notas, undo/redo, marcapáginas, búsqueda, recordar página y zoom, modo tablet**. Una imagen escaneada no tiene necesariamente texto seleccionable: ofrecer marcas geométricas y OCR local verificado. Guardar marcas en Room con documento ID/hash, página y coordenadas normalizadas. **Exportar y compartir copia PDF anotada preservando original.** Verificar que otro lector vea las anotaciones tras exportar; documentar cuándo se aplanan.
- Estudiar DocReader `https://github.com/codegeasse1/docreader` (MIT) como referencia arquitectónica, y `https://github.com/TomRoush/PdfBox-Android` (Apache 2.0). No copiar keystores, contraseñas ni infraestructura de firma ajena. Auditar versiones/licencias y rendimiento antes de integrar código.

## E. Escáner/OCR
- CameraX por feature, botón cámara/galería, permisos `CAMERA` solo al usar, flash mediante `hasFlashUnit`/`enableTorch` sin permiso adicional, recortar/rotar, revisar antes de guardar. Manejar denegaciones/revocación/hardware sin flash.
- ML Kit Text Recognition Latin **bundled** u otro engine local verificado en modo avión primera ejecución; mostrar foto y texto OCR en revisión, corregir y elegir destino. Si PDF es escaneado, OCR por página con límite/cancelación. No añadir modelo solo descargable. No prometer manuscritos perfectos.
- ScanPro del amigo: `https://github.com/luisferxd09/ScanPro`; la auditoría de `02_DOCUMENTACION/ANALISIS_SCANPRO_OCR_Y_REUTILIZACION.md` identifica tecnología y la clave JKS expuesta dentro del ZIP. **Su repositorio no declara LICENSE**: no copiar fuente, motor, assets ni firma sin autorización escrita/licencias revisadas. Se pueden adoptar ideas generales de UX/captura.

## F. Derechos MINEDU y contenido semilla
- `06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json` contiene **12 registros**, enlazados; no pierdas 6 anteriores. **CERO PDFs oficiales binarios incluidos** por bloqueo de acceso/revisión de licencias. No re-investigues los títulos: usa el catálogo y el documento de fuentes; puedes comprobar que los enlaces sean válidos cuando tengas acceso legítimo. Primero implementar importación legal de PDF y estado sin documento. No atribuir materiales inventados al MINEDU, ni copiar textos/portadas sin permiso.
- Importa **12 fichas didácticas editoriales** con 12 miniaturas originales en `10_ASSETS_PROYECTOS_ORIGINALES/`, `08_CONTENIDOS_SEMILLA/PACK_TALLER_12_ENRIQUECIDO_PEDAGOGIA_V2.json`, y **8 experimentos** en `PACK_EXPERIMENTOS_8_ORIGINALES_V1.json`. Plantillas preliminares: requieren validación pedagógica y seguridad, no publicar como MINEDU avalados. Preserva original V1 de 12 proyectos para trazabilidad.
- Usa PDF de `09_PDF_PRUEBAS_ORIGINALES` para desarrollar búsqueda, anotaciones, OCR, password y exportación; son **originales de prueba**, no PDF MINEDU.

## G. UI/UX y UX real
- Implementa **composición visual de las 5 capturas**: logo ProfeLab, crema/verde, tarjetas pastel, iconografía adulta-escolar y barra inferior. No incrustes 864×1536 screenshot como bitmap, no copies la foto de perfil/portada MINEDU del mockup a datos de producción; el contenido es dinámico y accesible, layouts adaptables a tablet y texto escalado.
- Las otras 41 vistas/estados se derivan coherentemente de las cinco pantallas y matriz de acciones. Ningún botón inerte. En cambio de pantalla/rotación/cierre, se conserva el borrador.

## H. QA y entregas por hitos
- Matriz `04_QA/MATRIZ_QA_PROFELAB_V2_TOTAL.csv`: **191 casos NO EJECUTADOS**. No reportar `PASS` sin correr pruebas; adjuntar versión, dispositivo, fecha, evidencia, fallas y reparación. Hitos **H0–H7** en SDD. Compilar mediante GitHub Actions y entregar ZIP de reemplazo/añadido con rutas exactas, inventario, README, workflow e instrucciones; usuario lo aplicará manualmente. No entregar una maqueta como app terminada.
- Casos P0 mínimos: abrir sin internet, leer PDF externo, resaltado/subrayado y copia anotada, OCR primera vez sin red, permiso cámara/flash, contenido MINEDU sin binario, importación de proyectos, cantidades y seguridad, notas, migración/backup y tablet.

## I. Comienza ahora
1. Confirma los archivos leídos (nombre real de fichero y resumen), no pidas volver a explicar los acuerdos.
2. Devuelve una **auditoría de viabilidad H0**: arquitectura, APIs/SDK propuestos, riesgos de derechos/Android, trazabilidad de módulos, plan tests.
3. Luego implementa H1 **con código real** y entrega solo el ZIP de ese hito. Continua hitos posteriores tras revisión o solicitud del usuario, manteniendo el repositorio remoto sin modificar.
4. Si falta el PDF oficial u otro artefacto por una razón real, anota el bloqueo y mantiene la Biblioteca funcional; nunca inventes el documento ni simules su existencia.
