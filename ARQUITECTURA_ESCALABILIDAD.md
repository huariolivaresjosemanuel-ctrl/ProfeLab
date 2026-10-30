# Arquitectura y escalabilidad

Se conservan los 16 módulos y la separación feature/core del SDD. El código del Taller se divide por responsabilidades; ninguna feature importa otra feature ni la base de datos.

| Capa / ruta | Responsabilidad |
|---|---|
| core/model/WorkshopModels, InventoryRules | Tipos puros, identidad, conversión y reglas generales de cantidades |
| core/model/Workspace | Contrato transaccional común |
| core/database/WorkspaceRepository | Persistencia Room, revisión y mutaciones atómicas |
| core/content/WorkshopCodec | Adaptación V1/V2, catálogo, alternativas y evaluación |
| core/content/WorkshopSeed | Índice de contenido incluido en APK |
| core/content/ContentPackValidator + ContentPacks | Contrato V2.1, validación e importación offline |
| core/files | Archivos privados y motor común de informe/exportación |
| feature/taller/WorkshopViewModel | Cola de escritura, inventario y compras confirmadas |
| feature/taller/WorkshopShoppingData | Consolidación de selecciones y lista persistente |
| feature/taller/Catalog, Guide, Editor, Inventory, Shopping | Flujos Compose reales |
| feature/taller/Images, Evidence, MaterialForms, Components | Imágenes/zoom/caché, registros, formularios y bloques reutilizables |
| app/ProfeApp, WorkspaceTools | Inyección del contrato existente, cámara/exportación y revisión de paquetes |

Nuevas dependencias: core:content → core:files; feature:taller → core:content, core:files y activity-compose. Continúan sus dependencias existentes. core:files no depende de content; el grafo sigue acíclico (control ejecutado). Profile solo cambia el texto de versión; no se sustituyen los motores funcionales de los otros módulos.

## Crecimiento sin una pantalla por ficha

A. **APK:** `catalog_index_v2.json` lista archivos de proyectos, experimentos, materiales y layouts. Añadir contenido es agregar JSON/imágenes autorizadas y registrar el archivo en el índice. Cada archivo usa su array `projects`, `materials` o `layouts`. Límites actuales: 50 archivos por categoría, 1000 objetos por archivo y 4 millones de caracteres por JSON. Preservar IDs e ilustraciones originales. Los futuros assetsImages deben estar en workshop/images con nombre simple png/jpg/jpeg/webp en minúsculas. El cargador APK confía en contenido editorial incluido en la aplicación firmada; no ejecuta el validador ZIP sobre assets.

B. **Offline:** Perfil → herramientas de respaldo/paquetes → selector SAF. Se inspecciona un ZIP, se presenta el recuento y el usuario confirma su instalación. Las nuevas catalog_project/experiment aparecen en las categorías existentes. Nuevas fichas usan el mismo motor de cantidades, bloques y editor de copia; no requieren escribir Kotlin por proyecto.

Los cinco estilos CONSTRUCTION, DIDACTIC, EXPERIMENT, CREATIVE y ACADEMIC eligen orden inicial de bloques. Un layout puede cambiar sectionOrder. Materiales, pasos, seguridad, fuentes y evidencias se mantienen obligatorios. No hay bloques de código arbitrario, HTML, JavaScript ni navegación nueva proporcionada por un paquete. Un bloque desconocido requiere otra versión del contrato y una nueva APK.

El ID de proyecto es estable; una nueva ejecución usa UUID propio y copia el contenido/layout resuelto. Una actualización del catálogo no reemplaza ese snapshot. Materiales tienen identidad global y las unidades físicas conocidas no cambian con el nombre de la ficha. Colisiones de nombre/sinónimo o de significado físico del mismo ID se rechazan al instalar.

Capacidad preparada para decenas/cientos de fichas, dentro de los límites de recursos documentados; no hay una prueba de rendimiento que certifique ese volumen. Listas exteriores son lazy, bitmaps muestreados y caché limitada a 12 MiB. Una ficha con muchos pasos sigue agrupando varios controles en un bloque; medir memoria, recomposición y desplazamiento antes de distribuir catálogos grandes.
