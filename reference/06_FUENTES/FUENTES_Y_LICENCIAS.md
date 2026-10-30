# Fuentes, derechos y procedencia — ProfeLab

**Fecha de comprobación:** 29 de octubre de 2026. Revalidar antes de codificar/distribuir. Estas URLs se incluyen como referencia y NO constituyen autorización automática de copia masiva o distribución dentro del APK.

## Minedu y currículo peruano

- [Programa curricular de Educación Primaria (2016), repositorio MINEDU](https://repositorio.minedu.gob.pe/handle/20.500.12799/4549). Incluye competencias, capacidades, enfoques, estándares y orientaciones; comprobar versión vigente y actualizaciones.
- [Recursos del Currículo Nacional, MINEDU](https://www.minedu.gob.pe/curriculo/recursos.php). Ejemplos de sesiones, unidades y planificación presentados como referencias flexibles, NO formatos obligatorios.
- [Cartilla de planificación curricular (2017)](https://repositorio.minedu.gob.pe/handle/20.500.12799/5310). Orienta planificación y evaluación formativa.

**No interpretar `openAccess` como equivalente a licencia CC BY, permiso de redistribución comercial ni dominio público.** Fichas de biblioteca deben tener licencia real verificada y URL de fuente. Las referencias de UI con `CC BY 4.0`, número de páginas y fechas son maquetas; no precargar esos rótulos como hechos.

## Android y OCR

- [ML Kit modelos bundled/unbundled](https://developers.google.com/ml-kit/tips/installation-paths): Text Recognition v2 soporta bundled y unbundled; el modelo bundled es requisito si se promete OCR offline desde la primera ejecución.
- [ML Kit Text Recognition v2 Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android): API, instalación y calidad de imagen.
- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer): `createOnDeviceSpeechRecognizer` existe desde Android 12 (API31) y depende de disponibilidad; la grabadora local NO garantiza transcripción offline.
- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer): renderización local de páginas de PDF. Necesita otra estrategia para extracción/búsqueda de texto.
- [ScanPro — repositorio público](https://github.com/luisferxd09/ScanPro): inspiración para cámara, recorte, OCR y documentos. Repositorio observado sin LICENSE en la raíz y código comprimido; obtener permiso escrito del autor antes de copiar código y auditar licencias de terceros.

## Permiso a solicitar al autor de ScanPro (plantilla orientativa)

> Estoy desarrollando ProfeLab, una aplicación Android educativa para estudiantes de primaria. ¿Me autorizas por escrito a estudiar, adaptar e integrar fragmentos específicos de ScanPro (indicar rutas y archivos), distribuirlos en mi APK y eventualmente comercializar la app? Indícame bajo qué licencia, atribución y condiciones podría hacerlo, y si todos los componentes y dependencias que utilizas permiten ese uso.

Aun con permiso del autor, auditar licencias de terceros como Tesseract.js, PDF libs, imágenes y modelos. El reconocimiento de texto no autentica documentos y no garantiza ausencia de errores.

## Política sobre muestras

Las cinco capturas en `01_REFERENCIAS_UI/` fueron proporcionadas por el usuario como especificación visual. NO usarlas como imágenes de fondo funcionales ni incorporarlas al APK como pantallas rasterizadas. Usar iconografía y fotografías propias o con licencia compatible. Las fixtures son ficticias.

## Fuentes oficiales Android para permisos, cámara y selector de archivos

- https://developer.android.com/training/permissions/requesting
- https://developer.android.com/training/permissions/usage-notes
- https://developer.android.com/media/camera/camerax/configuration
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/develop/ui/compose/notifications/notification-permission

Flash dentro de CameraX utiliza `hasFlashUnit()` y `enableTorch()`, no otro permiso de tiempo de ejecución llamado FLASH. Los permisos de cámara y micrófono son contextuales, mientras que SAF/Photo Picker evita pedir lectura global de archivos cuando son suficientes.


## Catálogo adicional listo para Codex (2026-10-29)

Se agregan metadatos preinvestigados de seis documentos oficiales (tres guías 2025 y Orientaciones 2026 incluidas) con enlaces en `CATALOGO_MINEDU_VERIFICADO.json`, un downloader/validador y una guía de derechos `MINEDU_BIBLIOTECA_E_INTEGRACION.md`. Ver también `ANALISIS_SCANPRO_OCR_Y_REUTILIZACION.md` para los módulos fuente y las cautelas de integración. **No se lograron descargar binarios PDF en este entorno**: el repositorio devolvió verificación anti-bot. La ficha y el pack de contenido no sustituyen un PDF original.
