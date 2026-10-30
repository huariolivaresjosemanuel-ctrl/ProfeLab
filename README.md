# ProfeLab · actualización de fuentes 1.0.2

Continuación de `main` en `43ff3eead533105006f7da851784cba7ace063a9` (30/10/2026), con Taller V1.0.1 conservado. Proyecto Android real de 16 módulos, cinco pestañas, `pe.profelab.app`, API26+, Kotlin/Compose, Room2 y DataStore. No incluye APK compilada. GitHub se consultó solo mediante lecturas; no se ejecutó Actions ni se hicieron commits o cambios remotos.

## Estado de esta actualización

Se corrigen en fuentes las firmas JVM no-void de tres tests instrumentados y la espera de navegación de Taller que dependía de un título fuera del área compuesta. Se conservan assertions, pruebas y timeout. El run histórico `41096812092` compiló, pero falló tres casos de app por API26/API36; ese resultado no valida las fuentes nuevas.

Incluye Docente «Cómo enseñarlo» junto a Práctico/Académico en las fichas del Taller: guion breve/desarrollado, edición persistente, fallback para fichas sin guion y exportación. Los doce proyectos y ocho experimentos originales conservan sus IDs y contenido. El overlay del Geoplano corresponde a **mat-003**, no mat-001. Los experimentos mantienen observaciones, mediciones y fotos propias; sus ocho portadas siguen pendientes de aprobación, con estado faltante explícito y sin resultados ficticios.

Se ajustan las cinco pantallas Compose hacia los diseños aprobados sin agregar pestañas. Hay registro local opcional, saludo por nombre/rol/hora, ajustes de mascota y movimiento reducido. Concepto C usa el master aprobado sin alterar; solo entrada por opacidad, sin presentar poses o GIF experimentales como animación final.

El respaldo incluye preferencias de forma aditiva, referencias a medios, borrador OCR con imagen retenida y páginas de lotes OCR pendientes. La restauración fusiona sin sobrescribir IDs existentes y usa un journal Room para recuperar preferencias si DataStore falla. No hay nueva versión de Room ni migración destructiva. Los motores PDF, cámara y reconocimiento ML Kit conservan sus implementaciones; se ajustan almacenamiento/sincronización de borradores OCR.

Packs V2/V2.1 siguen compatibles; V3 agrega guiones, categorías y metadatos de imágenes. Las extensiones MiniChef/FOOD y medios audio/video siguen como contratos futuros, sin publicación de recetas o talleres no revisados. Se conservan inventario, alternativas, reservas, recomendaciones, compras, Sesiones, Cuaderno y Biblioteca.

## Validación y distribución

Se ejecutaron 16 controles estáticos, 11 pruebas Python del helper de firma con herramientas simuladas, auditoría de delimitadores, preflight de contenido y migración SQLite en archivo con reapertura. Esto **no es compilación Kotlin ni ejecución Android**. No hay JDK/SDK/Gradle/adb disponibles en el entorno y no se instalaron. Los tests JVM, instrumentados, lint, actualización real y comparación visual están NOT_RUN/BLOCKED para estas fuentes. La entrega incremental adjunta sus logs y matriz QA en `03_INSTRUCCIONES`; los informes históricos conservados en esta raíz y `docs/` no acreditan esta versión.

`.github/workflows/android.yml` se inicia exclusivamente con **Run workflow** (`workflow_dispatch`). Mantiene builds/lint/JVM y pruebas Android API26/API36. Incluye una prueba preparada de dos reemplazos sin desinstalar, con APK DEBUG4→5→6 creadas con la misma clave dentro del job y comprobación de datos/UID/certificados. Aún no ejecutada; no demuestra compatibilidad con la APK que tiene el usuario.

La APK instalada del usuario procede de un artefacto DEBUG de Actions. Su certificado todavía no se comparó. No instalar un release de distinta firma esperando conservar datos automáticamente. La firma permanente existente requiere claves custodiadas por el usuario, los cuatro secretos y huella pública configurados; no se incluyen claves ni se reemplazan. El release usa `1000 + GITHUB_RUN_NUMBER`, mayor que `PROFELAB_LAST_DISTRIBUTED_VERSION_CODE`; el DEBUG predeterminado usa5. La APK permanente publicada se llama `ProfeLab-V1.0.2-release.apk`.

Antes de cambiar de origen, terminar revisiones OCR antiguas, guardar documentos temporales SAF en la Biblioteca y exportar respaldo completo con archivos. Verificar hashes, referencias y restauración en un entorno de prueba independiente antes de considerar una desinstalación autorizada. Permisos SAF externos no se transfieren entre instalaciones; ningún backup recupera archivos ya ausentes. El agente no desinstaló nada.

Los archivos de producción se aplican desde `01_IMPLEMENTAR/` y `02_REEMPLAZAR/` del ZIP incremental, conservando rutas. `03_INSTRUCCIONES/`, propuestas gráficas y el SHA del paquete no van en la raíz del repo. El `SHA256SUMS.txt` ubicado dentro de `02_REEMPLAZAR/` sí es el manifiesto renovado de fuentes. La guía externa explica cada paso manual.

Se mantienen licencias y atribuciones existentes, incluida la adaptación parcial de DocReader MIT; no se incorporó código de ScanPro ni PDFs oficiales sin permiso. No hay login, anuncios, suscripciones ni servicios remotos nuevos.
