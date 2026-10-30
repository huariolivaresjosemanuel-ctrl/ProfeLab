# ProfeLab — actualización incremental Taller V1.0.1

Esta entrega contiene fuentes Android reales para el proyecto existente. No es una APK ni sustituye todo el repositorio. La implementación está pendiente de compilación Kotlin/Android y de aceptación en teléfono. Lea REPORTE_QA.md antes de instalar una actualización.

Base exacta auditada y consultada otra vez antes de preparar la entrega:
- Repositorio: https://github.com/huariolivaresjosemanuel-ctrl/ProfeLab
- Rama: main; commit `81efabe5e12e0427eaeceedd68bba11b514a1e3b`.
- Árbol Git: `a5ca174b950cd1809f35b838282c80203975adc0`.
- Base: versionName 1.0.0, Room 2, 16 módulos, applicationId `pe.profelab.app`.
- Resultado: versionName 1.0.1; mismo applicationId, Room y módulos. Código debug predeterminado: 4; release conserva la fórmula existente `1000 + GITHUB_RUN_NUMBER`.

## Aplicación manual sobre su repositorio

1. Descargue y extraiga este ZIP en una carpeta independiente. Conserve una copia del repositorio actual y un respaldo desde Perfil de la aplicación instalada.
2. Compruebe que los archivos de destino corresponden a la base indicada. Si main avanzó, compare antes de reemplazar: INVENTARIO.csv contiene SHA-1 Git de la base y SHA-256 de los archivos entregados. No borre cambios propios posteriores.
3. Copie **el contenido interior** de `01_IMPLEMENTAR/` a la raíz del repositorio local, manteniendo las rutas `app/`, `core/` y `feature/`.
4. Copie **el contenido interior** de `02_REEMPLAZAR/` a esa misma raíz y acepte reemplazar solamente las rutas inventariadas.
5. Elimine únicamente `feature/taller/src/main/kotlin/pe/profelab/feature/taller/WorkshopData.kt`, según ARCHIVOS_A_ELIMINAR.csv. El editor antiguo de texto con barras fue sustituido por formularios tipados; dejarlo provocaría referencias obsoletas. No elimine datos de la aplicación ni otros archivos del repositorio.
6. Conserve `03_INSTRUCCIONES/` junto a su copia de la actualización. Sus documentos y evidencias no son dependencias de Gradle; puede archivarlos fuera del repositorio.
7. Revise el diff local contra INVENTARIO.csv: las tres carpetas de entrega **no** deben convertirse en carpetas envolventes del código dentro de GitHub.
8. Suba manualmente los archivos nuevos/modificados a sus rutas originales en GitHub y registre manualmente la eliminación indicada. Si usa la web, entre en la carpeta destino antes de subir cada grupo; el botón «Upload files» no reproduce por sí solo una estructura local aplanada. Realice usted el commit cuando termine de revisar los cambios.

Codex solo ha leído GitHub. No ha hecho commits, push, PR, cambios de rama remotos ni iniciado Actions. El desarrollo está aislado en `ProfeLab_Taller_V1_0_1/`; no se modificó Control Personal DNI ni los proyectos anteriores.

## Compilación manual en GitHub Actions

Conserve `.github/workflows/android.yml`: es idéntico al de main y tiene exclusivamente `workflow_dispatch`. No se incluye otra copia en el parche.

1. Después de su commit, abra **Actions → ProfeLab Android → Run workflow**.
2. Seleccione la rama que contiene el nuevo commit y active `run_emulator: true`.
3. Para una primera comprobación puede dejar `signed_release: false`. El workflow compila, ejecuta tests JVM y lint, genera los APK de pruebas y ejecuta instrumentación API 26 y 36. Revise que el SHA mostrado en la ejecución corresponde al commit nuevo; «Re-run jobs» de una ejecución vieja vuelve a compilar el commit viejo.
4. Para actualizar la aplicación distribuida, ejecute usted desde **main** con `signed_release: true` y emulador activado. Mantenga sus secretos y variables de firma existentes. No genere otra clave, no cambie su pin de certificado y no incluya secretos en archivos.
5. El auxiliar de firma ahora exige versionName 1.0.1. La fórmula de versionCode, comprobación de certificado y limpieza de clave temporal siguen iguales. Compruebe que el código nuevo supera `PROFELAB_LAST_DISTRIBUTED_VERSION_CODE` y el instalado. Si no lo supera, resuelva el contador siguiendo la política de firma existente antes de distribuir; no desinstale para eludir el problema.
6. Descargue el artefacto de firma permanente solo cuando todos los trabajos pasen. Los nombres históricos de artefacto aún contienen «V1.0»; verifique la información del APK y el informe de firma, donde la versión esperada es 1.0.1.
7. Instale sobre la aplicación anterior con la misma firma e identidad, sin borrar datos. Una APK debug de otro runner puede tener otra firma y no sirve como actualización permanente.

Los informes, inventarios y hashes antiguos que ya existían en la raíz del repositorio describen la entrega V1.0. Para **este parche** rigen SHA256SUMS.txt, INVENTARIO.csv y REPORTE_QA.md adjuntos. No se atribuyen los resultados históricos a este código nuevo.

## Verificación de integridad

SHA256SUMS.txt enumera cada archivo del paquete, salvo el propio listado. Sus rutas parten de la carpeta `PROFELAB_ACTUALIZACION_TALLER_V1_0_1/`. Compare el SHA-256 de un archivo con `Get-FileHash -Algorithm SHA256 -LiteralPath <ruta>`. El inventario distingue IMPLEMENTAR, REEMPLAZAR, ELIMINAR y SIN CAMBIOS; la matriz funcional identifica PENDIENTE cuando corresponde. Los archivos SIN CAMBIOS no están duplicados en el ZIP.

## Recuento de archivos

22 IMPLEMENTAR, 15 REEMPLAZAR, 1 ELIMINAR y 286 SIN CAMBIOS sobre 302 archivos base. Resultado del proyecto aplicado: 323 archivos. INVENTARIO.csv enumera todas las rutas del repositorio; los documentos de instrucciones y evidencias de la entrega se verifican mediante SHA256SUMS.txt y no requieren subirse para compilar.
