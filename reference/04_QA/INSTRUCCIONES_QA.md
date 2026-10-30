# Ejecución QA ProfeLab V1.0

La matriz contiene **134 casos**. Estado inicial NO_EJECUTADO; no está permitido marcar PASS sin evidencia.

- `UNITARIA`: tests automatizados del dominio/repositorios; guardar log de tests.
- `INTEGRACION`: tests de DAO/ViewModels/Android UI, datos persistidos.
- `DISPOSITIVO`: requiere instalación limpia y prueba física; indicar dispositivo/Android/fecha.
- `ESTATICA`: auditoría de código/permisos/ZIP/licencias.
- `VISUAL`: comparación contra referencias con capturas autorizadas.
- `CI`: log de GitHub Actions después de que el usuario suba archivos manualmente.

Casos de bloqueante `P0`: pérdida de datos, red obligatoria, OCR sin modelo, permisos mal gestionados, links rotos, botones inertes, respaldo corrupto sin detección, DPI/accesibilidad severa.

Para cada FAIL crear una incidencia con: ID de prueba, reproducibilidad, esperado, actual, archivo afectado, corrección, nueva evidencia de retest.

## Extensión modular, contenidos y permisos

Los nuevos casos `QA-088` a `QA-134` son obligatorios. Ejecutar suites de módulo + integración + regresión cruzada por hito, en especial denegación/revocación de permisos, linterna sin flash, importación de packs maliciosos/incompatibles, actualización idempotente, migraciones sin pérdida y uso offline tras importar PDFs/ideas. Nunca marcar PASS en pruebas de dispositivo no ejecutadas. Registrar versión Android, teléfono/tableta y evidencia reproducible.
