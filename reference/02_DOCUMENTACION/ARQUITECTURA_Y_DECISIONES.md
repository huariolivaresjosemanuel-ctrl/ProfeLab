# Arquitectura y decisiones — ProfeLab V1.0

**Anexo de cumplimiento obligatorio:** `CONTRATOS_MODULOS_PERMISOS_Y_CONTENIDO.md`. Arquitectura *feature-first* con contratos tipados, módulos desacoplados y packs de contenido versionados; evitar archivos gigantes, lógica compartida duplicada y migraciones destructivas.

## Decisión inicial recomendada

1. **Android Kotlin + Jetpack Compose + Material 3** ajustado a referencias ProfeLab. Gestión de estado con ViewModel + Flow/StateFlow, repositorios y UI state explícito. No usar WebView como única salida solo porque el proyecto de seguridad lo empleó; la app educativa es nueva.
2. **Room SQLite** para datos estructurados, migraciones, índices y relaciones; **DataStore** para preferencias. **Archivos privados internos** para fotografías, audio, OCR y copias de PDFs importados, con metadatos/hash en Room.
3. **CameraX** con permiso cuando se necesite; selector SAF para galería/PDF; **ML Kit Text Recognition v2 bundled (Latin script)** para texto impreso español. *Modelo empaquetado dentro del APK*, sin necesidad de Google Play Services para descargar en primera ejecución; confirmar implementación exacta y probar instalación limpia en modo avión.
4. **PDF local**: `PdfRenderer` para páginas e índice de lectura; búsquedas en texto cuando realmente se pueda extraer, mediante biblioteca de PDF compatible revisada o texto OCR ya confirmado. No asumir que PdfRenderer extrae texto por sí solo.
5. **Exportación PDF** con `PdfDocument` u otra implementación revisada; Word DOCX es extensión opcional si se implementa con licencia compatible y QA. Compartir con Android Sharesheet/SAF y `FileProvider`; nunca acceso general indiscriminado a almacenamiento.
6. **Notificaciones** con WorkManager para tareas programables, respetando limitaciones de ejecución y permisos Android; sin prometer exactitud al minuto si no hay permiso/requisitos necesarios.
7. **Audio**: MediaRecorder/Media3 u opción nativa adecuada; `SpeechRecognizer` general puede usar servicios remotos. En Android 12+ se puede consultar on-device recognizer, pero su disponibilidad/modelos depende del equipo. La función de voz a texto no se prometerá 100% offline salvo motor empaquetado probado en el dispositivo objetivo; V1 soporta grabación local y edición manual.
8. **OCR ScanPro**: copia no autorizada de código bloqueada por falta de licencia pública visible; puede estudiarse flujo UX y código solo tras permiso expreso del autor y revisión de dependencias. No montar su ZIP dentro de nuestro repositorio sin aprobación.
9. **Red**: ninguna petición necesaria en uso normal. Si la biblioteca permite abrir URLs oficiales, debe ser interacción explícita y mostrar que requiere conexión. Sin cuentas, tracking o SDKs que realicen envíos ocultos.

## Alternativas evaluadas

| Alternativa | Pros | Contras | Decisión |
|---|---|---|---|
| Kotlin + Compose | Diseño adaptativo, estado y accesibilidad Android, integración cámara/archivos nativos | Curva de desarrollo, compilación Android | **Preferida** |
| Java + WebView | Experiencia previa, HTML/CSS flexible | Puentes nativos complejos, IndexedDB/PDF/cámara y estado de sistema requieren más QA | Alternativa solo si Codex demuestra ventajas |
| ML Kit bundled | OCR local listo desde primera instalación, buena API Android | APK algo mayor, calidad dependiente de imagen, no reemplaza OCR manuscrito | **Base OCR** |
| Tesseract spa empaquetado | Control modelos/idiomas | Tamaño, rendimiento y mantenimiento | Fallback si benchmark lo justifica |
| Google Document Scanner | Recorte automático nativo conveniente | Ruta de instalación depende de Play services/modelos, no es bundled según matriz oficial | No usar como requisito del primer uso offline |
| ScanPro reutilizado | Encuadre, mejora, pipeline OCR | Licencia no identificada, ZIP sin auditoría de código en este paquete | Solo inspiración, integración condicionada |

## Requisitos de calidad

- API mínima a decidir con pruebas en teléfono/tableta; propuesto `minSdk=26` pendiente de confirmar con dispositivos objetivo y bibliotecas.
- Rendimiento: lista de 500 notas fluida en dispositivo de gama media; OCR no bloquea hilo UI; cancelar PDF multipágina, evitar OOM con downsampling y paginación.
- Accesibilidad: TalkBack, tamaño de fuente, contraste, al menos 48dp de tacto, navegación con teclado físico/tableta.
- Seguridad: sin contraseñas incrustadas; borrado confirmado; datos de menores/terceros solo con finalidad clara y consentimiento pertinente; metadatos EXIF de fotos compartidas minimizados cuando proceda.
- QA real: modo avión desde primera apertura, cámara física, lectura PDF grande, respaldos, giro, cierre forzado, revocación de permisos, dispositivos Android distintos.
- Flujo de compilación: código fuente entregado ZIP con inventario/rutas, compilación reproducible por GitHub Actions. No cambiar el repositorio Control Personal DNI.

## Referencias oficiales (verificar versiones al implementar)

- https://developers.google.com/ml-kit/tips/installation-paths
- https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/graphics/pdf/PdfRenderer
- https://www.minedu.gob.pe/curriculo/recursos.php
- https://repositorio.minedu.gob.pe/handle/20.500.12799/4549

## Aislamiento de features, mantenimiento y ampliaciones

La app será un único APK Android, con `:app`, `:core:*` y `:feature:*` o la alternativa de paquetes independientes documentada en el anexo. Cada feature contiene UI/estado/reglas/tests; dependencias transversales (OCR/PDF/archivos/permisos/datos) se declaran por interfaces compartidas. Los catálogos de guías/materiales se podrán actualizar mediante importación de usuario y contenido empaquetado seguro, no mediante código ejecutable descargado. GitHub seguirá en SOLO LECTURA y las entregas se hacen por ZIP para Actions.

**Permisos Android:** usar el contrato del anexo. Solicitar `CAMERA` en uso; el botón de luz usa `CameraControl.enableTorch()` previa detección `hasFlashUnit()`, sin supuesto permiso FLASH; `RECORD_AUDIO` cuando se grabe y `POST_NOTIFICATIONS` cuando el usuario habilite alertas en Android 13+. SAF/Photo Picker para documentos/fotos sin pedir acceso global. Declarar hardware de cámara opcional para que la app pueda instalarse y usarse sin cámara, si el device target lo requiere.

**Estabilidad:** matriz de dependencias, migraciones no destructivas, tests de API y regresión entre features, pruebas de instalación limpia y actualización. No prometer cero bugs: exigir evidencia y gestión explícita de errores.
