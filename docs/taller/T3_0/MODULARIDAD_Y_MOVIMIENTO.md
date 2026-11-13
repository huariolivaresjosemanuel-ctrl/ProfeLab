# Taller — recursos modulares y movimiento nativo

Base real: main05be1608b4eeaab747435fe4de25f34f7eff136d, consultada en solo lectura. La base tenía180de los199WebP declarados y19ausencias, incluidas9fotos de pasos. Se recuperaron los19binarios originales desde la entrega anterior, con SHA256 idéntico. La organización final contiene199WebP únicos,229vínculos,20actividades,80pasos y45identidades de materiales; cero recursos pendientes por falta de fuente.

## Organización incluida en la APK

`assets/profelab/taller/experiments/<id>/cover|steps|outcomes|teacher/` y `assets/profelab/taller/projects/<id>/...` contienen los recursos propios de actividad. Los materiales se deduplican en `assets/profelab/shared/materials/`; un binario reutilizado entre actividades y roles distintos puede residir en shared/media. Se mantienen hashes completos en el índice y nombres internos abreviados, con detección de colisiones. Las carpetas cooking/dance/heritage/ideas son posibles rutas de organización, no activación de categorías sin validador.

`profelab/media_index_v1.json` relaciona cada archivo físico con sus roles y actividades. `workshop/media_slots_v1.json` sigue siendo el contrato de vínculos y ahora utiliza assetPath. `MEDIA_PATH_MIGRATION.json` relaciona las199rutas antiguas y nuevas con su hash original. No cambia ningún contentId,stepId,materialId,contentVersion o huella del procedimiento. Las imágenes no son cantidades ni existencias.

## Compatibilidad y orígenes independientes

WorkshopAssetPaths valida segmentos y raíces, rechaza rutas absolutas, URI, traversal, escapes y rutas modulares ausentes del índice. Traduce los199nombres planos antiguos al nuevo destino. Un registro personal o respaldo con assetFilename antiguo continúa funcionando sin modificar su JSON. Las ilustraciones PNG legadas permanecen en su ubicación original.

Las rutas de APK nunca se confunden con UUID privados. WorkVisualAdapter y ContentPacks retiran assetPath/assetFilename/legacyAssetFilename externos al instalar medios como archivos privados. No se modifican permisos, el almacén privado, los contratos del importador ni la validación de firma. Una foto privada elegida como portada conserva prioridad.

Después de copiar código, índice y recursos, deben retirarse SOLO los180WebP planos que existían en main. El ZIP incremental proporciona mapa, lista y herramienta manual de retirada: comprueba todos los destinos y hashes antes de retirar duplicados. Los otros19no existían remotamente. Mantener ambos juegos no rompe la compatibilidad, pero duplica innecesariamente recursos de la APK y deja el inventario de archivos desajustado.

## Carga solicitada y caché

WorkshopBundledMedia.refs valida únicamente metadatos, identidad, versión, pertenencia y huella. Abrir Taller o el catálogo no abre ni calcula hashes de las199fotos. WorkshopPicture abre solo la imagen solicitada en Dispatchers.IO, verifica su hash cuando está declarado y decodifica con muestreo/caché acotada. El alias y la ruta modular usan la misma clave canónica de caché. verifiesAsset queda disponible para pruebas explícitas de archivos concretos, sin barrido automático de arranque.

La UI recibe referencias opacas; no construye carpetas por actividad. Cuando falta un recurso, falla un hash o la imagen no es legible, mantiene el espacio, un aviso y reintento sin bloquear pasos. La reorganización no escribe en Room, progreso, inventario o evidencias.

## Transiciones de Taller

WorkshopMotion y GuidedStepMotion usan las APIs nativas existentes; no se añade dependencia Gradle. Entrada al módulo250ms; rutas y ficha220ms; procedimiento240ms con dirección reversible; progreso/check180ms; perspectivas160ms. Solo foto,título e instrucción del paso comparten AnimatedContent. Encabezado, controles, notas y operaciones de negocio quedan fuera del contenido superpuesto.

Las entradas de ruta componen una sola pantalla y un solo borrador. Un guard y una barrera de puntero impiden navegación repetida durante la transición; volver conserva la cola de guardado. El reloj del paso se mantiene fuera de la lista perezosa para liberar el guard incluso con la foto fuera del viewport. La reanudación no vuelve a escribir una visita ya guardada.

Un único observador lee escalas Android y TalkBack, propagadas por CompositionLocal. El movimiento se anula con escalas deshabilitadas o exploración táctil; no hay bucles de animación permanentes. Los efectos no completan pasos, consumen materiales ni generan observaciones. Las cinco pestañas, propósito secundario, estado guardado y filtros permanecen en sus componentes actuales.

## Verificación y límites

La entrega incluye comprobaciones locales de integridad, pruebas Python y una repetición del generador real que produjo un árbol idéntico. Las pruebas Android nuevas ejercitan alias y apertura real, rechazo de rutas, navegación/persistencia y reglas de movimiento. Las pruebas antiguas afectadas se adaptan a assetPath conservando sus casos.

Compilación, lint Android, API26/API36, capturas reales, comparación visual, fluidez/memoria en dispositivo y actualización de APK instalada permanecen NO_EJECUTADO cuando se entrega sin herramientas Android locales. No se declaran aprobadas por la revisión léxica. El usuario debe ejecutar el workflow manual existente con run_emulator=true y contrastar las siete capturas aprobadas.
