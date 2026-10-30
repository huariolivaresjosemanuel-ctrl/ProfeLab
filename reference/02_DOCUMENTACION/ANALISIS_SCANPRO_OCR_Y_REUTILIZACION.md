# Auditoría de referencia técnica — ScanPro de luisferxd09

**Repositorio**: https://github.com/luisferxd09/ScanPro  
**Fecha revisada**: 29/10/2026  
**Licencia detectada**: SIN archivo `LICENSE` público ni licencia declarada en metadatos GitHub. Solicitar autorización escrita explícita para copiar/adaptar/distribuir código (incluso si el titular es amigo del usuario); auditar licencias de dependencias y modelos.

## Qué está realmente confirmado

- `README.md` declara versión 1.3.1, Android 7+, funcionamiento offline, corrección de perspectiva/recorte, detección de bordes, procesamiento por `Web Worker + OffscreenCanvas`, OCR Tesseract.js 5 español/inglés, exportación PDF/DOCX/JPG/PNG/WEBP/ZIP, guardado local IndexedDB y Java + WebView `WebViewAssetLoader`.
- Estructura de `scanprov2-android.zip` inspeccionada **sin copiar código**: contiene `app/src/main/java/com/scanpro/app/MainActivity.java`, `app/src/main/AndroidManifest.xml`, `app/build.gradle`, `web/scanpro-web.zip`, `scripts/fetch-ocr.sh`, `app/src/main/res/...`, scripts Gradle. El código del motor web está dentro de un ZIP anidado: **no se verificó a nivel de funciones** y NO se afirma que pueda extraerse o trasladarse sin cambios.
- `.github/workflows/build.yml` hace `unzip *.zip -d proyecto`, ejecuta `bash scripts/fetch-ocr.sh` para añadir OCR **en la compilación**, y `./gradlew assembleRelease`. Esto **no demuestra** que un APK descargado tenga el motor OCR listo para primera ejecución: debe probarse físicamente en modo avión.
- **Advertencia de seguridad al autor**: el ZIP del repositorio contiene `app/scanpro.jks` (keystore). Nunca copiar ni incluir esta clave en ProfeLab; si fuese la clave de firma de una APK publicada, el titular debe comprobar exposición y adoptar medidas de seguridad apropiadas.

## Diseño de integración propuesto para ProfeLab

`features/ocr/api/OcrEngine` -> contrato `recognize(imageBytes|uri, options): OcrDraft` con `engine`, `language`, `blocks`, `textOriginal`, `confidence?`, `orientation`, `sourceImageUri`, `errors`.

`features/scan/api/DocumentCapture` -> contratos `capture`, `pick`, `crop`, `rotate`, `torch`, `cancel`, usando CameraX/SAF, sin dependencia directa de las pantallas.

`features/ocr/ui/ReviewOcrScreen` -> original a la izquierda/arriba y texto a la derecha/abajo, edición manual obligatoria, botón `Guardar texto corregido`, destinos `Cuaderno`, `Sesiones`, `Taller`, `Biblioteca`; cancelar vuelve al origen sin perder borrador.

### Elección inicial recomendada

- Engine principal: **ML Kit Text Recognition v2 Latin bundled** (documentación oficial: `com.google.mlkit:text-recognition:16.0.1`). Es independiente de Google Play Services para descargar el modelo durante uso. Reconocimiento principalmente de texto impreso; nunca prometer manuscrito perfecto.
- Pipeline de captura propio de ProfeLab con CameraX y recorte/rotación básico; detección avanzada de bordes/perspectiva **opcional** para V1.x, a evaluar con permiso y código auditado de ScanPro.
- Alternativa opcional: Tesseract OCR con `spa` y `eng` modelos empaquetados, previo análisis de APK, memoria, velocidad y licencias del modelo. No crear doble motor obligatorio si complica QA.
- Captura de PDF escaneado: `PdfRenderer` por página, OCR asincrónico y cancelable, tamaño máximo configurable para evitar OOM.
- Android: solicitud de cámara solo al usar; `hasFlashUnit()` y `enableTorch()` para linterna sin permiso adicional; revocación y ausencia de flash con UX clara. SAF evita READ_MEDIA global para selección de PDF.

### Lo que NO debemos asumir

1. Que ScanPro ya ofrece un parser de DNIe peruano o corrige el error QR de Control Personal DNI.
2. Que sea automáticamente seguro o compatible con los SDK/depencias de ProfeLab.
3. Que el contenido de `web/scanpro-web.zip` se haya auditado (pendiente).
4. Que tener GitHub público, amistad o un release APK equivalga a licencia de reutilización.
5. Que firmar ProfeLab con claves de ScanPro sea necesario o seguro.

### Tareas de Codex antes de reutilizar componentes

- Verificar permiso escrito del titular, alcance comercial y atribución.
- Extraer y estudiar en una copia temporal `scanprov2-android.zip` y `web/scanpro-web.zip`, **sin incorporar originales automáticamente al repo ProfeLab**.
- Elaborar inventario de librerías/archivos y posibles secretos, verificar licencias, comparar con ML Kit bundled.
- Si procede, aislar el procesamiento en módulo reemplazable `DocumentCapture` y mantener `OcrEngine` independiente; benchmark en modo avión y con cámara real.
- Documentar pruebas de imagen torcida, borrosa, baja luz, varias páginas, denominaciones de PDF y errores; registrar PASS/FAIL con evidencia.
