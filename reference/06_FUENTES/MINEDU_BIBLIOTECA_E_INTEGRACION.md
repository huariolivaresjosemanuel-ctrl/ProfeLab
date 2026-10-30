# Inventario de documentos oficiales MINEDU: integración para ProfeLab

**Investigado el 29/10/2026.** Catálogo validado contra fichas del repositorio institucional y documentación oficial accesible. **El ZIP NO contiene los binarios de los PDFs originales**: el servidor del repositorio presentó una verificación anti-bot y el entorno de descarga no pudo recuperar los bytes. Se incluyen **títulos, autores, años, páginas oficiales, enlaces directos verificados cuando están disponibles, política de uso, script de descarga y catalogación para Codex**. No declarar `PDF descargado` hasta verificar cabecera `%PDF-` y hash real.

La descarga a un equipo de desarrollo para revisión no equivale a licencia de redistribuir dentro de APK. El `openAccess` de una ficha significa acceso, no permiso comercial. **Conflicto detectado:** metadatos de las Guías 1 y 2 indican CC BY 4.0, mientras los PDFs indexados muestran un aviso editorial de "Todos los derechos reservados"; hasta aclarar alcance, mantener `NO_BUNDLE` en APK. El programa curricular y el CNEB consultados incluyen aviso restrictivo de reproducción. Codex debe informar al usuario si falta autorización, sin copiar PDFs a `assets/` por defecto.

| ID | Año | Uso propuesto | Página oficial | Licencia / acción |
|---|---:|---|---|---|
| MINEDU-CNEB-2017 | 2017 | Marco nacional: perfil de egreso, competencias, estándares, enfoques y evaluación. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/4551) | Acceso abierto; sin licencia CC específica en ficha histórica; **NO_BUNDLE_REFERENCE_ONLY** |
| MINEDU-PRIMARIA-2017 | 2017 | Competencias, capacidades, desempeños, estándares, ciclo, enfoques y planificación. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/4549) | Acceso abierto; sin licencia CC específica en ficha histórica; **NO_BUNDLE_REFERENCE_ONLY** |
| MINEDU-ALFA-G1-2025 | 2025 | Condiciones para la alfabetización inicial: ambiente alfabetizador, biblioteca de aula y organización. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/11807) | Repositorio: CC BY 4.0 (dc.rights.uri); **DOWNLOAD_TO_REVIEW_NOT_APK** |
| MINEDU-ALFA-G2-2025 | 2025 | Enfoques y situaciones didácticas de enseñanza y aprendizaje del lenguaje escrito. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/11809) | Repositorio: CC BY 4.0 (dc.rights.uri); **DOWNLOAD_TO_REVIEW_NOT_APK** |
| MINEDU-ALFA-G3-2025 | 2025 | Situaciones para revisar hipótesis de escritura y reflexionar sobre el lenguaje escrito. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/11810) | Repositorio: CC BY 4.0 (dc.rights.uri); **DOWNLOAD_TO_REVIEW_NOT_APK** |
| MINEDU-INICIO-2026 | 2026 | Prioridades 2026, planificación de inicio y organización de la escuela. | [Fuente](https://repositorio.minedu.gob.pe/handle/20.500.12799/12677) | Repositorio: CC BY 4.0 (dc.rights.uri); **DOWNLOAD_TO_REVIEW_NOT_APK** |

## Protocolo de integración

1. `06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json` es la fuente estructurada, NO los textos ficticios de los mockups.
2. Antes de empaquetar contenido, ejecutar `07_SCRIPTS/descargar_minedu.py` desde un entorno autorizado con red: guarda PDFs para revisión en `09_PDF_MINEDU_DESCARGADOS/` y genera `ESTADO_DESCARGAS_MINEDU.json` con hashes reales; no usa rutas inventadas ni oculta errores.
3. Si un enlace oficial solicita comprobación humana, abrir el **enlace oficial ya registrado** en navegador y descargar manualmente al archivo que indique el catálogo; ejecutar `--validar-directorio` para producir hashes sin descargar.
4. Registrar `id`, título, autor, año, `fuente_url`, `license_text`, `license_url`, `sha256`, páginas verificadas cuando se pueda, `rights_review_status`, `bundled=false`, `lastChecked` y categoría. No inventar ni reproducir licencias/número de páginas del mockup.
5. Codex puede incorporar la ficha del documento y el enlace oficial sin el PDF original; la app permite importar el PDF local con SAF y enlazarlo al catálogo. Si más adelante se autoriza la distribución, actualizar derechos, checksum y pack versionado explícitamente.
6. No copiar párrafos largos ni imágenes de publicaciones con restricciones para usarlas como plantillas. Las ideas de taller aportadas en `08_CONTENIDOS_SEMILLA` son originales y no deben atribuirse al MINEDU.

## Distinción de uso de archivos

`09_PDF_MINEDU_DESCARGADOS` = carpeta para PDFs obtenidos **después**, fuera de este paquete; una descarga se realiza desde la fuente oficial. `08_CONTENIDOS_SEMILLA` = contenidos originales de ProfeLab que sí pueden servir como primer pack de catálogo. No modificar el PDF de origen; los metadatos se guardan aparte.

## Referencias tecnológicas

- ML Kit Text Recognition v2 Android bundled: https://developers.google.com/ml-kit/vision/text-recognition/v2/android — implementación `com.google.mlkit:text-recognition:16.0.1` (comprobar versión al integrar). El modelo Latin se incluye en APK y puede trabajar sin red desde la primera ejecución.
- ML Kit installation paths: https://developers.google.com/ml-kit/tips/installation-paths — Text v2 sí soporta bundled; el **Document Scanner** gestionado por Play Services no tiene modalidad bundled según esta tabla.
- Android Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files — `ACTION_OPEN_DOCUMENT` y URI persistible para PDFs elegidos.
- `ScanPro`: https://github.com/luisferxd09/ScanPro — fuente de ideas y paquete a inspeccionar bajo permiso/licencia. Documento `02_DOCUMENTACION/ANALISIS_SCANPRO_OCR_Y_REUTILIZACION.md`.
