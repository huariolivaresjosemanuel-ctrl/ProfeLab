> HISTÓRICO: describe H0/H1/H1.1. Para esta entrega prevalecen README.md, REPORTE_QA_REAL.md, GUIA_FIRMA_Y_ACTUALIZACION.md y PENDIENTES_REALES.md de la raíz. Las antiguas pausas por hitos ya no aplican.

# Hallazgos H1.1

La base H1 ya compila: no se atribuye un error de compilación nuevo a ese run. Se corrige la ausencia de una configuración permanente de release, la falta de verificación de certificado antes de distribuir y el versionado fijo para futuras actualizaciones. Se diferencia debug de release, se exige pin público de certificado y se reconstruye/limpia el archivo privado solo en el runner temporal. Release sin configuración falla; no hay fallback a debug ni clave nueva por ejecución.

No se modifica arquitectura, UI, dependencias, datos, modelos, catálogo ni esquema. La imposibilidad de garantizar actualización con una clave diferente se documenta como bloqueo de transición, no se oculta cambiando applicationId. Los requisitos de H2–H7 permanecen aprobados y pendientes; no se declaran terminados.
