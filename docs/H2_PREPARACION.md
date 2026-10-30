# Preparación H2, sin implementación antes de aceptar H1.1

Se conservan las 12 plantillas V2, sus 12 IDs y miniaturas, las 8 propuestas de experimentos y las 12 referencias MINEDU. `reference/` y los cinco mockups permanecen idénticos a la base. No se añade ninguna pestaña ni se toca el módulo Taller en H1.1.

Orden preparado para la próxima etapa: entidades Room con migración 1→2 no destructiva; catálogo inmutable y copias personales con nuevo UUID; importador de packs con checksum/versiones/staging/rollback; inventario decimal/unidades/herramientas y faltantes; guías con checklist; experimentos con resultados del usuario; editor de ideas; observación/rúbricas; exportación y adjuntos revisables. Cada capacidad debe quedar ligada a las filas UI-006..019 y sus QA de la matriz maestra.

Adaptadores necesarios ya detectados: V1 `documents/officialUrl` frente a V2 `docs/official_page`; safety.reviewStatus frente a reviewStatus en raíz de experimentos; cantidades de experimentos con unidad `referencial: ajustar` no acreditan stock ni disponibilidad. El manifiesto de ejemplo tiene SHA pendiente y no es un pack instalable. Los estados de seguridad/pedagogía pendientes no se convierten en actividades verificadas. No se completan competencias oficiales desde textos no cotejados.

UI requerida: `02_TALLER.png`, encabezado Mi taller, buscador/chips, destacado con miniatura original, inventario, preparación y proyectos. Materiales/Experimentos/Mis ideas son secciones internas. La implementación debe producir capturas Android auténticas y comparar composición, fuente grande y tablet; no se aprueba fidelidad a partir de código.

Contrato de aceptación: tests de cantidades/unidades incompatibles/herramienta faltante, seguridad, idempotencia de packs y preservación de copias, migración con datos H1, borradores tras cierre, selección de ambos modos sin perder checklist y exportación con autoría. Requiere aceptación previa de H1.1; H3–H7 conservan íntegro su alcance aprobado.
