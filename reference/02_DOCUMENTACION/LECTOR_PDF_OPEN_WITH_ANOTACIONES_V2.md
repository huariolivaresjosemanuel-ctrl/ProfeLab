# Contrato del lector PDF avanzado, recepción de archivos y OCR

**Estado P0 aprobado:** PDF recibidos por Android, resaltado, subrayado, lápiz, notas, marcadores, exportar copia y compartir. **No se limita a mostrar imágenes de página**.

## Requisitos `PDF-*`

- `PDF-01` Intent `ACTION_VIEW`, MIME `application/pdf`, URI `content://`; filtrar asociaciones apropiadas sin secuestrar otros archivos. Ejecutar desde Chrome/Descargas/Archivos y proveedor externo con cada versión Android objetivo.
- `PDF-02` `ACTION_SEND` con MIME PDF y `EXTRA_STREAM` para compartir. Si `ACTION_SEND_MULTIPLE` se implementa, debe procesar múltiples URI de forma atómica por archivo, sin duplicados ni pérdida.
- `PDF-03` Elección `Leer ahora` (temporal, informa persistencia) / `Guardar en Biblioteca` (copia privada y verificación de integridad). Abrir sin permisos no solicitados, manejar URI nula, `SecurityException`, contraseña, error de lectura, daños y tamaño > límites.
- `PDF-04` `ACTION_OPEN_DOCUMENT` dentro de Biblioteca; URI persistente solo si procede y el proveedor lo permite, copiar si el archivo debe permanecer. No usar `file://` global ni `MANAGE_EXTERNAL_STORAGE`.
- `PDF-05` motor `PdfRenderer` y almacenamiento lazy, paginación, zoom anclado, minimapa/miniaturas, reanudar página y offsets, rotación y tablet.
- `PDF-06` resaltar/subrayar **texto seleccionable** cuando hay text layer; si no, modo dibujo/selección geométrica y OCR con texto revisado. Detallar UX del cambio, no fingir que existe capa de texto.
- `PDF-07` lápiz y stylus, borrador, reordenar herramientas, elegir color/grosor, undo/redo persistible y eliminar nota individual.
- `PDF-08` anexar notas por ancla `docId`, `pageNumber`, bounding boxes normalizadas/rotación, `quote` opcional y comentario editable; índice de anotaciones.
- `PDF-09` guardar anotaciones **fuera del PDF original**, con ID estable, hash del documento/versión y Room transaccional, revisión de original reemplazado. Evitar confiar solo en hash de URI.
- `PDF-10` buscar texto en PDFs de texto; en escaneados solo después de OCR local. Mostrar progreso, cancelación, límites y resultados por página.
- `PDF-11` guardar **copia anotada** en PDF con raster/vector según soporte; avisar si se aplana. Mantener original, previsualizar, validar que las marcas sobrevivan a reapertura en otro lector y que se vean en impresión.
- `PDF-12` compartir copia PDF anotada con FileProvider/URI temporal; revocar permisos cuando proceda, evitar fugas en logs.
- `PDF-13` cifrado: solicitar contraseña local en PDF cifrado; no almacenarla en texto plano ni ocultarla dentro de backups.
- `PDF-14` documentos grandes: scroll y render por página; no renderizar todos los 400+ folios en RAM; límites de OCR por lote y recuperación de interrupción.
- `PDF-15` anotación se mantiene tras rotación, Android kill, exportación y actualización sin perder marcador.
- `PDF-16` vincular fragmentos a Cuaderno/Sesiones/Taller por IDs y cita documento:página; si se elimina el vínculo, la fuente no desaparece.

## Modelo de anotación sugerido
`annotationId(uuid)`, `documentId`, `sourceFileSha256`, `pageIndex(0-based)`, `type(HIGHLIGHT|UNDERLINE|PEN|NOTE)`, `pointsNormalized`, `rectNormalized`, `textQuote`, `comment`, `colorArgb`, `strokeWidth`, `createdAt`, `updatedAt`, `revision`, `rotationAtCapture`, `origin(TEXT_LAYER|MANUAL|OCR_REVIEWED)`.

## Alternativas y decisiones
- `PdfRenderer` para visualizar, PDFBox-Android (Apache-2.0) como candidato para edición/exportación, previa prueba de memoria y coordenadas. `DocReader` MIT como ejemplo; no copiar claves ni credenciales GitHub que estén en repositorios de ejemplo.
- `androidx.pdf` candidato secundario condicionado a versión de Android/extensión; ni prometer acceso completo en Android 7–11 ni usar solo una versión beta sin fallback.
- Lápiz de tablet Lenovo: usar `MotionEvent` y presión solo si hay soporte; dedo debe seguir funcionando; palm rejection según API y hardware, nunca garantizarlo.

## Errores evitados
Un `URI content://` puede caducar. Si usuario anota un PDF abierto temporalmente, antes de almacenar cambios debe proponer guardar copia persistente. Si PDF cambia de hash, no pegar anotaciones automáticamente a páginas equivocadas. Rechazar anexos ejecutables y PDFs maliciosos con ruta segura y procesamiento sandbox por los APIs disponibles.
