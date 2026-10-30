# Corrección del fallo de compilación · ejecución17

Se consultó GitHub exclusivamente mediante lecturas. Base de esta corrección: `main`, commit `9c2a09cc0d5651b9eca36f61c4bfcddbee05d4b3`. Ejecución fallida: [41066315537](https://github.com/huariolivaresjosemanuel-ctrl/ProfeLab/actions/runs/41066315537), job `124098537432`.

## Causa y cambio

El compilador falló en `core/files/src/main/kotlin/pe/profelab/core/files/DocumentFiles.kt`, línea29 del código anterior: `PdfDocument().use { ... }`. `android.graphics.pdf.PdfDocument` tiene `close()`, pero no implementa Closeable/AutoCloseable. Por eso el compilador no encuentra una extensión `use` aplicable; la inferencia de R también falla como consecuencia. [API oficial PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument).

Se sustituyó por `val doc=PdfDocument()` y `try/finally { doc.close() }`. Cada página iniciada se finaliza incluso si falla el dibujo; las imágenes se reciclan en finally; se elimina una salida parcial si falla la generación. No se ocultan errores ni se desactivan lint o pruebas.

Se añadió `app/src/androidTest/kotlin/pe/profelab/app/ReportExportTest.kt`: un informe de varias páginas con imagen que se reabre, y un adjunto inválido que no deja PDF parcial y permite reintentar. Estos dos casos están **escritos, NOT_RUN**, hasta ejecutar un emulador.

## Evidencia real

El artefacto `informes-V1.0-17` contiene resultados JVM reales: HomeRulesTest10/10, InventoryRulesTest7/7 y ContentPolicyTest2/2, sin errores, fallos ni omisiones. Esos módulos no se modificaron en esta corrección. Sus XML se conservan en `docs/qa/correccion-ci-run17/`. La compilación completa del intento17 sigue **FAIL**; los jobs de emulador y firma quedaron omitidos.

También se recuperó el `2.json` generado realmente por KSP en ese intento, sin editar su identidad. Generar el schema no prueba migración ni actualización Android. Se conservan los esquemas1 y2.

La nueva corrección no se pudo compilar aquí: no hay JDK/SDK/Gradle disponibles y no se instalaron. No se lanzó ni reintentó Actions. Los controles locales se registran en REPORTE_QA_REAL.md; no garantizan ausencia de otros errores que antes quedaron detrás del módulo fallido.

## Subir y comprobar manualmente

1. Extrae el nuevo `PROFELAB_COMPLETO_V1_0_FUENTES.zip`; su raíz es la del proyecto. Sustituye el contenido de tu copia de ProfeLab por esta entrega completa, incluidas las carpetas ocultas. Revisa `INVENTARIO_COMPLETO.csv`, comparado contra el commit fallido. No hay rutas que retirar respecto a esa base.
2. El cambio funcional está en DocumentFiles.kt. Incluye también la prueba nueva, el schema2 real, las evidencias y documentación actualizadas y los inventarios/hashes. El `.gitignore` local conserva exclusiones de cachés/APK/claves que no estaban en el último commit; no elimina ninguna exclusión anterior.
3. Sube tú los archivos al repositorio cuando decidas. El asistente no hizo commit/push ni modificó GitHub. No hay APK nueva instalada ni datos migrados aquí.
4. GitHub → Actions → ProfeLab Android → **Run workflow**, nueva ejecución sobre main; `run_emulator=true`, `signed_release=false` para comprobar. No basta con reintentar el run17: eso compilaría su commit antiguo.
5. Revisa build y API26/API36. Si aparecen nuevos errores, conserva la línea concreta `e: ...` o el informe de lint. Una compilación corregida todavía no se declara PASS en esta entrega. Para firma permanente mantén la guía existente y las precauciones de compatibilidad con H1.

Workflow sin cambios: solo `workflow_dispatch`. Se preservan las cinco pestañas y los contenidos originales; no se modifica Control Personal DNI ni otros proyectos.
