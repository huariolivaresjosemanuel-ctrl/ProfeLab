# Pruebas realmente realizadas · 30 de octubre de 2026

Alcance: fuentes integradas V1.0. No se instaló JDK/SDK/Gradle local, no se lanzó GitHub Actions y no hay capturas de una APK V1.0. Los resultados H1/H1.1 son antecedentes y no validan estas modificaciones.

| Área | Estado | Evidencia / alcance exacto |
|---|---|---|
| Integridad estructural y datos originales | PASS | `scripts/verify_project.py`, 16/16; `docs/qa/static-checks.json` |
| Firma, lógica Python | PASS | 11/11 en `scripts/test_signing.py`; log en `docs/qa/completo/firma-python.txt`. Bytes de clave ficticios; apksigner/aapt simulados. NO es una firma real. |
| Delimitadores Kotlin/KTS | PASS | `scripts/check_kotlin_delimiters.py`; `docs/qa/completo/delimitadores.json`. Comprueba comillas/paréntesis/llaves; NO es compilador ni parser Kotlin. |
| Metadatos de cinco AAR nuevos | PASS | `docs/qa/completo/dependencias-nuevas.json`. Artefactos consultados en Google Maven/Maven Central y minCompileSdk/AGP cuando constan. No se resolvió el grafo Gradle completo. |
| SQL de migración H1→v2 | PASS limitado | El control estático ejecuta el SQL original del schema1 y MIGRATION_1_2 en SQLite Python, preservando tarea/perfil centinela. No ejecuta Room Android ni valida su identityHash. |
| Integridad del ZIP de entrega | PASS al empaquetar | Verificador de inventario y reapertura ZIP; evidencia `docs/qa/completo/empaquetado.json`. No es compilación. |
| `assembleDebug` y `assembleDebugAndroidTest` | BLOCKED | No hay java/gradle/adb en PATH ni SDK configurado en este entorno. No se instalaron herramientas. Preparado en Actions manual. |
| lintDebug / lintRelease | NOT_RUN | Requieren la compilación Android. |
| Tests JVM Kotlin (incluido inventario) | NOT_RUN | No se ejecutó Gradle. Hay siete casos de cantidades/reservas/unidades/consolidación en InventoryRulesTest, además de los heredados. |
| Room Android, persistencia y migración | NOT_RUN | PersistenceTest y MigrationTest escritos; requieren emulador. La prueba nueva de migración usa SQLite Android y centinela, no MigrationTestHelper completo. |
| PDF real, capa de texto y copia anotada | NOT_RUN | PdfIntegrationTest preparado con fixture seleccionable, hash del original y comparación de render. Se debe ampliar y ejecutar para rotaciones 0/90/180/270, CropBox, contraseña y anexos. |
| OCR local con fixture | NOT_RUN | OcrIntegrationTest preparado; no acredita primer arranque en modo avión ni calidad de manuscritos. |
| Packs, antizip-slip, idempotencia y fusión backup | NOT_RUN Android | Tres tests ContentAndBackupTest preparados. No se probaron cortes de proceso, almacenamiento lleno ni todos los ZIP maliciosos. |
| Navegación y recreación | NOT_RUN | NavigationTest adaptado; requiere Compose/emulador. |
| Firma criptográfica / dos actualizaciones reales | BLOCKED | Falta clave privada del usuario y dispositivo de prueba. No se reconstruye la clave a partir del certificado público. |
| UI contra cinco PNG, móvil/tablet/stylus/TalkBack | NOT_RUN | Sin emulador ni dispositivo conectado. Comparación de composición de fuentes en INFORME_FIDELIDAD_UI.md, sin aprobación visual. |
| Cámara, linterna, enfoque, voz, permisos denegados | NOT_RUN | Requieren hardware o emulador compatible. |
| Modo avión, rendimiento, proceso muerto, 191 escenarios QA | NOT_RUN | Matriz completa preservada; ningún caso Android se marca PASS por existir una función. |

## Comandos ejecutados

Se utilizó el Python incluido en el entorno. Comandos equivalentes desde la raíz del proyecto:

```text
python scripts/verify_project.py
python -m unittest discover -s scripts -p test_signing.py -v
python scripts/check_kotlin_delimiters.py
python scripts/verify_inventory.py
```

Para las pruebas de firma se fijó `PROFELAB_TEST_TEMP` en una carpeta temporal de trabajo autorizada. Una ejecución previa con el temporal predeterminado falló por permisos del sandbox; al usar una carpeta permitida pasaron los once casos. No se ocultó un fallo de Android: Android nunca llegó a ejecutarse. Lecturas de rutas equivocadas y una URL de tag inexistente durante revisión se corrigieron; no cuentan como tests del producto.

## Aceptación pendiente (una sola versión integrada)

Ejecutar manualmente Actions con emuladores. Conservar informes y schema2 generado por KSP. En dispositivos de prueba API26 y API36 crear perfil/tareas/notas/sesión/idea, importar los tres fixtures PDF, añadir marcas y páginas vinculadas, grabar voz, obtener OCR con primera instalación en modo avión y exportar copias a otro lector. Reiniciar/rotar/matar proceso y comprobar registros. Comprobar backup con y sin archivos, restaurar en instalación de prueba con registros existentes y verificar que no se pisan. Probar ZIP corrupto/duplicado/traversal y espacio insuficiente. Finalmente instalar DOS APK de firma idéntica y códigos crecientes, sin desinstalar, y comprobar datos y marcas. La transición desde H1 requiere además certificado compatible.

La primera comprobación final de inventario falló porque el generador excluía también el SHA256SUMS original de reference/, en lugar de excluir solo el control de la raíz. Se corrigió esa exclusión y se repitió el empaquetado y la comprobación completa de hashes. No hay resultados FAIL finales en las suites locales ejecutadas. Existen capacidades parciales e incertidumbres de ejecución; se enumeran en PENDIENTES_REALES.md y no se convierten en PASS.
