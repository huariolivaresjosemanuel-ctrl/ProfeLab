# CONTRATO OBLIGATORIO — Modularidad, catálogo ampliable y permisos Android

**Proyecto:** ProfeLab V1.0. **Prioridad:** P0 para permisos, persistencia, aislamiento de módulos e importación segura; P1 para expansiones y mejoras. **Estado:** especificación; NO afirma que el código esté implementado ni libre de errores.

## A. Separar por funcionalidades, no construir un archivo gigante

La app es **un solo APK Android**, no varias apps. El código debe estar organizado por funcionalidad (*feature-first*), con responsabilidades explícitas y contratos reutilizables. **Inicio, Taller, Sesiones, Cuaderno, Biblioteca, OCR, Perfil/Tareas y Respaldo** son áreas independientes. El OCR es una herramienta transversal reutilizable, no una copia de cámara diferente por pantalla.

Propuesta de organización (adaptable por Codex si documenta cómo mantiene los límites):

```text
:app                          ensamblaje Android, navegación raíz, inyección y manifest
:core:model                   modelos compartidos, referencias tipadas, Result/Error
:core:designsystem            colores, tipografía, componentes, 5 tabs de ProfeLab
:core:database                Room, DAOs y migraciones con pruebas
:core:files                   import/export SAF, contenido privado, hash y cuotas
:core:permissions             permisos runtime, estados, alternativas, ajustes
:core:content                 manifiestos, catálogos y packs importables validados
:feature:home                 Inicio, progreso, atajos y actividad
:feature:workshop             Taller, proyectos, inventario, checklist
:feature:sessions             Sesiones, edición, currículo enlazado
:feature:notebook             Cuaderno, prácticas, notas, portafolio y audio
:feature:library              Biblioteca MINEDU, PDFs, lector, marcadores y notas
:feature:ocr                  Cámara, linterna, imagen/PDF, OCR, revisión, entrega
:feature:tasks                Pendientes y recordatorios locales
:feature:settings             Perfil, permisos visibles y preferencias
:feature:backup               Respaldo, validación y restauración
```

Si la cantidad de módulos Gradle penaliza CI o tiempo de desarrollo, se admite una variante *package-by-feature* con los mismos límites y tests; los límites de dependencia son obligatorios, el nombre de cada módulo Gradle no. No añadir un plugin system de código ejecutable ni carga dinámica de DEX.

### Contratos y dependencias

1. `:app` ensambla y navega, pero no contiene SQL, OCR, parser PDF, lógica curricular ni UI de todas las pantallas.
2. Cada `feature` contiene UI, ViewModel, estado de pantalla, reglas y pruebas propias; evitar `MainActivity` gigante y utilidades globales mutables.
3. La lógica compartida vive en `:core`; **una feature no importa ni modifica la UI o estado interno de otra feature**. Los vínculos se expresan con `EntityRef(type,id)`, servicios/repositorios y argumentos de navegación tipados, con versiones si los contratos cambian.
4. OCR entrega `ScanResult` tras revisión y destino `{NOTE, SESSION, PROJECT, LIBRARY}`; cada destinatario decide si adjunta/guarda su contenido. Cancelar devuelve al origen sin sobrescribir.
5. Un PDF no se copia en cada módulo: Biblioteca gestiona documento/URI local y los demás guardan referencia estable. Las notas y sesiones sobreviven al borrado de un documento vinculado.
6. Los repositorios exponen operaciones y errores explícitos (`Success`, `PermissionDenied`, `MissingDocument`, `InvalidContent`, `UnavailableHardware`, `StorageFull`); no capturar excepciones silenciosamente.
7. Navegación entre 5 tabs conserva estado/borrador y no reinicia un proceso OCR por recomposición o rotación.
8. Cambios en un módulo exigen tests del módulo, contratos compartidos y una suite de regresión de los demás. **Modularidad reduce riesgo, pero no garantiza cero bugs**.

## B. Ampliar contenido sin rehacer o borrar la app

Soportar **dos vías complementarias**, ambas voluntarias y offline:

- **Importación individual:** PDFs de MINEDU o del usuario mediante selector Android; proyectos/materiales propios, fotos, fichas y notas. Guardar original, metadatos, origen y vínculo; editable sin reimportar.
- **Paquetes de contenido versionados:** ZIP/JSON de catálogo que añaden *plantillas originales*, ideas, áreas/categorías, instrucciones, imágenes autorizadas y PDFs solo cuando sus derechos de redistribución estén comprobados. Instalar desde selector de archivos; no requieren actualizar la APK. También permitir futuras actualizaciones de la propia app sin perder bases existentes.

Contrato inicial `profelab-content-pack`:

```json
{
  "schemaVersion": 1,
  "packId": "org.example.plantillas-matematica",
  "packVersion": "1.0.0",
  "minAppVersion": "1.0.0",
  "title": "Plantillas de matemáticas (ejemplo)",
  "source": "CREACION_ORIGINAL",
  "items": [
    {"id":"sumas-tapas-01", "type":"MATERIAL_PROJECT", "path":"items/sumas-tapas-01.json", "sha256":"<SHA256_REAL_64_HEX>"}
  ]
}
```

Es **ejemplo de esquema, no un paquete de contenido instalado**. Definir `ContentPack`/`ContentItem` con id estable, versión, fecha, procedencia, autor, derechos verificados, etiquetas, área, grado, manifest y hash de cada adjunto. No inferir que una guía es oficial por tener MINEDU escrito en el título. El usuario podrá ver una lista de paquetes importados, versión, origen y capacidad de actualizar/quitar contenido de catálogo sin eliminar notas o creaciones que lo referencian.

Validación de importación obligatoria: versión de esquema/aplicación, JSON válido, tamaños máximos, tipo MIME, archivos cifrados/corruptos, path traversal (`../`), bombas ZIP, hashes, referencias duplicadas, derechos/metadatos mínimos. Vista previa de altas/cambios/conflictos; confirmar antes de escribir; importación transaccional, rollback ante error y registro de procedencia. En actualización **no sobrescribir contenido que el usuario editó**: separar plantilla base de copia del usuario o solicitar decisión. Reimportar el mismo pack no duplica registros. Evitar migraciones destructivas.

**PDFs MINEDU:** por defecto se importan desde archivos elegidos por el usuario; incluirlos en el APK o distribuirlos en packs solo si la fuente oficial y los derechos concretos permiten ese uso. Mostrar metadatos verificables, no rótulos de licencia ficticios del mockup. Un PDF importado se podrá usar offline, con buscador por título y posición de lectura. La búsqueda dentro del PDF requiere texto extraíble u OCR aprobado.

## C. Permisos y capacidades Android por acción

| Acción | Implementación | Momento y alternativa |
|---|---|---|
| Abrir cámara de escaneo/OCR | `android.permission.CAMERA` (runtime) con CameraX | Solicitar al pulsar **Escanear**; si se deniega, ofrecer **Elegir imagen/PDF**. Nunca al iniciar la app. |
| Encender linterna dentro de cámara | Comprobar `CameraInfo.hasFlashUnit()` y llamar `CameraControl.enableTorch(true/false)` | Reutiliza la sesión de cámara; **NO solicitar un permiso “flash” aparte**. Ocultar/desactivar botón si no hay unidad; restaurar OFF al salir o perder cámara. |
| Grabar nota de voz | `android.permission.RECORD_AUDIO` (runtime) | Pedir al pulsar **Grabar voz**; denegación no bloquea notas manuales. Detener/liberar al salir, gestionar cancelación. |
| Recordatorios/notificaciones | `POST_NOTIFICATIONS` en Android 13+ cuando aplique | Pedir solo tras activar avisos; si se deniega, conservar pendientes sin notificaciones. |
| Importar PDF, archivo JSON/ZIP, exportar respaldo | Storage Access Framework (`OpenDocument`, `CreateDocument`) | Mostrar selector del sistema, sin `MANAGE_EXTERNAL_STORAGE` ni acceso general a carpetas; copiar a almacenamiento privado o conservar URI con permiso persistente cuando corresponda. |
| Elegir fotografía existente | Android Photo Picker (preferido) o selector de documento | No pedir permiso de toda la galería; si el usuario niega/cancela, mantener pantalla intacta. |
| Stylus/tableta | API de entrada estándar | No pedir permiso extra para lápiz; permitir dedo y teclado. |

Implementar un **único coordinador de permisos** usando APIs modernas Activity Result, evitando solicitudes duplicadas o simultáneas, especialmente tras rotar el dispositivo. Estados de UI: no solicitado, explicando finalidad, concedido, denegado, denegado permanentemente/configuración, hardware no disponible, revocado mientras se usa y cancelado.

- Si se deniega y Android permite volver a solicitarlo, explicar brevemente y dejar reintentar cuando el usuario pulse la función. Si ya no puede solicitarse desde la app, ofrecer **Abrir ajustes** con retorno controlado.
- No bloquear Inicio, Sesiones, Taller ni Biblioteca porque no haya cámara, micrófono o flash.
- Revocar cámara en ajustes durante OCR no debe causar pantalla negra persistente ni bucle de solicitudes. Liberar CameraX y permitir volver a abrir o importar imagen.
- Torch: botón ON/OFF visible solo si flash disponible, estado real observado, apagado obligatorio en `onStop`/liberación y antes de salir; prevenir `enableTorch()` tras cerrar la cámara.
- No solicitar acceso a internet ni envío de datos para funciones offline. Enlaces MINEDU externos, si existen, requieren acción expresa y conectividad voluntaria.
- Evaluar compatibilidad Android `minSdk`, `targetSdk`, modelos OCR y `compileSdk` contra los dispositivos objetivo; prueba física en móvil y tableta Lenovo Tab Plus. No inventar versiones compatibles ni marcar PASS sin prueba.

Fuentes Android oficiales:
- https://developer.android.com/training/permissions/requesting
- https://developer.android.com/training/permissions/usage-notes
- https://developer.android.com/media/camera/camerax/configuration
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/develop/ui/compose/notifications/notification-permission

## D. Aislamiento y actualizaciones seguras

- Contratos de almacenamiento versionados. Migraciones Room `vN -> vN+1` con pruebas partiendo de bases antiguas; **PROHIBIDO** `fallbackToDestructiveMigration()` en producción o limpiar datos para resolver errores.
- Separar contenido editorial de contenido de usuario; la actualización de guías nunca reescribe notas, sesiones, checklist o inventario.
- No usar `SharedPreferences` como repositorio de toda la app ni pasar objetos binarios grandes entre pantallas. Recursos grandes en archivos privados con cuotas, copia atómica y cleanup.
- En cada entrega H1-H7: inventario de archivos; cambios por feature; APIs modificadas; QA automático/visual/dispositivo; regresiones en 5 tabs; compatibilidad con esquema anterior; y flujo de instalación limpia y actualización sobre instalación existente.
- Todo módulo debe admitir estado sin datos, permiso denegado, error recuperable, datos corruptos y retorno desde otra pantalla sin perder borrador.
- Futuros módulos se añaden con navegación + DI + contrato de datos + tests; nunca duplicar OCR/lector PDF para cada módulo.

## E. Pruebas de aceptación adicionales

Bloquean entrega si fallan: aislamiento feature-to-feature, migración sin pérdida, reimportación idempotente de paquetes, denegación/revocación de CAMERA, no-flash, Torch OFF al cerrar, primer OCR offline, importar PDF sin permisos excesivos y recuperación tras cancelación. Se incluyen como casos `QA-088` en adelante en `04_QA/MATRIZ_QA_PROFELAB_V1.csv`; ninguno es PASS hasta ejecutarlo.
