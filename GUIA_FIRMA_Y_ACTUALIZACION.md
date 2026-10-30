# Firma permanente y actualización · V1.0

Se conserva `pe.profelab.app`. Mantener el paquete no basta: Android necesita firma compatible y versionCode creciente. **No se generó ninguna clave, no se firmó APK y no se verificó una actualización real en esta entrega.**

## Instalación H1 existente

Antecedente público H1: APK del run41002583890, SHA256 `fac34f13fd8ed6a0ab6b3ab349ddf30f3450b604441b36dab5514908472fac50`; certificado declarado en bloque APKv2 `ed49929e7923b1e1ba8bc4df03429255f39dc578dd7d7468df3ba93f566fa0a5`. Evidencia histórica en `docs/qa/h1.1/certificado-h1-publico.json`. No demuestra que esa sea tu APK instalada ni sustituye apksigner. Una huella pública no contiene la clave privada.

Si tienes herramientas ya disponibles, verifica la APK instalada/original con `apksigner verify --verbose --print-certs H1.apk`, y compara con `keytool -list -v -keystore RUTA_PRIVADA -alias ALIAS`. Solo si posees la misma clave privada se puede evaluar actualización directa; debe probarse con datos ficticios. No pegues contraseñas ni Base64 en el chat.

Si tu nueva clave es distinta, la actualización será incompatible normalmente. **No desinstales ni borres datos como solución.** H1 no tiene exportador completo; el backup V1.0 no puede ejecutarse antes de conseguir instalar una actualización compatible. Conserva H1, busca su keystore original y prueba V1.0 en otro dispositivo. La extracción técnica del sandbox debug con herramientas preexistentes no se ha validado aquí ni se promete como migración segura.

## Crear o reutilizar TU clave

Prioriza la clave original compatible si la posees. Si eliges una nueva identidad permanente, hazlo en equipo confiable con JDK disponible, fuera del repositorio y de carpetas compartidas; el asistente no ejecuta estos comandos:

```text
keytool -genkeypair -v -storetype PKCS12 -keystore RUTA_PRIVADA/profelab-release.p12 -alias profelab -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore RUTA_PRIVADA/profelab-release.p12 -alias profelab
```

Contraseñas interactivas; para PKCS12 conserva la contraseña del almacén también como clave. Guarda dos copias protegidas y contraseñas en un gestor independiente. No crear claves por build ni mezclar las de otras apps.

En PowerShell, con tu ruta privada real, Base64 directo al portapapeles sin imprimir:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('RUTA_PRIVADA\profelab-release.p12')) | Set-Clipboard
```

Pégalo solo en el secreto correspondiente y limpia después con `Set-Clipboard -Value ''`. Base64 no cifra; revisa el historial/sincronización de portapapeles.

## Configuración manual de GitHub

Settings → Secrets and variables → Actions → Secrets:

| Secreto | Valor |
|---|---|
| `PROFELAB_KEYSTORE_BASE64` |Una línea Base64 de tu keystore |
| `PROFELAB_KEYSTORE_PASSWORD` |Contraseña del almacén |
| `PROFELAB_KEY_ALIAS` |Alias exacto |
| `PROFELAB_KEY_PASSWORD` |Contraseña de la clave |

En Variables, públicas:

- `PROFELAB_SIGNING_CERT_SHA256`: huella de TU certificado,64hex (separadores `:` aceptados). No copiar la de H1 si tu clave difiere.
- `PROFELAB_LAST_DISTRIBUTED_VERSION_CODE`: mayor código que ya distribuiste; usa1 solo si efectivamente H1 code1 es el máximo. Tras distribuir, actualiza con el código de `firma-publica.json`.

VersionName1.0.0. Debug code3 por defecto, solo pruebas. Release utiliza `1000 + github.run_number` y exige superar el máximo registrado. Un rerun mantiene el código: para una nueva actualización distribuible usa NUEVO Run workflow. No reiniciar contadores sin revisar el máximo.

Actions → ProfeLab Android → Run workflow en `main`, `run_emulator=true` y `signed_release=true`. Es el único disparador. Release exige build y tests API26/36 exitosos. El keystore se reconstruye en RUNNER_TEMP fuera del checkout; job firmado sin caché ni configuration cache; cleanup always. Si se pierde abruptamente el runner, se depende de su destrucción efímera.

Antes de publicar, `scripts/signing.py` usa apksigner y aapt reales en runner para comprobar firma v2, certificado fijado, paquete/código/nombre. Solo publica APK, hashes y `firma-publica.json`; no secretos. Si falla, no publica release. La suite Python local simula estas herramientas; no acredita ejecución de ellas.

## Prueba de actualización aún BLOCKED

En dispositivo de prueba: instalar releaseA firmada con tu clave, crear datos y backup con archivos, luego instalar releaseB misma firma/código mayor sin desinstalar. Revisar tareas/perfil/sesiones/notas/ideas/documentos y marcas, abrir/exportar PDFs y restaurar copia en otro entorno de prueba. Comprobar hashes/certificados. La migración Room1→2 tiene SQL aditivo, pero su verificación en Android sigue NOT_RUN. No distribuir basándose solo en este documento.

Fuentes: [firma Android](https://developer.android.com/studio/publish/app-signing), [apksigner](https://developer.android.com/tools/apksigner). No se configura Play App Signing en esta entrega.
