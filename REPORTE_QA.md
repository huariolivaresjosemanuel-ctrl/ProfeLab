# QA V1.0.1 — evidencia ejecutada y pendientes

Fecha de preparación: 30 de octubre de 2026. Base 81efabe5e12e0427eaeceedd68bba11b514a1e3b, proyecto local ProfeLab_Taller_V1_0_1. GitHub exclusivamente lectura; ninguna ejecución Actions iniciada por Codex. No se instaló Java, Gradle, Android SDK ni Node.

| Control realmente ejecutado | Resultado | Alcance y evidencia |
|---|---|---|
| scripts/verify_project.py | PASS, 16/16 | Estructura de 16 módulos/grafo/aislamiento, cinco destinos, manifest, semillas/recursos, SQL/migración por simulación Python. evidencias/estructura.log |
| Python unittest scripts/test_signing.py | PASS, 11/11 | Auxiliar de firma, validaciones y fallos esperados con apksigner/aapt simulados y bytes de prueba. No firma una APK Android. evidencias/firma_simulada.log |
| scripts/check_kotlin_delimiters.py | PASS, 105 archivos sin errores | Solo balance de delimitadores/cadenas. No resuelve símbolos, tipos, overloads o dependencias. evidencias/delimitadores.json |
| tools/preflight_taller_v101.py (auditoría de entrega) | PASS, 5/5 | JSON/PNGs originales exactos, índice/5 layouts/10 bloques, 45 IDs y nombres derivados, esquema/workflow/otras features preservados, versión/retirada del renderer crudo. evidencias/preflight.json |
| Integridad del ZIP y reconstrucción del parche | Consulte VERIFICACION_ENTREGA.json | Reapertura ZIP, CRC, rutas/bytes/hashes y cobertura inventario; es una prueba de entrega, no de app |
| Compilación Gradle/Kotlin, lint y generación de APK | BLOCKED | java/gradle/adb no disponibles en PATH; entorno sin JDK/SDK operativo. Usuario prohíbe instalarlos. No se intentó presentar un fallo de compilación como resultado de un comando inexistente |
| JVM Kotlin: 18 tests nuevos + regresiones heredadas | NOT_RUN | Requiere toolchain Gradle/JDK; no se reutilizan aprobaciones de otra versión |
| Android: 8 tests nuevos + instrumentación heredada | NOT_RUN | Sin compilación/emulador/dispositivo accesible. Workflow existente puede ejecutarlos manualmente |
| Teléfono/UI/rotación/reinicio/accesibilidad/fidelidad/backup/firma real | NOT_RUN | GUIA_PRUEBAS_USUARIO.md y matriz funcional |
| Matriz maestra de 191 escenarios | NOT_RUN, 191 | Archivo original conservado; no se declara ninguno aprobado por inspección |

Ninguna comprobación ejecutada de la suite final terminó en FAIL. Esto **no demuestra ausencia de errores Android**. El fixture de tests puede contener rechazos esperados dentro de pruebas PASS; no son fallos productivos. Hubo correcciones durante preparación de scripts (restauración de bytes de .gitignore y lectura UTF-8 de metadatos); no se contabilizan como compilaciones ni como pruebas funcionales de Android.

## Tests nuevos escritos, sin ejecutar

- WorkshopRulesTest: 18 casos JVM de decimal/legado/identidad, conversiones, paquetes conocidos/desconocidos, incompatibilidades, reservas, consolidación, reutilizables/herramientas y seguridad separada.
- WorkshopPersistenceTest: 4 casos Android de adaptación, recursos semilla, selección/recibos idempotentes con compra parcial y persistencia Room, rollback transaccional. Usa Room en memoria; no demuestra por sí solo reinicio de teléfono o migración desde una instalación real.
- WorkshopUiTest: 2 casos Compose de guía/materiales/modos, y copia/checklist comprobado en Room y de nuevo en UI tras recrear Activity.
- WorkshopContentPackTest: 2 casos de paquete con imagen/layout/material, idempotencia/preservación de registro propio y rechazo de archivo no listado.

Total: 26 tests nuevos en fuentes. Todos requieren compilación antes de ejecutarse. Los tests UI no sustituyen la aceptación visual de los cinco mockups.

## Reproducción de los controles locales

Desde la raíz del proyecto, con Python ya disponible:
```text
python scripts/verify_project.py
python -m unittest discover -s scripts -p test_signing.py -v
python scripts/check_kotlin_delimiters.py
```
Si el entorno restringe temporales, test_signing admite PROFELAB_TEST_TEMP hacia una carpeta local escribible. El control preflight es una auditoría realizada en el workspace con el árbol Git base; su resultado y condiciones exactas se incluyen en evidencias/preflight.json. No es una dependencia de compilación del repositorio.

Los registros antiguos bajo docs/qa del repositorio siguen siendo históricos. Los logs adjuntos y resultados.json son los de esta actualización; no se incorporan como reemplazos de esos informes previos.

## Condiciones para aceptar la versión

Una ejecución nueva del workflow sobre el commit integrado debe completar compilación, lint, tests JVM y Android API 26/36. Después, probar actualización firmada con datos previos, los flujos del teléfono y comparación visual. Sin esos resultados no se certifica el Taller como terminado ni la APK como válida. Los ocho experimentos requieren además revisión editorial humana antes de su publicación infantil.
