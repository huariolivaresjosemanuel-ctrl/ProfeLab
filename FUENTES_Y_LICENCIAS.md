# Fuentes, procedencia y licencias

Base ProfeLab: `main` en commit `4ec7a104e9345fd1aa1a0c74eddbe72169304b80`; copia solo de lectura. Fuentes y recursos propios desarrollados para el usuario. Esta entrega no asigna licencia pública nueva al proyecto ni registra marcas.

## Contenido aportado

`reference/` conserva originales del paquete V2: SDD/anexos,46 vistas,191QA, cinco PNG, catálogo de12 referencias,12 proyectos/miniaturas,8 experimentos y fixturesQA. Los seis ZIP temáticos eran copias y no se duplican como proyectos. `reference/encargo_completo/` conserva la instrucción vigente y la plantilla de lectura.

Los JSON enriquecidos y las12 miniaturas se empaquetan en `core/content/src/main/assets/workshop/`; los originales se conservan separados. Los tres PDF y la imagen de prueba se usan en `androidTest`, no se anuncian como documentos oficiales. Los mockups no forman parte de las pantallas ni sourceSets del APK.

MINEDU: solo metadatos y enlaces del catálogo suministrado; distribución de PDFs no aprobada, sin binarios oficiales. No se afirma que abrir un enlace requiera menos derechos que redistribuir su PDF. Competencias/páginas introducidas por el usuario requieren cotejo propio. ProfeLab no es una app oficial MINEDU.

## DocReader MIT adaptado

Origen: [codegeasse1/DocReader](https://github.com/codegeasse1/DocReader), commit fijo `092d7fd48eae18d2409d8c97149620218db99657`. Descarga única saneada según expediente aportado; SHA256 del ZIP saneado de consulta: `baeb61462e58816fd430eaa1f6d37e6a77c927536ce3eab64ffbf550af02436a`.

- `feature/pdf/.../PdfDocumentHandle.kt`: adaptado del wrapper original de PdfRenderer/PDFBox; límites de render y liberación de recursos ajustados.
- `feature/pdf/.../PdfEngine.kt`, función `point`: adaptación de la transformación `PdfOps.toPdf` para coordenadas de página.
- El resto de la integración Room, selección de posiciones, UI, copia/exportación y navegación corresponde a esta implementación; no se declara DocReader completo incorporado.
- Copyright (c)2026 codegeasse1 y texto MIT íntegro en `licenses/DocReader-MIT.txt`, también en assets del APK y visible en Acerca de.
- No se incluyen claves, workflows ni proyecto externo completo.

## Dependencias

| Biblioteca | Versión | Licencia / términos publicados |
|---|---|---|
| Gradle Wrapper |8.13|Apache2.0; licencia íntegra y hashes/origen ya conservados |
| Kotlin/KSP/coroutines |2.2.21 /2.2.21-2.0.5 /1.10.2|Apache2.0 |
| AndroidX/Compose/Room/Navigation/Lifecycle/DataStore/WorkManager |`gradle/libs.versions.toml`|Apache2.0; avisos de sus artefactos |
| CameraX camera2/lifecycle/view |1.5.1|Apache2.0 |
| PDFBox-Android |2.0.27.0|Apache2.0; [repositorio y licencia](https://github.com/TomRoush/PdfBox-Android) |
| ML Kit Text Recognition Latin bundled |16.0.1|[Condiciones ML Kit](https://developers.google.com/ml-kit/terms); no atribuir licencia Apache al modelo propietario |
| Material Icons |1.7.8|Apache2.0 |
| JUnit4 |4.13.2|EPL1.0, solo pruebas |

El inventario de dependencias transitivas realmente resueltas y avisos de distribución del APK debe cerrarse con la compilación Gradle. No se vende esta tabla como auditoría legal exhaustiva. [ML Kit Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) distingue bundled `com.google.mlkit:text-recognition` del artefacto dependiente de descarga. [Fuente de PDFBox LosslessFactory](https://github.com/TomRoush/PdfBox-Android/blob/master/library/src/main/java/com/tom_roush/pdfbox/pdmodel/graphics/image/LosslessFactory.java) documenta la entrada Bitmap usada por el anexo imprimible.

ScanPro: no tiene permiso escrito acreditado en el expediente, no se copió/adaptó/distribuyó código, recursos ni claves. Cámara/OCR se implementaron de forma propia con APIs documentadas. El expediente del tercero no concede licencia por sí solo.
