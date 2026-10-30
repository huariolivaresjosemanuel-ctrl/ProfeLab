> HISTÓRICO: describe H0/H1/H1.1. Para esta entrega prevalecen README.md, REPORTE_QA_REAL.md, GUIA_FIRMA_Y_ACTUALIZACION.md y PENDIENTES_REALES.md de la raíz. Las antiguas pausas por hitos ya no aplican.

# Dependencias y compatibilidad

Consulta y preparación: 29/10/2026. Todas las versiones están fijadas en `gradle/libs.versions.toml`. Se eligió una combinación estable con requisitos declarados compatibles; no se afirma que sea la última versión de cada componente ni que el proyecto haya compilado.

| Componente | Selección | Justificación |
|---|---|---|
| Application ID | `pe.profelab.app` | Independiente de Control Personal DNI; no afirma propiedad de un dominio web. |
| min / compile / target SDK | 26 / 36 / 36 | Baseline Android 8, java.time nativo; dependencias Activity/Core seleccionadas exigen compile 36. |
| AGP / Gradle / JDK | 8.11.1 / 8.13 / 17 | Combinación soportada por AGP; Gradle Wrapper con hash de distribución y JAR oficial. |
| Kotlin / Compose compiler | 2.2.21 / 2.2.21 | Mismo plugin Kotlin/Compose; sin tabla antigua de compilerExtensionVersion. |
| KSP | 2.2.21-2.0.5 | Línea compatible con Kotlin seleccionado, artefacto oficial comprobado. |
| Compose BOM | 2025.10.01 | Línea estable fijada: UI 1.9.4 / Material3 1.4.0, compatible con AGP/SDK seleccionados según AAR. |
| Material icons | 1.7.8 | Catálogo vectorial existente, separado del BOM; pendiente medir tamaño APK. |
| Room | 2.8.5 | Corrección estable disponible; AAR minCompileSdk 34 y minAGP 8.1.1. Requiere API mínima 23, cubierta por 26. |
| Activity / Core | 1.11.0 / 1.17.0 | AAR requieren compileSdk 36 y AGP ≥8.9.1. |
| Lifecycle / Navigation | 2.9.4 / 2.9.5 | Estado Compose y navegación con restauración. Navigation 2 sigue suficiente para H1. |
| Coroutines / DataStore | 1.10.2 / 1.1.7 | Flow y preferencias persistentes. |
| WorkManager | 2.10.5 | Recordatorios diferibles; no promete alarmas exactas. |
| JUnit / Android Test | 4.13.2 / runner 1.6.2, ext 1.2.1, core 1.6.1 | Pruebas preparadas para CI. |

Se inspeccionaron POM y metadatos de 13 artefactos concretos (ver `audit/dependency_metadata.json`); no es una resolución completa de dependencias transitivas. `audit/compose-bom-2025.10.01.pom` conserva el BOM consultado. Gradle resolverá el grafo completo en Actions. Las versiones fijadas mejoran repetibilidad, pero sin lockfile resuelto, runner fijo por digest y verificación de todos los binarios no se promete compilación bit a bit reproducible.

## Fuentes primarias

- [Compatibilidad AGP 8.11](https://developer.android.com/build/releases/agp-8-11-0-release-notes): Gradle 8.13, JDK17, máximo SDK36.
- [Compatibilidad del plugin Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html): Kotlin 2.2.20–2.2.21 admite AGP hasta 8.11.1 y Gradle hasta 8.14 en la matriz consultada.
- [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler): plugin Compose alineado con Kotlin.
- [BOM oficial](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.10.01/compose-bom-2025.10.01.pom).
- [Room](https://developer.android.com/jetpack/androidx/releases/room), [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation), [WorkManager](https://developer.android.com/jetpack/androidx/releases/work).
- [KSP releases](https://github.com/google/ksp/releases).
- [Gradle checksum](https://gradle.org/release-checksums/) y [wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

## Dependencias previstas, aún no integradas

CameraX y ML Kit Latin bundled en H6; verificar versiones otra vez antes de incorporar. `PdfRenderer` del framework y PDFBox-Android como candidato en H5, con revisión de licencia/rendimiento. Ninguna de estas dependencias se presenta como funcional en H1. ScanPro/DocReader no se descargaron ni copiaron como fuentes.
