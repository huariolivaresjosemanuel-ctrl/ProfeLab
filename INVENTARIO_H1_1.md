# Inventario H1.1

Base: `5891cd9257eb966dbce319b4b55f5dc86436fe9d`. Archivos completos, con rutas relativas exactas.

| Ruta | Acción |
|---|---|
| `.github/workflows/android.yml` | REEMPLAZAR |
| `.gitignore` | NUEVO |
| `ERRORES_CORREGIDOS_H1_1.md` | NUEVO |
| `ESTADO_REQUISITOS_H1_1.csv` | NUEVO |
| `INFORME_QA_H1_1.md` | NUEVO |
| `INSTRUCCIONES_SUBIDA_H1_1.md` | NUEVO |
| `INVENTARIO_H1_1.md` | NUEVO |
| `PROPUESTAS_NO_IMPLEMENTADAS.md` | NUEVO |
| `SHA256SUMS.txt` | REEMPLAZAR |
| `app/build.gradle.kts` | REEMPLAZAR |
| `docs/H1_1_FIRMA_Y_TRANSICION.md` | NUEVO |
| `docs/H2_PREPARACION.md` | NUEVO |
| `docs/qa/h1.1/certificado-h1-publico.json` | NUEVO |
| `docs/qa/h1.1/pruebas-firma.json` | NUEVO |
| `docs/qa/h1.1/revision-base.json` | NUEVO |
| `docs/qa/h1.1/verificacion-workflow.json` | NUEVO |
| `scripts/signing.py` | NUEVO |
| `scripts/test_signing.py` | NUEVO |
| `scripts/verify_project.py` | REEMPLAZAR |

Motivos: Gradle y workflow para firma/versionado; helpers y pruebas de protección de release; .gitignore para archivos privados; informes/estado por requisito y preparación H2; índice de hashes actualizado. Ningún keystore o contraseña real se incluye.
