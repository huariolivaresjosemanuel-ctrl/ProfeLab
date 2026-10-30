# QA H1.1

Base remota `5891cd9257eb966dbce319b4b55f5dc86436fe9d`; run H1 41002583890: build y publicación debug completados correctamente según GitHub, emuladores skipped. Esa evidencia corresponde a H1, no a las modificaciones H1.1.

- PASS: 14 comprobaciones estructurales locales del proyecto.
- PASS: 11 pruebas Python del helper (ausencia de secretos, Base64 inválido, código repetido/menor, protección contra sobreescritura, limpieza, huella incorrecta, firma rechazada, falta de v2, aplicación incorrecta y salida pública). Son pruebas con bytes ficticios y herramientas simuladas; no producen un keystore real ni verifican una firma criptográfica real.
- PASS: controles estáticos de workflow, XML/TOML/JSON leídos y referencias sin cambios; certificado público extraído de la APK H1 y hash de esa APK calculado.
- PASS: ZIP de cambios, CRC, rutas y bytes cotejados; hashes de entrega preparados.
- ERROR DE ENTORNO resuelto: primera ejecución de pruebas usó la carpeta temporal restringida de Windows y produjo errores de permisos (10 tests, 16 errores incluidos cleanup). Se repitió usando temporales dentro del workspace; las 11 pruebas actuales pasan. No fue un fallo de firma real.
- NOT_RUN: compilación Gradle H1.1, lint debug/release, pruebas JVM e instrumentadas H1.1, generación de release con clave del usuario, apksigner/aapt reales, actualización firmada de un teléfono y recuperación de datos. No hay JDK/Android SDK local utilizado y no se instaló toolchain.
- BLOCKED para aceptación de transición: falta identificar la APK efectivamente instalada y verificar disponibilidad de su clave privada o una estrategia de conservación de datos. No hay clave privada en esta entrega.
- NOT_RUN: fidelidad visual real, capturas Android, TalkBack, fuente200%, Lenovo Tab Plus. H1.1 no modifica las composiciones UI ni mockups; no se declara aprobación visual. Las diferencias de disposición H1 respecto a las cinco referencias y el desarrollo H2–H7 siguen pendientes de prueba por hito.

Versión propuesta `0.1.1-h1.1`. Room sigue en versión1, sin migraciones ni cambios de tablas. Para aceptar H1.1: configurar secretos/huella, correr workflow sobre main con emuladores y release, verificar artefacto/firma, y probar DOS builds con igual certificado y códigos crecientes, con tareas/perfil conservados. La instalación H1 con certificado distinto no se usa como prueba de actualización satisfactoria. No desinstalar datos importantes.
