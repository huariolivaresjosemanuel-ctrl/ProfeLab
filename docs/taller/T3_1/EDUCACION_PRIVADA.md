# Aulas y evaluación privada — contrato implementado

La subsección Mis alumnos vive en Cuaderno; no añade una sexta pestaña. `feature:students` depende de core:model/content/ui y se inyecta desde app en Taller, sin dependencias entre módulos feature. No crea alumnos, aulas ni evaluaciones de ejemplo. La selección de participantes es opcional: Taller funciona con un número y sin nombres.

## Persistencia

EducationRepository utiliza `WorkspaceStore.editRecords` para cada operación atómica en la tabla existente workspace_records de Room3. Los tipos privados son `edu_classroom`, `edu_student`, `edu_team`, `edu_run`, `edu_observation` y `edu_phrase`, todos con schemaVersion1 y UUID estable. No hay tablas nuevas ni SQL de migración: la base anterior y sus registros conservan sus identidades. Los respaldos privados incluyen estos registros; el validador de restauración comprueba sus relaciones antes de fusionarlos.

Se solicitan únicamente nombre visible y aula del estudiante. No hay DNI, domicilio, teléfono, contactos, analítica, sincronización remota ni inserción en catálogos públicos. Las operaciones edu_* no crean eventos de actividad con nombres de menores. El nombre visible debe distinguir registros del aula; si dos estudiantes comparten nombre se puede usar una inicial o alias docente.

Aulas, estudiantes, equipos y ejecuciones pueden archivarse sin borrar registros históricos. La pertenencia del estudiante a su aula es estable en esta versión: editar su nombre no cambia su ID ni traslada sus evaluaciones. No se ofrece una transferencia automática entre aulas; requiere un contrato posterior de historial de matrícula. Se admiten grados1–6 y ejecuciones de1–100 participantes.

## Ejecuciones y observaciones

Una definición editorial, su copia personal y una ejecución docente son registros distintos. Crear una ejecución reutiliza las mismas imágenes y texto, guarda actividad/versión y refiere personalProjectId. Conserva snapshots de nombres de participantes, etiqueta de aula, integrantes de equipos y stepIds. Cambiar posteriormente un equipo no reinterpreta sus ejecuciones anteriores. Seleccionar participantes de otra aula o equipos archivados se rechaza dentro de la transacción.

Cada ejecución nueva comienza con stepProgress vacío y lastVisitedStepId vacío. EducationProgress proyecta y guarda completado, notas de paso y última visita en el edu_run seleccionado, sin trasladar los avances de un aula a otra ni duplicar fotografías. Al preparar individualmente se conserva el progreso anterior de la copia personal. Visitar y completar siguen siendo acciones distintas. Inventario y cantidades de preparación permanecen asociados a esa copia personal y requieren sus acciones explícitas de confirmación; registrar progreso docente no crea consumos.

El puntero de la copia personal es teacherPreparation.educationRunId. Cambiar a participación INDIVIDUAL lo conserva como historial pero no activa esa ejecución para observar alumnos. Elegir o crear una ejecución de alumnado es una acción explícita. La cantidad se ajusta al máximo entre el número preparado y las identidades seleccionadas; el resto puede representar participantes sin nombre. No crea identidades ficticias, no multiplica materiales y no mueve inventario. teamCount se actualiza al número de equipos de esa ejecución o1 como base cuando no hay equipos seleccionados.

Cada observación refiere ejecución, aula, objetivo STUDENT/TEAM/ACTIVITY, criterio con ID/etiqueta, estado, nota, paso opcional, fecha y evidencias. Los cuatro estados son autonomía, con apoyo, en proceso y no observado; no se convierten en AD/A/B/C ni otras notas. Un registro de equipo no se copia a los integrantes. Las notas, registros de actividad y datos de inventario existentes no se reutilizan como calificaciones. La fecha original se conserva al editar; el criterio mantiene su etiqueta histórica hasta que el docente elige explícitamente otro.

Los adjuntos permitidos en una observación nueva son fotografías privadas de esa copia, no medios editoriales. evidenceOwnerProjectId conserva el propietario. Quitar después el vínculo de una foto en la ficha no borra su vínculo histórico de evaluación: editar esa evaluación puede mantener o quitar las evidencias previamente autorizadas, pero no añadir archivos de otro proyecto. El respaldo incluye sus archivos mediante evidenceIds y valida el propietario de la relación.

## Retroalimentación y compartir

EducationFeedback genera texto únicamente a partir de observaciones guardadas de la ejecución, aula y objetivo seleccionados. Distingue expresamente los equipos del desempeño individual. No diagnostica, acredita competencias oficiales ni infiere hechos no registrados. Para mantener el resumen breve se muestran hasta30 registros recientes y extractos marcados cuando una nota o criterio es largo; el historial completo permanece guardado.

El docente puede editar el texto. Copiar o compartir requiere marcar la confirmación de revisión y pulsar la acción correspondiente; editar otra vez invalida esa confirmación. Compartir abre el selector de aplicaciones de Android, donde WhatsApp puede aparecer si está instalado. No se leen contactos ni se envían mensajes automáticamente.

## APIs de integración

- `StudentsWorkspace(all, store, back)`: administración privada dentro de Cuaderno.
- `EducationParticipationPanel(record, all, store)`: selección opcional de aula, estudiantes, equipos o solo cantidad en Materiales.
- `EducationObservationPanel(record, all, store, stepId, dismiss)`: panel contextual reutilizable en procedimiento, resultados o guía, sin crear otra ventana principal de Taller.
- `EducationData`: proyecciones tipadas de filas privadas y selección de ejecución compatible con la copia.
- `EducationRepository`: altas/edición, archivo, creación/selección de ejecuciones y observaciones con validación transaccional.

## Validación y límites

Se comprobaron delimitadores de las fuentes y se revisaron las relaciones y la separación de datos; eso no compila Kotlin. Las pruebas instrumentadas de aislamiento, snapshots, archivado, respaldo y restauración se entregan para API26/API36. No se ejecutaron Android, APK, dispositivo, compartición real ni actualización instalada en este entorno; revisar el informe QA consolidado para las pruebas locales efectivamente ejecutadas. La actualización del esquema lógico de registros es aditiva y no equivale a una migración SQL ejecutada.
