# Contrato futuro, sin módulo de alumnos

Esta documentación no crea interfaces runtime, entidades ni estudiantes. El contrato se implementará acotadamente en T5 dentro de core:model, no como un catálogo de personas.

activityId identifica contenido estable. Para contenido de pack debe acompañarse de sourcePackId/namespace para no colisionar con otro pack. projectRunId puede reutilizar el UUID de la copia personal de construcción; activityRunId de una aplicación docente es otra identidad, creada solo cuando el docente inicia esa ocasión, nunca al consultar. Conservar el snapshot de versión/guion utilizado.

criterionId debe ser estable y conservarse al editar el texto. Los criterios actuales formativeEvidence son cadenas; convertirlos requiere identificadores persistentes en la copia, no un hash del texto que cambie al corregirlo. evidenceId apunta al recurso privado añadido voluntariamente; no copiar archivos o datos de alumno a un pack. assessmentTemplateId/version diferencia la propuesta pedagógica de una observación real.

El futuro módulo privado será propietario de studentId, aula y evaluación; esos campos no se agregan al JSON editorial, pack, semilla, índice global ni pantalla de Taller. El placeholder PRIVATE_IDENTIFIER_FUTURE del ejemplo es documentación y no un registro. Sin AD/A/B/C automático, diagnósticos o resultados científicos inventados.

A6 podrá guardar reflexión y criterios de la actividad, sin evaluaciones por menor. Se conserva el diseño del bloque próximo, sustituyendo nombres/avatares ficticios por una explicación no interactiva. Reutilizar Concepto C original. Ninguna sexta pestaña ni cuentas/red/permisos de contactos.
