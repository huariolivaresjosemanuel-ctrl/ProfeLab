# Subida manual y compilación en GitHub

El asistente no realizó commits, push, merge, PR ni cambios remotos. Estos pasos los ejecuta el usuario. **Utiliza un repositorio nuevo para ProfeLab**; no uses Control Personal DNI ni otro proyecto.

## Archivos que deben quedar en la raíz

Al extraer el ZIP verás `ProfeLab/`. En GitHub deben aparecer **directamente**:

```text
.github/workflows/android.yml
app/
core/
feature/
gradle/
gradlew
gradlew.bat
build.gradle.kts
settings.gradle.kts
gradle.properties
scripts/
docs/
reference/
README.md
THIRD_PARTY_NOTICES.md
INVENTARIO_ENTREGA.csv
SHA256SUMS.txt
```

No subas únicamente el ZIP: GitHub Actions no lo descomprime. Tampoco pongas todo dentro de una carpeta `ProfeLab` remota, porque el workflow debe estar en `.github/workflows` en la raíz. El archivo Gradle Wrapper JAR sí es necesario; no es una APK ni una clave.

## Opción web, sin instalar Android Studio

1. Crea un repositorio independiente en tu cuenta, por ejemplo `ProfeLab`, preferiblemente privado durante las pruebas. La rama puede ser `main` o `master`.
2. En el Explorador de Windows activa **Elementos ocultos** para ver `.github`, `.gitignore` y `.gitattributes`.
3. En GitHub abre **Add file → Upload files**. Arrastra desde el interior de la carpeta local `ProfeLab` sus archivos/carpetas, conservando jerarquía. Confirma cada lote tú mismo. El sitio limita cantidad/tamaño por lote; si no admite todo, sube carpetas en lotes pequeños. No elimines rutas para sortear el límite.
4. Si el navegador no conserva `.github`, usa **Add file → Create new file**, escribe exactamente `.github/workflows/android.yml` y copia el contenido del archivo de esta entrega. No cambies su extensión a `.txt`.
5. Verifica las rutas del listado anterior y que `gradle/wrapper/gradle-wrapper.jar` exista. No subas `local.properties`, claves, carpetas `.gradle` ni `build`.
6. Los pushes manuales a `main/master` disparan `build`. Durante una subida en lotes pueden fallar ejecuciones por archivos faltantes; completa todos los lotes y ejecuta manualmente una compilación nueva.

Para muchos archivos, GitHub Desktop también permite agregar toda la carpeta de un repositorio independiente; cualquier commit/publicación sería una acción manual del usuario. Esta entrega no necesita permisos de escritura del asistente.

## Ejecutar el workflow

1. Abre **Actions** y habilita workflows si GitHub lo solicita.
2. Selecciona **ProfeLab Android → Run workflow** sobre la rama que contiene la entrega completa.
3. Mantén `run_emulator=true` para ejecutar también pruebas Android API26/API36 después del build. El build ejecuta comprobaciones estáticas, pruebas JVM, lint y ensamblado de APKs de app/test.
4. GitHub instala JDK/SDK **en su runner**; no en esta PC. Requiere acceso a Google Maven, Maven Central y Gradle, y cuota de Actions. El workflow solo solicita `contents: read` y no publica releases.
5. Espera los resultados de `build` y `android-tests`. Si `build` falla no habrá APK de entrega; abre el primer paso rojo y conserva el log. Si fallan pruebas de emulador, el APK puede existir pero la validación está pendiente.
6. Descarga `ProfeLab-H1-debug-<número>` desde **Artifacts**, extrae `app-debug.apk`. Descarga también informes y esquemas Room. Conserva el `1.json` generado bajo `core/database/schemas` antes de una futura modificación de esquema.

## Probar y actualizar

Instala primero en un dispositivo de prueba. Android puede pedir autorizar la instalación desde el gestor de archivos elegido. Sigue `INFORME_PRUEBAS.md`; registra modelo, versión Android, APK/run y evidencias. Cámara/OCR/PDF no están habilitados en esta entrega.

La firma debug estándar del runner puede cambiar entre builds. Para actualizaciones que conserven datos, antes de usarlas en producción configura una clave propia estable en secretos de Actions y una variante de firma controlada; no reutilices claves de otras apps. No desinstales una versión con trabajos importantes para resolver un conflicto de firma: la desinstalación borra los datos y H1 aún no ofrece backup manual.

Si aparecen avisos de permisos de Actions de tu organización, el administrador debe permitir los actions usados. No hace falta conceder permisos de escritura al workflow. El ZIP e inventario permiten revisar todos los cambios antes de subirlos.
