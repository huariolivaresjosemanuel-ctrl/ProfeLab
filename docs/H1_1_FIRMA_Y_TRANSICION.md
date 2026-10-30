> HISTÓRICO: describe H0/H1/H1.1. Para esta entrega prevalecen README.md, REPORTE_QA_REAL.md, GUIA_FIRMA_Y_ACTUALIZACION.md y PENDIENTES_REALES.md de la raíz. Las antiguas pausas por hitos ya no aplican.

# H1.1 · Firma permanente y transición

Base auditada: `main`, commit `5891cd9257eb966dbce319b4b55f5dc86436fe9d`. [Compilación H1 exitosa](https://github.com/huariolivaresjosemanuel-ctrl/ProfeLab/actions/runs/41002583890). No se ha escrito en GitHub ni generado una clave privada por el asistente.

## Estado comprobado

Una APK, `pe.profelab.app`, mínimo API26, compile/target36, 16 módulos. Inicio, tareas, perfil, Room v1 y navegación existentes conservados. La APK H1 se construyó como debug con la configuración de firma estándar de Android; no existe configuración de firma permanente en esa base. El job de emuladores quedó omitido. Las otras secciones mantienen el alcance parcial de H1, no se consideran producto terminado.

Se descargó el artefacto `ProfeLab-H1-debug-6`, id 12954415207, de ese run. SHA-256 de su APK:

`fac34f13fd8ed6a0ab6b3ab349ddf30f3450b604441b36dab5514908472fac50`

Se extrajo el certificado público DER de su bloque de firma APK v2; huella SHA-256:

`ed49929e7923b1e1ba8bc4df03429255f39dc578dd7d7468df3ba93f566fa0a5`

Esta lectura identifica el certificado declarado, no sustituye la verificación criptográfica con apksigner. Tampoco acredita que sea exactamente la APK instalada en tu teléfono. No se encontró un keystore en fuentes o artefactos. La APK solo contiene el certificado público: no permite recuperar la clave privada. No se inspeccionaron ni alteraron cachés privados del usuario. Si conservas el keystore original, se puede evaluar su uso; no se afirma que esté perdido definitivamente.

## Compatibilidad con la instalación actual

Android exige identidad de paquete y firma compatibles para actualizar conservando datos. Mantener solo el applicationId no basta. Para API26 no se propone rotación de certificados como solución a la falta de la clave original.

1. Conserva el instalador H1 original. En un equipo con Android Build Tools ya disponibles, ejecuta `apksigner verify --verbose --print-certs H1.apk`. Es información pública; guarda su huella SHA-256.
2. Para comprobar lo instalado, si ya tienes adb: `adb shell pm path pe.profelab.app`; copia la ruta exacta devuelta mediante `adb pull` y verifica esa APK con apksigner. No se instalan herramientas en tu PC en esta tarea.
3. Compara ese certificado con el del keystore que tú poseas (`keytool -list -v -keystore RUTA -alias ALIAS`, contraseña interactiva). Si tienes la misma clave privada y certificado, una APK con código de versión mayor puede actualizar; debe probarse en equipo de prueba con datos ficticios.
4. Si solo tienes una clave nueva y la huella difiere, NO se garantiza actualización directa. Android normalmente rechazará el instalador. No fuerces una desinstalación para continuar.
5. H1 no implementa aún exportación/restauración completa de datos personales y tiene Auto Backup deshabilitado. H1.1 tampoco añade backup H7. Puedes copiar manualmente texto de tareas/perfil, pero eso no es una restauración automática. No prometemos que extraer SQLite por adb funcione en todos los dispositivos; no es un flujo validado de respaldo.
6. En el caso de firma distinta, la instalación inicial con clave permanente queda pendiente de una decisión del usuario y de proteger sus datos. Puedes probarla en otro dispositivo sin desinstalar la H1 que contiene trabajos. No se recomienda borrar datos. Un backup posterior no recupera información eliminada antes de exportar.

## Crear o reutilizar la clave bajo tu control

Si posees la clave privada de H1, evalúa primero conservarla: generar otra no reproduce su certificado. Si no la posees y decides iniciar la identidad permanente, genera una sola clave en un equipo confiable con JDK ya disponible, fuera de cualquier carpeta Git, OneDrive compartida o ZIP de fuentes. El asistente no ejecuta esta operación.

Ejemplo interactivo (elige tu ruta privada):

```text
keytool -genkeypair -v -storetype PKCS12 -keystore RUTA_PRIVADA/profelab-release.p12 -alias profelab -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore RUTA_PRIVADA/profelab-release.p12 -alias profelab
```

Escribe contraseñas solo en las solicitudes interactivas; no en argumentos ni archivos de código. En PKCS12 usa la contraseña del almacén también como contraseña de la clave. Si reutilizas JKS con contraseñas distintas, conserva ambas. No copies certificados de otras aplicaciones.

Guarda dos respaldos protegidos del keystore en medios bajo tu control y contraseñas en un gestor independiente. Verifica que una copia puede abrirse y tiene la misma huella antes de depender de ella. Perder la clave o su contraseña puede impedir actualizar una app distribuida fuera de Play. Cambiarla por otra arbitraria no resuelve esa pérdida.

Para convertir tu archivo a Base64 en PowerShell sin imprimirlo en consola:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('RUTA_PRIVADA\profelab-release.p12')) | Set-Clipboard
```

Pega directamente en el secreto de GitHub y después limpia el portapapeles (`Set-Clipboard -Value ''`). Base64 no cifra el archivo. No lo pegues en este chat, issues, variables públicas, logs o archivos del repositorio. Revisa el historial/sincronización de portapapeles según tu configuración.

## Configurar GitHub manualmente

Repositorio → Settings → Secrets and variables → Actions → Secrets. Crear una sola vez:

| Secreto | Valor bajo tu control |
|---|---|
| `PROFELAB_KEYSTORE_BASE64` | Archivo keystore codificado en una sola línea |
| `PROFELAB_KEYSTORE_PASSWORD` | Contraseña del almacén |
| `PROFELAB_KEY_ALIAS` | Alias exacto elegido |
| `PROFELAB_KEY_PASSWORD` | Contraseña de la clave |

En Actions → Variables (datos públicos), configurar:

- `PROFELAB_SIGNING_CERT_SHA256`: huella del certificado de TU keystore (64 dígitos hexadecimales; se aceptan separadores `:`). No uses la huella H1 por copiarla si tu clave es diferente: el build fallará deliberadamente.
- `PROFELAB_LAST_DISTRIBUTED_VERSION_CODE`: mayor código distribuido hasta ahora con la identidad prevista. Primera configuración después de H1: `1`; si ya distribuiste códigos superiores, usa el mayor real.

Los secretos permanecen en GitHub por decisión del propietario. Limita escritura del repositorio y ejecución de workflows a personas de confianza, protege `main` y revisa cambios antes de usar secretos. Ningún administrador del asistente configura o recibe esos secretos.

## Flujo preparado

Actions → ProfeLab Android → Run workflow sobre `main`: `run_emulator=true`, `signed_release=true`. Debug normal no requiere secretos; un release solicitado requiere configuración completa. Si se solicita release sin emuladores, el build falla con explicación.

El job firmado depende del build y de los tests instrumentados API26/36. Después reconstruye el keystore en `$RUNNER_TEMP/profelab-signing`, permisos 700/600 en Linux. Gradle recibe ruta/credenciales mediante entorno, sin copiar archivos al repo. Caché desactivada para ese job y configuration cache desactivada. No se ejecuta generación de claves.

Versión de fuentes: `0.1.1-h1.1`, code por defecto 2 para debug. Release usa `1000 + github.run_number`, y rechaza un código no mayor al valor público de última distribución. Cada nueva actualización distribuida debe venir de un nuevo Run workflow; rerun de la misma ejecución conserva el código. No reinicies el contador del workflow para distribuir versiones sin revisar el máximo registrado. Tras distribuir una APK, actualiza manualmente la variable de último código con el número de `firma-publica.json`.

Antes de publicar, apksigner verifica firma (incluido v2 para API26), el helper compara el certificado con la huella fijada y aapt comprueba paquete/versionCode/versionName. Si falla, no se publica el release. El paso `always()` elimina el directorio temporal; si el runner se pierde abruptamente, no puede garantizarse ejecutar el cleanup, y se depende de la destrucción del runner efímero de GitHub.

Artefactos diferenciados:

- `ProfeLab-H1.1-DEBUG-no-distribuir-*`: clave debug temporal, solo pruebas. No asumir compatibilidad de actualización entre esos builds.
- `ProfeLab-H1.1-FIRMA-PERMANENTE-*`: únicamente APK verificada, SHA256SUMS y firma-publica.json. Ningún keystore, contraseña, certificado privado o directorio de build se publica.

Gradle falla si se intenta ensamblar release sin habilitar y configurar la firma. En ejecución directa también debe comprobarse el certificado con el helper; el workflow es la ruta soportada. Este sistema protege coherencia de firma, pero no sustituye una prueba de actualización real con datos.

## Aceptación H1.1 pendiente

No se ha firmado ni instalado una APK con la clave del usuario. Deben ejecutarse Actions y una actualización entre DOS builds con la misma clave y códigos crecientes, comprobando tareas/perfil y base intactos. Revisa primero si el certificado de H1 coincide; no desinstales para realizar esa prueba. Hasta tu aceptación, H2 no se implementa.

Fuentes: [firma Android](https://developer.android.com/studio/publish/app-signing), [apksigner](https://developer.android.com/tools/apksigner). No se contempla Play App Signing en esta entrega de APK directa.
