# ProfeLab · fuentes integradas V1.0

Proyecto Android completo en esta raíz: `app/`, `core/`, `feature/`, wrapper Gradle, recursos, pruebas y workflow manual. Una APK `pe.profelab.app`, API26+, Kotlin/Compose, Room v2, DataStore. Versión de fuentes 1.0.0; no incluye APK compilada.

Se amplió la copia de `main` del repositorio ProfeLab, commit `4ec7a104e9345fd1aa1a0c74eddbe72169304b80`, árbol `ce2d9fbd0999bd0947d4e0e9ba35d2286d89544c`. No se escribió en GitHub ni se modificaron otros proyectos. La entrega es integral de fuentes, con funcionalidad real en las cinco secciones; **no equivale a una V1.0 validada para distribución**. No se pudo compilar Android en este entorno. Las diferencias y funciones parciales se detallan en [PENDIENTES_REALES.md](PENDIENTES_REALES.md).

Empieza por [LEER_PRIMERO_SUBIDA_MANUAL.md](LEER_PRIMERO_SUBIDA_MANUAL.md). El contenido del ZIP va directamente en la raíz del repositorio existente. Actions funciona exclusivamente con **Run workflow**; subir archivos no compila nada.

## Funcionalidad escrita e integrada

- Inicio existente: perfil/ciclo universitario, tareas/borradores/avisos opcionales, progreso calculado, actividad y navegación por IDs.
- Taller: 12 proyectos y miniaturas originales + ocho experimentos pendientes de revisión, filtros/favoritos, copias personales, editores de pasos/checklist, observaciones, modos práctico/académico, inventario por cantidades/unidades/reservas y compras consolidadas.
- Sesiones: Primaria 1–6, tres estructuras temporales offline, planificación y evaluación, cotejo curricular declarado por el usuario, recuperación/versiones, recursos vinculados y PDF personal. No incorpora textos oficiales sin cotejo.
- Cuaderno: Apuntes/Prácticas/Ideas, búsqueda, adjuntos, reflexión, voz local con permiso, portafolio seleccionado y exportación con revisión de privacidad.
- Biblioteca: las 12 referencias del paquete, fuentes/derechos/estado local, importación SAF y recepción de PDFs externos con elección temporal/privada. No se distribuyen PDFs oficiales.
- PDF: render nativo, páginas/miniaturas/zoom, búsqueda de texto, selección a partir de posiciones de caracteres, lápiz/resaltado/subrayado/notas, Room por documento/hash/página, marcadores y copia aplanada. Comentarios íntegros en anexo imprimible. Adaptación parcial de DocReader MIT, atribuida.
- Cámara/OCR: CameraX, enfoque y linterna condicionada a hardware, selección SAF, recorte de márgenes/rotación, ML Kit Latin bundled, edición y confirmación del texto, evidencias y lotes de hasta diez páginas con borrador local.
- Integración: archivos privados UUID, enlaces estables, packs validados, respaldo ZIP con vista previa/fusión controlada, migración explícita Room1→2 y configuración de firma permanente del usuario.

## Verificación

Los 16 controles estáticos y las 11 pruebas Python del helper de firma pasan localmente. El intento manual17 del usuario ejecutó19 pruebas JVM de core:model/core:security, todas aprobadas, pero falló la compilación de core:files por PdfDocument().use. Ese error está corregido en las fuentes; la compilación de la corrección aún NOT_RUN. Instrumentadas, lint completo, dispositivos, fidelidad visual y actualización firmada siguen pendientes. Lee [LEER_PRIMERO_CORRECCION_CI.md](LEER_PRIMERO_CORRECCION_CI.md) antes de volver a subir. Consulta [REPORTE_QA_REAL.md](REPORTE_QA_REAL.md), [MATRIZ_COBERTURA_REQUISITOS.csv](MATRIZ_COBERTURA_REQUISITOS.csv) y [INFORME_FIDELIDAD_UI.md](INFORME_FIDELIDAD_UI.md).

Las instrucciones H0/H1/H1.1 conservadas son históricas; la documentación raíz de esta entrega y `docs/completo/` describen el estado actual. Ningún PASS histórico acredita el código nuevo. `reference/` conserva los cinco mockups y documentos aportados, fuera de la APK.
