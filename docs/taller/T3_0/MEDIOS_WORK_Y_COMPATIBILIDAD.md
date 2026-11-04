# Integración preparada para medios de Work — Taller 3.0

Este parche implementa slots y un adaptador. No contiene las fotografías finales de Work, no aprueba imágenes y no convierte los siete mockups en recursos Android. Su ausencia no bloquea las ventanas ni el texto del procedimiento.

## Slots de la aplicación

`WorkshopMediaSlot(record, role, stepId?, materialId?, modifier)` reutiliza WorkshopImages, almacenamiento privado, caché acotada y zoom. El contenedor conserva estas proporciones: catálogo/apoyo docente 3:2, ficha/paso/resultado 16:9 y material/sector1:1. Solo una imagen real admite zoom. Una referencia ausente, hash incorrecto, imagen ilegible o conflicto de revisión deja un estado funcional sin asignar otra fotografía.

La resolución exige contentId y contentVersion exactos, role y stepId/materialId cuando correspondan. Una portada no sustituye un paso. La misma foto puede ocupar dos roles únicamente si ambos vínculos están declarados y aprobados. Dos revisiones iguales con distintos hashes son ambiguas y no se eligen por fecha o título. Una revisión superior es un cambio visual, no una modificación del progreso o del inventario.

La identidad pública es el ID persistente del registro/namespace del paquete. Un activityId arbitrario dentro de una definición pública no puede suplantar otra actividad. En una copia personal se conserva el origen de su definición. Los proyectos personales antiguos sin versión suficiente no reciben un medio de otra versión por intuición; mantienen sus recursos anteriores o el placeholder.

Cada fotografía de paso conserva además una huella SHA256 de la pareja JSON [título, instrucción] de la definición verificada. El instalador comprueba esa huella antes y dentro de la transacción. Si una copia personal modifica el procedimiento conservando el ID, deja de mostrar esa foto editorial; done y notas no alteran la huella. Las evidencias privadas solo sustituyen la portada tras elegir explícitamente «Usar como portada»; adjuntar una foto no cambia automáticamente la portada.

Las versiones 2.0.0 de los 12 proyectos y 1.0.0 de los 8 experimentos se obtienen de las cabeceras reales al leer los catálogos. Esta proyección no reescribe copias personales previas. Los JSON originales y sus 80 pasos no se modifican.

`assets/workshop/media_slots_v1.json` registra los 20 IDs y los 80 stepIds actuales y tiene `resources: []`. Es cobertura pendiente, no un manifiesto de fotos producidas. El motor usa N pasos del contenido, no exige exactamente cuatro ni limita nuevos paquetes a esos veinte IDs.

## Conversión posterior del lote editorial

El JSON WORK_VISUAL_DELIVERY_1 no se presenta como un manifest.json importable directamente. La API `WorkVisualAdapter.toContentPack(manifest, files, definitions, packId, packVersion, materialCatalog)` realiza una conversión explícita a los mismos paquetes offline que ya valida ContentPacks. No se añade otra base de datos ni un importador que ignore las verificaciones existentes.

Antes de utilizarla con un lote real:

1. Recibir físicamente los archivos de Work; cotejar HEAD, actividad, versión, pasos, seguridad, derechos y contenido de cada foto con el usuario.
2. Mantener las entradas pendientes fuera del listado de archivos disponibles. TEMPLATE_NOT_REAL_OUTPUT, PENDING_HUMAN_APPROVAL o PENDING_REVIEW no pasan el adaptador.
3. Solo tras aprobación real, el manifiesto declara reviewStatus=APPROVED_BY_USER y safetyReviewStatus=APPROVED / APPROVED_BY_USER / NOT_REQUIRED cuando sea pertinente. El adaptador valida esas declaraciones; no certifica por sí mismo que se haya realizado la revisión humana.
4. El mapa files contiene exactamente los binarios de medios declarados, por relativePath. No incluye README, mockups, archivos extra ni rutas hipotéticas. El adaptador verifica existencia, SHA256, MIME, dimensiones reales, derechos, límites, referencias e IDs. Admite PNG/JPG/JPEG/WebP. Reutilizar un binario en varios roles requiere entradas lógicas explícitas.
5. Proporcionar las definiciones públicas vigentes y el catálogo de materiales real. Un medio para una actividad o versión desconocida, un stepId ajeno o un material inexistente se rechaza. Los seis iconos del portal usan contentId=sector:experiments/taller/cooking/dance/heritage/ideas, versión1.0.0 y role=sector_icon; un icono no activa su categoría.
6. Escribir el mapa devuelto en un ZIP y pasarlo al flujo normal SAF de ContentPacks: previsualizar y después instalar manualmente. Esta conversión es una API para la siguiente integración de recursos; este parche no añade un botón que acepte sin revisión cualquier ZIP editorial.

El resultado utiliza la extensión schema3/contentContractVersion3.0, tipo workshop_media. Las imágenes pasan por la misma validación de bytes, MIME, tamaño y licencias del importador. Los metadatos del slot también deben coincidir con el recurso validado. Una versión antigua sin esta extensión rechazará el tipo; se requiere la APK con este parche, aunque la etiqueta pública siga siendo1.0.2. No se modifica la firma o el esquema Room para incorporar el contrato.

Se mantienen límites actuales: hasta180 items de paquete,200 entradas ZIP,8MiB por imagen y40MiB por lote. Un banco completo que los supere debe dividirse en lotes por actividad/categoría, no desactivar comprobaciones ni aumentar límites silenciosamente.

## Instalación y actualización sin pérdida

Un paquete de medios crea registros catalog_media y el marcador de versión habitual. No clona las actividades base ni escribe en project/experiment/inventory. La vinculación es una proyección de lectura por CompositionLocal; puede actualizar la apariencia de una definición compatible sin alterar la copia personal, sus notas, balances, reservas, done, último paso, conclusión o adjuntos. Se validan los objetivos antes de copiar medios y nuevamente en la transacción de instalación.

Los fileIds privados se asignan por PrivateFiles durante la instalación. Los mediaIds son identidades editoriales, nunca rutas privadas. No se aceptan fileIds de un ZIP externo como autoridad sobre archivos preexistentes. resourceFileIds permite que el respaldo actual incluya las fotografías y recupere sus vínculos; no requiere migración de Room3.

Los adjuntos privados se reconocen por la relación attachments y su MIME, no por palabras del texto alternativo. Una imagen editorial, incluso si su alt comienza con «Fotografía», nunca se añade a records/attachments como evidencia propia. Las referencias de resultado se rotulan como ilustrativas, no como datos observados.

Para nuevo contenido compatible: instalar primero su paquete de definiciones validado y después el lote de medios correspondiente a su namespace/versión. Los paquetes de contenido originales con images por definición/paso siguen funcionando. Cocina/FOOD y dominios incompatibles conservan sus bloqueos y requieren reglas y APK específicas, no solo nuevas fotos.

## Validación preparada

WorkVisualAdapterTest contiene casos Android para PNG/JPEG/WebP, aprobaciones pendientes, plantilla vacía, versión/paso erróneos, hash/MIME/dimensiones/rutas, archivos ausentes/corruptos/extra, reutilización explícita por roles, revisiones ambiguas, suplantación de IDs, instalación repetida/actualización sin pérdida, actividad nueva de siete pasos y respaldo de archivos/vínculos. Son fixtures sintéticos de prueba, no recursos de Work aprobados. Su ejecución requiere el run Android del usuario; no se declaran aprobados por inspección del código.
