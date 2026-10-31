# Decisiones, conflictos y límites de entrada

| ID | Hallazgo | Decisión T0 y alcance pendiente |
|---|---|---|
| V01 | A1 no tiene PNG autónomo aprobado | Usar lenguaje de A0/A2 para propuesta nativa en T2; captura individual candidata, sin atribuirle aprobación previa. |
| V02 | A0 muestra6 tarjetas, el SDD/sectores JSON enumera8 incluyendo Arte/Tecnología | Conservar composición/orden de las6 visibles como referencia. No inventar en T0 una nueva fila o menú. La ubicación de los2 sectores adicionales requiere revisión de la captura candidata de A0 en T2; el registro extensible puede prepararse antes sin activar contenido vacío. No declarar esa ampliación visual aprobada. |
| V03 | A3 dice que al completar se descontarán consumibles | Prima la solicitud explícita y las reglas de confirmación. Texto futuro: «Revisa y confirma el consumo real antes de descontar». Abrir/avanzar/marcar pasos no descuenta. |
| V04 | A3 muestra2 paquetes de ligas,20 clavos y stock Listo de muestra | Reemplazar solo datos, con cantidades/unidades del recurso y stock real. No crear esos materiales ni equivalencias a partir de una imagen. |
| V05 | A5 muestra5 pasos y supervisión al manipular clavos | Mostrar N real del catálogo/copia y preparación exclusiva del adulto según seguridad. No generar pasos adicionales para igualar un número de maqueta. |
| V06 | A6 contiene estudiantes ficticios y retratos; hay otra mascota dibujada | Omitir esas filas/fotos. Solo bloque no interactivo Próxima integración y Concepto C canónica. Sin base de alumnos ni sexta tab. |
| V07 | A4 muestra una competencia curricular | Solo mostrar asociación si el recurso la tiene cotejada con fuente verificable; si no, propuesta editorial y fuente pendiente, sin aval MINEDU. |
| D01 | Ejemplo activityId mat-003 usa IDs example-wood-board/elastic-bands | No son los IDs reales del catálogo. El ejemplo es un contrato propuesto; no se instala ni reemplaza materialId de usuarios. |
| D02 | contentVersion=1 en ejemplo y semver en packs actuales | Separar contractVersion, packVersion, contentRevision y RoomSchema. Adaptador explícito; no castear números a versiones de pack ni degradar WorkshopCodec2. |
| D03 | Ejemplo Preparar tablero tiene adultOnly=false/isDangerous=false | Esos flags no acreditan seguridad. Validar cada paso real; las operaciones con clavos no se autorizan por copiar el ejemplo. |
| D04 | Ejemplo workflow.screenOrder no incluye ficha detail | Las rutas se rigen por A0→A1→A2→A3→A4→A5→A6, no por un array ilustrativo incompleto. |
| D05 | A0 sectores próximos, pero el repositorio ya permite importar dance/heritage | Próximamente es estado por disponibilidad de contenido compatible, no veto a trabajo previo. Mantener acceso a todos los packs/copies existentes; no trasladar arte-001 fuera de Taller ni ocultarlo bajo una categoría vacía. |
| M01 | Ninguno de los8 candidatos tiene humanApproved=true | No pasar medios a assets ni usarlos como portadas definitivas. exp-003..008 sin imagen específica; exp-001 insuficiente; exp-002 solo candidata. No producir arte nuevo en T0. |
| M02 | Iconos, fotos de pasos y materiales están compuestos dentro del mockup | No son archivos independientes aprobados. Inventariar faltantes para T2/T4; no recortar el teléfono ni generar sustitutos y declararlos aprobados. |
| P01 | Manual de inventario asume una reserva por registro | T1 debe incorporar reservas independientes por dueño y consumo+liberación propios atómicos, conservando registros legados ambiguos para revisión. |
| P02 | No hay eventId/idempotencyKey persistente de consumo por ejecución | Una cola o botón deshabilitado no sustituye unicidad durable. Diseño en MIGRACION_LEDGER_DISENO.md, no realizado en este ZIP. |
| P03 | Backup2 ignora tablas nuevas y fusiona por ID | Bloqueo de salida T1 hasta exportación/restauración completa y conflictos probados; no publicar ledger incompleto. |
| R01 | Firma release falla por PROFELAB_KEYSTORE_PASSWORD ausente | Configuración manual del propietario; no cambiar clave ni silenciar checks. No bloquea la auditoría T0 ni la compilación DEBUG existente. |
| R02 | README antiguo menciona dos JPG comparativos ausentes | No se necesitan. Solo PNG individuales vinculantes; no reconstruir ni utilizar la infografía rechazada. |
| R03 | Mensaje corto cita ZIP histórico y T0/T1 | Prima el mensaje actual: T0 documental primero. Este ZIP no incluye T1 ni interfaz nueva. |

La aprobación de un mockup no aprueba sus datos demostrativos, imágenes de procedimiento, alumnos o asignaciones científicas. La revisión de seguridad personal de una copia tampoco equivale a aprobación editorial de contenido/medios.
