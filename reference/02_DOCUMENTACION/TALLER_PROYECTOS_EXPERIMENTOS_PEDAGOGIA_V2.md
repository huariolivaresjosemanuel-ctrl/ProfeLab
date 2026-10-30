# Taller V2: motor de proyectos, experimentos, ideas y fundamentación

## 1. Secciones independientes (no pestaña nueva)
- **Materiales didácticos**: construir un recurso para enseñar; objetivo, materiales/utensilios, pasos con checklist, seguridad, uso, variantes, fotos, reflexión.
- **Experimentos / indagación**: pregunta, hipótesis, variables, controles, preparación, observaciones con fecha, resultados, interpretación, limitaciones, informe y seguridad.
- **Mis ideas**: ficha desde cero, auto-guardado, título, imágenes propias, adjuntos, boceto, pasos, contenido pedagógico opcional, versión y duplicación; estado privado por defecto.

## 2. Recomendador tipo Mi Despensa
- El inventario tiene `materialId`, unidad de medida, cantidad y reservados. Unidad compatible (pieza, ml, g, hoja, etc.); conversiones solo si están definidas.
- Se calcula `cobertura = materialesDisponibles / materialesRequeridos`, **considerando cantidades**, herramientas, seguridad, grado y pertinencia; materiales alternativos con explicación.
- Clasificar `Puedo hacerlo`, `Me faltan materiales`, `Requiere preparación adulta`, `No recomendado` (por riesgo); no confundir presencia de objetos con seguridad.
- Generar lista de compras editable y agrupada por material, consolidando proyectos y descontando inventario sin duplicar.
- El inventario del usuario y las listas personales nunca se sobrescriben al instalar un pack.
- No usar IA generativa remota como componente necesario; búsqueda/recomendación determinista offline; contenido precurado con fuente.

## 3. Dos modos confirmados
- **Práctico**: para qué sirve, objetivo, tiempo, preparación, pasos, evidencia observable, adaptaciones y consejos breves.
- **Académico**: competencias sugeridas, capacidades y desempeños únicamente si cotejados contra fuente oficial concreta; fundamentación de estrategias, funciones cognitivas o socioemocionales como *posibilidades de práctica*, limitaciones y citas por afirmación.
- El cambio no cierra el proyecto ni destruye edición/borrador/checklist. Estado de modo por usuario y por proyecto opcional; sincronización local del mismo objeto.
- No diagnosticar ni asignar perfiles psicológicos a menores; solo describir conductas observables, sin etiquetas clínicas, cifras de mejora ficticias ni promesas.

## 4. Observación docente
- Antes/durante/después; instrumentos: lista de cotejo, rúbrica con criterios editables, registro anecdótico. Crear evidencias anónimas por defecto (nombres no obligatorios), fotos/audio solo cuando hay autorización y controles de exportación.
- Cada evidencia vincula proyecto, sesión opcional, fecha, indicadores y adjuntos; el formulario no debe impedir que el docente capture una observación de 20 segundos.
- Cuaderno/Portafolio se conectan con referencia, no copian o destruyen la evidencia original.

## 5. Inclusión y seguridad
- Adaptaciones según barreras observadas: texto, pictogramas, acceso motriz, tiempo, comunicación, materiales y trabajo colaborativo; no condicionar por diagnóstico.
- DUA: ofrecer opciones de presentación, compromiso y expresión. Material extraíble/imprimible; modo de alto contraste, letra escalable, objetivos claros.
- Preparación exclusiva de adultos para cortar, perforar, clavar o usar calor. No recomendar sustancias peligrosas, residuos contaminados, electricidad de red, combustión ni ingestión de experimentos.
- Cada pack trae `safety.reviewStatus`; proyecto sin revisión de seguridad **NO se presenta como actividad verificada**.

## 6. Contenido semilla incluido
- Doce materiales/ideas originales enriquecidos `PACK_TALLER_12_ENRIQUECIDO_PEDAGOGIA_V2.json` + sus 12 miniaturas conceptuales originales.
- Ocho experimentos originales de baja peligrosidad `PACK_EXPERIMENTOS_8_ORIGINALES_V1.json`, con preguntas, hipótesis, variables, controles y observaciones; **pendientes de validación pedagógica y de seguridad antes de publicación**.
- `PACK_TALLER_12_PROYECTOS_ORIGINALES.json` se conserva como base original para trazar cambios.
- Cada perfil académico sin cita individual validada se etiqueta `PROPUESTA_EDITORIAL`, no se presenta como verdad oficial del MINEDU.

## 7. Casos especiales
- Crear una idea propia con materiales sin cantidad: el usuario completa unidad y cantidad, no se simula disponibilidad.
- Hacer copia editable de una guía oficial: crea un objeto nuevo `forkedFromId`, conserva fuente, nunca sobreescribe el paquete original.
- Integrar con Sesiones: referencias `projectId`, `sessionId`, `sourceDocId`, `noteId`, sin joins por título ni duplicación destructiva.
- Pérdida de energía/cierre mientras se registra observación: borrador y adjuntos recuperables.
