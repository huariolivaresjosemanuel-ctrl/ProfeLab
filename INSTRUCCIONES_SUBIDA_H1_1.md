> HISTÓRICO: describe H0/H1/H1.1. Para esta entrega prevalecen README.md, REPORTE_QA_REAL.md, GUIA_FIRMA_Y_ACTUALIZACION.md y PENDIENTES_REALES.md de la raíz. Las antiguas pausas por hitos ya no aplican.

# Subida y prueba H1.1

Base `5891cd9257eb966dbce319b4b55f5dc86436fe9d` en main. El ZIP contiene rutas desde la raíz, sin carpeta contenedora. Extrae y sube manualmente solo los archivos del inventario; reemplaza los MODIFICADOS y añade los NUEVOS. Si hubo cambios posteriores en esas rutas, compara antes de reemplazar. No elimines módulos ni datos. Incluye `.github/workflows/android.yml` y `.gitignore` aunque Windows los oculte. El asistente no modificó GitHub.

Lee primero `docs/H1_1_FIRMA_Y_TRANSICION.md`, especialmente la compatibilidad con la instalación H1. Crea o reutiliza la clave bajo tu control; configura los cuatro secretos y las DOS variables públicas indicadas. No envíes contraseñas ni keystore al chat ni al repo. No es necesario compartirlos con el asistente.

Ejecuta Actions sobre main: run_emulator=true y signed_release=true. Si solo deseas verificar fuentes sin secretos, signed_release=false. Para release ambos emuladores deben pasar. Descarga el artefacto FIRMA-PERMANENTE y conserva APK, SHA256SUMS y firma-publica.json. Revisa la huella esperada y versionCode, y actualiza manualmente PROFELAB_LAST_DISTRIBUTED_VERSION_CODE después de distribuir. Un nuevo Run incrementa el código; rerun del mismo Run no es una nueva actualización.

Si faltan secretos o no coincide el certificado, el release falla; no reemplaces el pin para ocultar una diferencia sin comprobar qué clave estás usando. La clave debug no garantiza actualización desde H1. Si son distintas, prueba en otro dispositivo y protege los datos existentes; H1 aún no ofrece respaldo/restauración completo. No se recomienda desinstalar.

H2 solo puede comenzar después de probar y aceptar H1.1. Su preparación queda en docs/H2_PREPARACION.md. No hay nuevas pantallas ni capturas de la aplicación en esta entrega.
