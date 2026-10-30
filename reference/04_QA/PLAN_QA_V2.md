# QA total: 191 escenarios (NO EJECUTADOS)

- Se preservaron **134** casos de V1.1 exactamente; se añadieron **57** casos V2, `QA-135..QA-191`.
- La matriz `MATRIZ_QA_PROFELAB_V2_TOTAL.csv` contiene **191** filas con `ESTADO=NO_EJECUTADO`. Este número significa **definidos, no aprobados**.
- Validar por feature en GitHub Actions: tests de unidad de inventario/pack/ACL, instrumentation UI y Android reales con cámara/PDF/stylus. Reportar por versión APK, fecha, dispositivo/Android, logs depurados sin datos personales y captura de evidencia.
- PDF: fixtures originales sin derechos de terceros, exportación anotada validada con render comparado, SHA-256 del original igual antes/después.
- Modo avión: primera apertura, OCR bundled y lectura documentos ya importados. Si un PDF MINEDU solo está referenciado, mostrar acción de importar, no descargar ocultamente.
- Estados QA solo PASS/FAIL/BLOCKED/NOT_RUN; no convertir pruebas teóricas en PASS.
