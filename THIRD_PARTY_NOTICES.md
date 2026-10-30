# Procedencia y avisos de terceros

El código específico de ProfeLab se crea para este proyecto. Esta entrega no asigna una licencia pública al código del usuario ni registra la marca ProfeLab.

La licencia íntegra que acompaña al Gradle Wrapper se conserva en `licenses/Gradle-Apache-2.0.txt`.

| Componente | Titular / licencia publicada | Uso |
|---|---|---|
| Gradle Wrapper | Gradle Inc. y colaboradores, Apache License 2.0 | Scripts y JAR oficiales sin modificaciones; hashes y origen en `docs/audit/wrapper_provenance.json`. |
| AndroidX, Compose, Room, Navigation, Lifecycle, DataStore, WorkManager | Android Open Source Project, Apache License 2.0 | Dependencias Maven; no vendorizadas en las fuentes. |
| Kotlin, kotlinx.coroutines, KSP | JetBrains / Google y colaboradores, Apache License 2.0 | Compilación/runtime; licencias se deben conservar al distribuir. |
| Material Icons | Google / AOSP, Apache License 2.0 | Iconos vectoriales del artefacto Compose. |
| JUnit | JUnit contributors, EPL 1.0 | Solo pruebas. |

Fuentes: [Gradle LICENSE](https://github.com/gradle/gradle/blob/v8.13.0/LICENSE), [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Kotlin](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt), [KSP](https://github.com/google/ksp/blob/main/LICENSE), [JUnit](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt).

MINEDU: se incluye únicamente el catálogo de metadatos/enlaces aportado por el usuario; derechos de cada PDF pendientes, ningún PDF oficial distribuido. Los tres PDFs de QA y las miniaturas editoriales permanecen bajo `reference/`, fuera del APK. Los cinco mockups también quedan fuera del APK. No se copian fotos de perfil ni portadas oficiales de esas capturas.

El libro y planta Canvas de `core:ui` y el icono de aplicación son dibujos vectoriales escritos para esta entrega a partir de la identidad solicitada; no son recortes del mockup. No se usaron assets o código de ScanPro/DocReader, ni claves privadas de terceros.

Antes de un release público: generar inventario de dependencias transitivas realmente resueltas y sus avisos, revisar derechos del contenido y mantener las atribuciones que correspondan. Esta lista inicial no sustituye esa auditoría de distribución.
