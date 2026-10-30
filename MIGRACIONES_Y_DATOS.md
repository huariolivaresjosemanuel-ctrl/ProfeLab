# Migraciones, compatibilidad y datos personales

No cambia el esquema SQLite: Room continúa en **2**. Las entidades, los JSON exportados de esquemas 1 y 2 y el registro de la migración 1→2 permanecen byte a byte iguales al commit base. No se agregó migración de número ni fallback destructivo. El control Python del SQL heredado pasó; la ejecución Room en Android y la actualización desde un teléfono V1.0 NO se han ejecutado aquí.

WorkspaceStore incorpora `editRecords`, implementado por WorkspaceRepository con `withTransaction`: lee el estado actual y aplica únicamente las filas devueltas, con ID único, límite de JSON e incremento de revisión. Inventario + comprobante + lista se confirman juntos; ante una excepción Room revierte la transacción. El test instrumentado de rollback está escrito, NOT_RUN.

## Adaptación aditiva del contenido

WorkshopCodec acepta arrays de materiales existentes, objetos tipados o cadenas heredadas de barras. Leer no modifica la base. Al editar/copiar/exportar se normaliza un payload de versión 2, preservando campos desconocidos y `legacyMaterialsOriginal`. El catálogo APK original no se reescribe. Filas mal formadas se presentan como revisión necesaria y no cubren requisitos mediante cantidades inventadas. Corregir una fila requiere acción del usuario.

Los IDs heredados de material se derivan de nombre canónico con UUID.nameUUIDFromBytes UTF-8; un catálogo puede resolverlos por nombre/sinónimo inequívoco. No hay emparejamiento difuso automático. Se retienen registros antiguos de inventario. Las listas de compra de texto previas se muestran conservadas y no se transforman en stock sin confirmación.

## Separación

| Datos | Ubicación lógica | Política |
|---|---|---|
| Originales | assets/workshop, project_template/experiment_template | Inmutables, doce proyectos y ocho experimentos intactos |
| Paquetes instalados | catalog_project/experiment/material/layout + content_pack | Actualización por packId/versión; sin tocar UUID personales |
| Existencias | inventory, user_material | Registro real del usuario, reservas y movimientos |
| Creaciones propias | project / experiment | UUID independiente, vínculo al origen, copia de layout y contenido |
| Avances y registros | JSON de cada creación | Checklist/observaciones/mediciones y enlaces de archivos |
| Compras | shopping_v2 | Selecciones, exclusiones, adquirido, recibos; antigua shopping preservada |
| Imágenes | documents/<id privado> | Recursos copiados y evidencias; no referencias permanentes al URI del proveedor |

Una actualización de paquete conserva copias personales y sus imágenes, aunque el catálogo cambie. Los archivos de versiones anteriores no se borran automáticamente. Archivar una creación o material es recuperable desde Perfil. Quitar evidencia elimina vínculos del proyecto y sus observaciones, conservando el archivo privado para otros usos.

La cola del ViewModel serializa guardados, mantiene el último borrador en memoria, muestra error/reintento y espera antes de la navegación interna, exportación o cámara. Campos de formularios y filtros usan rememberSaveable. Esto no sustituye pruebas de muerte del proceso o falta de espacio: un cambio aún no confirmado puede perderse si el sistema interrumpe la app. Compruebe el indicador de guardado.

El respaldo existente se reutiliza sin alterar su formato ni motor. Debe probarse en dispositivo con nuevos tipos de registros, imágenes de paquetes y evidencias. Haga respaldo antes de actualizar; no desinstale ni borre almacenamiento. La continuidad de datos exige applicationId y certificado iguales, además de versionCode creciente. Esta entrega no contiene ninguna clave.
