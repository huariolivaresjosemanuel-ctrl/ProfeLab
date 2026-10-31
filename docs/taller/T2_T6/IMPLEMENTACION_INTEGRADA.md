# Taller 2.0 — implementación integrada T2–T6

Base exacta: main `77e3d37da6cd0435875d026b0f24857b1b74a133`, 425 archivos verificados contra sus blobs Git mediante lectura. Esta entrega continúa T1 ya incorporado. No crea otra aplicación ni modifica GitHub.

Estado: recorrido implementado en código; aceptación Android y visual PENDIENTE. No se declara terminado el encargo. No se instalaron Java, Gradle, SDK o Node. No se ejecutaron Actions. Los documentos T0/T1 se conservan como antecedentes históricos.

## Código y comportamiento por fase

| Fase | Implementación |
|---|---|
| T2 | WorkshopHub, SectorCatalog y ActivityDetail: portal, categorías activas/futuras, recientes reales, inventario/compras, búsqueda, área, favoritos, ficha con portada y tres perspectivas. |
| T3 | MaterialsChecklist: cantidades necesarias, físicas, reservadas y disponibles en unidades comparables; preparación personal separada del stock; lista de compras, clipboard y ACTION_SEND text/plain; panel de reservas/consumos/reversión T1 reutilizado. |
| T4 | PurposePanel y GuidedStepPager: propósito progresivo por perspectiva, fuentes, adaptaciones, guía/lista, paso por ID estable, anterior/siguiente, notas y progreso persistente. Experimentos conservan hipótesis/variables/seguimiento/resultados propios. |
| T5 | TeacherClassroomPanel: guion breve expandible, inicio/desarrollo/cierre, preguntas, inclusión/seguridad, criterios y reflexión docente; contrato ActivityRunReference/FutureClassroomAssociation sin alumnos ni notas automáticas. |
| T6 | Integración de cola, navegación, recuperación, export y editor; pruebas adaptadas y 14 casos Android nuevos; captura de las seis ventanas y A1 candidata preparada. Compilación/capturas/revisión de dispositivo no ejecutadas aquí. |

Los componentes son Compose nativos. No leen los PNG de mockups como pantalla ni recortan sus elementos para simular controles. Se reutilizan paleta crema/verde, tarjetas suaves, imagen principal, jerarquía y orden de las seis referencias. La aplicación conserva encabezado, cinco pestañas y Concepto C canónica. No se generan personajes ni ilustraciones nuevas.

## Persistencia y contratos

WorkRecord continúa siendo el almacenamiento del proyecto. No cambia Room3, sus tablas, migraciones, firma, applicationId ni configuración de versión. `workshopFlow.version=1` es una extensión JSON opcional de la copia personal:

- lastVisitedStepId: ubicación visitada; no significa que el paso esté hecho.
- perspective: PRACTICAL/TEACHER/ACADEMIC, independiente del progreso.
- preparedMaterialIds: checklist personal; no acredita existencias ni consume.
- observedCriterionIds y criteriaById: observaciones explícitas de la actividad y snapshots de sus criterios. Los criterios anteriores no desaparecen cuando cambia un texto del guion.
- teacherReflection: reflexión docente local, sin estudiantes o calificaciones.

`steps.done`, los IDs, notas, registros, guion y evidencias previos conservan su significado. WorkshopFlowAdapter ofrece una proyección de lectura que admite pasos antiguos sin ID y pasos de texto. Consultar una plantilla no genera una copia, un run ni movimientos. Comenzar crea explícitamente otra copia; se impide otra creación mientras la cola de guardado está pendiente. Duplicar conserva la función del editor y reinicia el progreso de la copia nueva.

El último paso y las notas se guardan mediante el ViewModel/Room existentes. La navegación mantiene una pila recuperable con un máximo de 100 entradas y estado por ventana. Volver espera la cola; si falla, mantiene el borrador y ofrece reintento. El enlace a una actividad siempre presenta primero la ficha, que ofrece Continuar hacia el último paso o Materiales si aún no hay avance.

Los nuevos contratos referencian activityId, activityRunId, contentVersion, criterionIds y, para el futuro, groupId/studentId/competencyRef/evidenceIds opcionales. No instancian asociaciones privadas, no añaden tablas ni crean una sexta pestaña. Actualmente activityRunId identifica la copia personal iniciada; una futura ocasión docente requerirá su propio contexto de aplicación y privacidad antes de guardar asociaciones individuales.

## Inventario y compras

Se conserva sin cambios el repositorio transaccional T1, las tres entidades, su migración y BackupTransfer. Todo consumo pasa por su comando durable y confirmación. Completar los pasos muestra una invitación a revisar uso real; nunca descuenta por navegación. Herramientas/reutilizables no se consumen. El panel T1 permite reservas por dueño, cancelación de pendientes, recuperación y reversión compensatoria controlada.

MaterialsChecklist muestra disponibilidad que incluye la reserva propia y protege reservas ajenas. Un tipo/saldo/unidad ambiguo muestra Revisar. La lista del proyecto no inventa faltantes cuando no constan existencias; usa «por revisar». La compra consolidada histórica mantiene la cantidad total requerida cuando no hay inventario, ahora rotulada expresamente como cantidad necesaria sin faltante verificado, preservando las funciones y pruebas de compras anteriores. Cuando existe stock compatible, se muestran los faltantes calculados. No se fusionan cantidades ambiguas ni se incorporan existencias al copiar o compartir.

El botón de compartir abre Android Sharesheet con text/plain. WhatsApp es una opción del sistema cuando está disponible; no se consulta el teléfono ni se necesitan contactos o servicios externos. Si no hay receptor, se ofrece Copiar. Las listas son seleccionables incluso si el clipboard falla. La lista completa existente también incorpora copiar/compartir.

## Escalabilidad y compatibilidad

Taller mantiene los 12 proyectos de base, incluido arte-001; Experimentos mantiene exp-001…008. Danza y Tradiciones se activan solo si ya hay contenido compatible instalado con categorías dance/heritage. Cocina, Arte y Tecnología quedan Próximamente; no se activa FOOD ni se supone que haya paquetes nuevos. Música/Teatro y demás categorías compatibles siguen accesibles dentro de Taller y sus filtros avanzados.

Se reutilizan importación, namespaces, versiones, hashes, créditos y vínculo de imágenes de paquetes por ID/paso. La extensión opcional activityContractVersion=1 valida criterios identificables y rechaza estado local/asociaciones privadas en definiciones públicas. Los contratos previos 2/2.1/3 siguen aceptados. La actualización de un catálogo no reemplaza las copias personales. La selección de medios y los layouts anteriores siguen accesibles en la guía completa.

El editor, guion editable, variantes/alternativas, galería, evidencias, ocho seguimientos experimentales, revisión personal de seguridad, archivo y PDF permanecen accesibles desde «Contenido completo, alternativas y evidencias». El portal conserva recomendaciones y filtros avanzados mediante el catálogo existente. Inventario, compras y recuperación desde Perfil permanecen disponibles.

## Imágenes y diferencias visuales no resueltas

Se inspeccionaron los seis PNG verticales originales. A1 no tiene aprobación individual. No se usó la infografía rechazada. Las ocho imágenes de 04_IMAGENES_REALISTAS_PROPUESTAS siguen sin aprobación humana según el manifiesto del paquete; no se añadieron a assets.

La ficha muestra imágenes propias/importadas asignadas a su registro o la ilustración conceptual existente, con etiqueta. Los experimentos sin medio específico muestran el aviso correspondiente. A5 usa únicamente imageFileIds del paso; no sustituye una imagen de procedimiento por una portada. Esto conserva la seguridad editorial, pero impide dar por cumplida la portada fotográfica/ilustración específica de todas las actividades.

Las tarjetas usan iconos vectoriales nativos en lugar de los objetos 3D de ejemplo de las maquetas, porque no hay activos individuales aprobados para ellos. No se recortaron del mockup. Los textos, cantidades y progreso se calculan con datos reales; A6 omite las fichas nominales ficticias y emplea únicamente el bloque Próximamente y la mascota canónica existente. No se muestra una competencia del mockup si no está verificada para el contenido.

La estructura visual está implementada; no se ha medido fidelidad de píxeles, 360dp/tablet, texto200%, TalkBack o rendimiento en Android. No se presenta como aprobación visual terminada.

## Evidencia y puerta de salida

Se consultó en SOLO LECTURA el run 41243795806 del HEAD base: build aprobado; API26 y API36 fallaron en InventoryMigrationTest por `No value for indices`. Se corrige solo el fixture con `optJSONArray("indices") ?: JSONArray()`, tal como el fixture histórico ya hacía. Se conservan SQL, apertura/validación completa por Room y todas las aserciones. No se silencia el test ni se cambia el resultado remoto. Su nueva ejecución sigue pendiente.

Ejecutadas en este entorno: 51 pruebas Python aprobadas y chequeo de delimitadores de 169 Kotlin/KTS. Se preservan las 74 declaraciones instrumentadas previas, se comprueba que 177 archivos de funciones/recursos ajenos al alcance siguen idénticos y se añaden 14 casos instrumentados: diez contratos/progreso, dos interacciones, una captura integrada y un roundtrip de respaldo. Resultado de esos 14 casos: NO_EJECUTADOS. La revisión léxica y las pruebas Python no compilan Kotlin ni ejecutan Compose/Room.

WorkshopFlowCaptureTest generará, únicamente al ejecutarse en Android, siete PNG reales en `app/build/reports/androidTests/visual/taller-2-0/`: w00_taller_portal, a1_catalogo_candidato, w01_ficha_visual, w02_materiales, w03_proposito, w04_pasos_guiados y w05_clase_evaluacion. El workflow manual existente ya ejecuta app/core:database y recoge esa carpeta. No se añadieron triggers ni se ejecutó el workflow.

Pendiente obligatorio: compilar el parche, pasar las suites pertinentes en API26/API36, revisar capturas una a una contra los originales, comprobar compartir con/sin WhatsApp, cierre real de proceso, accesibilidad, modo avión, actualización con la firma existente y respaldo con datos de prueba. Sin ello los criterios 11/12 y la aprobación visual permanecen bloqueados. No se entrega APK ni capturas inventadas.
