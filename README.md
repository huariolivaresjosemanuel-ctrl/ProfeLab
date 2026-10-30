# ProfeLab · H0 + fuentes H1

Aplicación Android independiente para estudiantes y docentes de Educación Primaria en Perú.

**Entrega de código real `0.1.0-h1`, applicationId `pe.profelab.app`. No incluye APK compilada ni aprobación de instalación.** La compilación y las pruebas de ejecución están preparadas para GitHub Actions; no se ejecutó Android/Gradle en la PC del usuario ni se modificó GitHub.

## Abrir y compilar

1. Descomprimir esta entrega. La carpeta `ProfeLab/` es la raíz del proyecto Android.
2. Seguir [SUBIDA_MANUAL_GITHUB.md](docs/SUBIDA_MANUAL_GITHUB.md). Subir **el contenido** de `ProfeLab/` a la raíz de un repositorio nuevo e independiente, incluida `.github/`.
3. En GitHub → Actions → **ProfeLab Android** → **Run workflow**. La opción de pruebas en emulador está activada por defecto para la ejecución manual.
4. Si el job `build` termina correctamente, descargar el artefacto `ProfeLab-H1-debug-<número>`; dentro estará `app-debug.apk`.
5. Revisar también `informes-H1-*` y `android-api-*`. Una APK generada no demuestra por sí sola que todas las pruebas Android hayan pasado.

## Implementado en las fuentes

- 16 módulos Gradle: `app`, seis `core:*`, nueve `feature:*`. Kotlin/Compose/Room/DataStore, interfaces y DI por constructores.
- Cinco pestañas en orden aprobado, navegación atrás, estado de navegación y formularios. Tema crema/verde, tarjetas pastel, logo y planta vectoriales originales.
- Inicio: saludo local, ciclo universitario, progreso real de tareas de lunes a domingo, entregas, accesos, actividad persistida y pendientes de hoy.
- Crear, editar, completar/reabrir, deshacer completado y eliminar tareas con confirmación; fecha/hora, prioridad, notas, búsqueda y filtros. Borradores Room al editar y al salir.
- Perfil opcional con nombre, ciclo e institución; preferencia local para ver completadas. Nunca confunde ciclo académico con grado escolar.
- Avisos locales opcionales con WorkManager, permiso contextual, cancelación y control de entrega por tarea/fecha. Android puede retrasar avisos; no son alarmas exactas.
- Biblioteca: consulta y búsqueda offline de las 12 referencias entregadas; ficha y apertura explícita de fuente oficial en navegador. **Cero PDFs oficiales incluidos.**
- Contratos futuros de documentos, anotaciones, archivos, OCR, captura, proyectos, inventario, evidencias, packs y respaldo. Las capacidades no implementadas no se registran como servicios disponibles.
- Workflow de APK debug, lint, pruebas JVM y pruebas Android en emuladores API 26 y 36.

## Pendiente

Taller completo y packs/inventario (H2), Sesiones (H3), Cuaderno/portafolio/audio (H4), importación y lector PDF anotable (H5), cámara/OCR (H6), integración, backup/migraciones y validación física/tablet/accesibilidad (H7). Las pestañas pendientes muestran un estado explícito, sin editores falsos ni botones inertes.

**No dar H1 por aceptado como app instalable hasta obtener la compilación y evidencia de pruebas.** `minSdk=26` es el mínimo de esta base; compatibilidad efectiva con teléfono y Lenovo Tab Plus pendiente. El tema claro es el aprobado; modo oscuro y avatar fotográfico no están habilitados.

## Documentación y evidencia

- [Auditoría H0](docs/H0_AUDITORIA.md): conflictos, viabilidad, riesgos y decisiones.
- [Arquitectura y contratos](docs/ARQUITECTURA.md).
- [Matriz de cobertura de las 46 vistas](docs/COBERTURA_UI.csv).
- [Informe real de pruebas](docs/INFORME_PRUEBAS.md).
- [Dependencias y fuentes](docs/DEPENDENCIAS.md).
- [Inventario](INVENTARIO_ENTREGA.csv): todas las rutas de esta entrega son nuevas respecto al workspace inicial.
- `SHA256SUMS.txt`: hashes de entrega, excluyendo el propio listado.

`reference/` conserva documentación, mockups, datos y PDFs de prueba del paquete fuera de los sourceSets de la APK. No contiene fuentes copiadas de ScanPro o DocReader. El ZIP original permanece intacto fuera del proyecto.

No hay claves de firma release. La clave debug que utiliza el runner sirve para pruebas y puede cambiar entre ejecuciones; conservar datos durante actualizaciones entre builds exige configurar una clave de prueba estable de propiedad del usuario. **No desinstalar para actualizar si necesitas conservar datos.** El respaldo manual todavía no está implementado.
