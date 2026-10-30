# PROYECTO PROFELAB — PAQUETE ÚNICO PARA ENTREGA INTEGRAL

**Instrucción vigente:** `PROMPT_MAESTRO_CODEX_PROFELAB_COMPLETO.md`.

Este paquete se entrega a Codex PARA que cree la versión integral de ProfeLab a partir del repositorio ACTUAL, no es el código Android ya completado ni APK instalable. El usuario desea una sola entrega final con todos los módulos aprobados, no pausar tras H2, H3, etc.

## Orden de lectura (para ahorrar tokens)

1. `INSTRUCCION_CORTA_PARA_PEGAR_CODEX.txt` (mensaje de entrada que acompaña el ZIP).
2. `PROMPT_MAESTRO_CODEX_PROFELAB_COMPLETO.md` (autoridad de tarea y entregable completo).
3. `01_ORIGINAL_MAESTRO_V2/02_DOCUMENTACION/SDD_MAESTRO_PROFELAB_V2.md` y documentos complementarios, matrices 46 vistas/191 QA.
4. Las 5 imágenes exactas `01_ORIGINAL_MAESTRO_V2/01_REFERENCIAS_UI/0*.png`.
5. Las fuentes semilla del Taller y las 12 ilustraciones en los directorios originales.
6. Los módulos ZIP separados (son COPIAS de selecciones por temática, para lectura rápida; NO entregables individuales).
7. Documentación de DocReader MIT y ScanPro; fuente MIT disponible mediante script offline-safe que solo descarga una vez cuando hay Internet.
8. Leer el repositorio real en GitHub SÓLO LECTURA; no sustituir por respaldos V1/H1 obsoletos.

## Cambios respecto al kit anterior

Se ha eliminado el mandato anterior de "empezar y detenerse en H2". Toda mención antigua de entregas por hitos o prompts históricos está SUPERADA por el nuevo `PROMPT_MAESTRO_CODEX_PROFELAB_COMPLETO.md`. Los hitos son una organización INTERNA de implementación; se exige **UN ZIP FINAL CON EL PROYECTO COMPLETO**.

## Qué contiene físicamente

- 83 originales del paquete maestro (incluyendo mockups, SDD, matrices, proyectos, imágenes y PDFs de QA).
- 6 ZIP temáticos derivados para consulta rápida (copias de los originales).
- Expediente DocReader MIT con licencia, SHA fijada, mapa de archivos y scripts para descargar y sanear la referencia en un entorno con red.
- Expediente ScanPro SIN permiso acreditado: auditoría documental, NO código reutilizable.
- Prompt maestro completo y plantilla de entrega.

## Qué NO contiene

- APK de ProfeLab terminada (Codex debe producirla mediante el código y las pruebas).
- Archivos secretos de firma de ProfeLab ni de terceros.
- Código fuente ZIP completo de DocReader y ScanPro (la descarga binaria del primero no estuvo disponible en el entorno de empaquetado; el segundo no se redistribuye sin permiso).
- 12 PDF oficiales MINEDU: hay catálogo de referencias y 3 PDF originales de PRUEBA; no atribuirles derechos inexistentes.

## Prohibiciones de seguridad y de alcance

Solo cinco pestañas Inicio, Taller, Sesiones, Cuaderno, Biblioteca; nada de nuevos módulos ajenos a SDD. GitHub SOLO LECTURA. GitHub Actions SOLO MANUAL mediante workflow_dispatch, sin push. Firmas permanentes exclusivamente del usuario, sin keystores en repositorio. No borrar datos anteriores ni fingir QA.
