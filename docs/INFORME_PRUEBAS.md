# Informe de pruebas · entrega H1

Fecha de entrega: 29/10/2026, America/Lima. Fuentes `0.1.0-h1`. Equipo de preparación: Windows; Python y Poppler ya incluidos en el runtime de Codex. No se instaló ni ejecutó toolchain Android.

## Ejecutado

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Lectura e inventario de 83 archivos, incluida inspección de ZIP histórico | PASS de inspección estructural | `audit/package_review.json`, `audit/historical_zip.json` |
| Hashes del paquete maestro | 81/81 PASS | `checksums` en `audit/package_review.json` |
| 19 PNG decodificables y dimensiones; cinco mockups inspeccionados | PASS de archivo/referencia | Registro de auditoría; no prueba visual Android |
| 4 PDF parseados; fixture cifrado descifrado con contraseña QA | PASS de archivo | páginas/texto/cifrado en auditoría, extracción `.txt` |
| Guía visual renderizada e inspeccionada | PASS con advertencias de sustitución de fuentes Poppler | `audit/guia_visual.png` |
| Existencia de 13 artefactos/metadata oficiales y requisitos AAR | 13/13 PASS de metadatos | `audit/dependency_metadata.json` |
| Gradle Wrapper oficial y SHA oficial del JAR; distribución fijada | PASS de integridad | `audit/wrapper_provenance.json` |
| Suite estática final: módulos, permisos, referencias, datos, rutas, SQL, CI, wrapper | 12/12 PASS | `qa/static-checks.json` |

La comprobación SQL ejecuta `EXPLAIN` de las consultas exactas del DAO contra SQLite con las columnas de las entidades. **No ejecuta Room ni prueba sus transacciones Android.** Los checks de navegación inspeccionan rutas; no simulan pulsaciones.

## Fallos encontrados y corregidos

1. Primera suite estática: 11 PASS, 1 FAIL. El parser del verificador omitía `DraftEntity` porque su declaración ocupa una sola línea. Se corrigió la expresión del verificador, se volvió a comprobar el SQL y la suite terminó 12/12. Informe original conservado en `qa/static-checks-first-run.json`.
2. Descarga inicial del wrapper: error DNS en el sandbox. La descarga de archivos oficiales se completó con acceso de red autorizado, verificando hash. No hubo instalación ni compilación.
3. Revisión de código: corregidos exposición de Room en API del módulo, configuración `java-library`, icono de chevron, conservación de completado concurrente y espera de guardado al salir de editores. Son correcciones de revisión estática, no fallos reproducidos en dispositivo.

No se detectó ningún fallo restante en las comprobaciones locales ejecutadas. Esto no acredita ausencia de errores de compilación, runtime o diseño.

## Definido pero NO ejecutado

| Suite/criterio | Estado | Cómo obtener evidencia |
|---|---|---|
| 12 pruebas JVM Kotlin (HomeRules y ContentPolicy) | NOT_RUN | Actions `:core:model:test :core:security:test` |
| 4 pruebas instrumentadas Room (reapertura, borrador, concurrencia, avisos) | NOT_RUN | job `android-tests`, API 26 y 36 |
| 2 pruebas instrumentadas UI (cinco tabs y recreación de editor) | NOT_RUN | job `android-tests` |
| Compilación Kotlin, recursos, manifiesto combinado, KSP, lint y APK | NOT_RUN | job `build`; descargar logs y APK solo si termina bien |
| Pruebas de los 191 escenarios originales | NOT_RUN | `reference/04_QA` conservada; ejecución incremental por hito |
| Fidelidad visual de Inicio, 200% de fuente, TalkBack, teclado/insets | NOT_RUN | Teléfono + tablet y capturas comparadas con `01_INICIO.png` |
| Primera apertura en modo avión, cierre forzado y poco almacenamiento | NOT_RUN | Instalación limpia y datos locales reales en dispositivo de prueba |
| Permiso de notificaciones denegado/revocado, reinicio y batería | NOT_RUN | Android 13+ y API 26 sin permisos de notificación runtime |
| Instalación real en Lenovo Tab Plus y rendimiento | NOT_RUN | Tablet física con versión de Android registrada |
| Migración 1→2, backup/restore, packs, lector PDF, OCR/cámara/audio | NOT_RUN / implementación en H2–H7 | No disponibles en H1 |

## Reproducción de comprobación local sin Android

Desde la raíz del proyecto: `python scripts/verify_project.py`. Solo usa biblioteca estándar y modifica el informe `docs/qa/static-checks.json`. Para integridad de entrega usar `python scripts/verify_inventory.py` **antes** de editar archivos; una edición legítima cambiará los hashes.

## Aceptación manual H1

1. Ejecutar Actions, guardar URL/run ID, logs, SHA del APK y esquema Room generado. Si build falla, no declarar APK disponible.
2. Instalar en equipo de prueba, modo avión: no pide cuenta ni permisos al entrar; cinco tabs y estado vacío reales.
3. Perfil 6.º ciclo; volver/reabrir. Crear tareas con/sin fecha, prioridades y cinco fechas de semana actual; completar tres y comprobar 3/5, deshacer y reiniciar.
4. Editar sin confirmar; girar, salir por Volver y volver a abrir. Confirmar recuperación del borrador; descartar no altera la tarea anterior.
5. Eliminar tarea con confirmación; abrirla desde actividad muestra origen no disponible. Aviso no vuelve a dispararse.
6. Activar avisos con fecha futura; denegar permiso y comprobar que se puede guardar. Conceder, probar canal bloqueado y cancelación tras completado. Registrar retrasos de Android sin esperar exactitud al minuto.
7. Biblioteca muestra 12 referencias, busca título/área; ningún documento afirma disponibilidad PDF offline. Navegador solo abre por toque explícito.
8. Comparar Inicio con mockup a tamaño móvil y tablet; texto 200%, navegación gestual, teclado y TalkBack. Guardar capturas de vacío y datos creados, sin menores identificables.

H1 se entrega como fuentes listas para la primera compilación; **aceptación instalable y visual pendiente**. No se entrega un certificado de QA Android.
