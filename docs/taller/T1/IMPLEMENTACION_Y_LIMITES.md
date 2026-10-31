# T1 — Inventario transaccional implementado

Base: main `ba7d00c7368a8e2152cbe0462857edab6cbdc285`, verificado el 31/10/2026 mediante consultas GET. Se continúa el Taller existente y el plan `docs/taller/T0/PLAN_T1_T6.md`. T0 se conserva como antecedente; sus afirmaciones de «todavía no implementado» describen aquella entrega.

Estado de esta entrega: código real implementado y controles locales superados; aceptación Android pendiente. No está declarada T1 validada para distribución. No se inicia T2 hasta superar las pruebas Kotlin/Room/API26/API36 y revisar sus informes.

## Arquitectura y persistencia

- `core:model/InventoryOperations.kt`: disponibilidad READY/PARTIAL/MISSING/REVIEW, cantidades decimales acotadas, solicitudes y contrato InventoryOperationStore. Sin Android, red ni dependencia de feature.
- `core:database/InventoryEntities.kt`: operaciones durables con clave única, movimientos por stock/secuencia y reservas por stock/dueño. Reversión única por operación. Relación de movimientos con cabecera mediante FK; las referencias a stock/run existentes se validan en repositorio y respaldo porque el stock sigue siendo un WorkRecord.
- `InventoryOperationRepository.kt`: una transacción Room incluye cantidad física, reservas, historial, proyección compatible y estado del comando. WorkspaceRepository implementa el contrato y audita también los caminos anteriores de guardado, ajuste, compra y archivo.
- `WorkshopCodec.kt`: conserva requisitos y datos anteriores. Calcula disponibilidad descontando reservas ajenas y permitiendo la propia. Stock ausente o de tipo/saldo ambiguo requiere revisión; no se inventa una cantidad disponible.
- `feature:taller`: conserva ViewModel, cola y errores/reintento. Se conectan controles funcionales a la ficha personal y al formulario existentes. No se crean módulos Gradle ni dependencias nuevas.

## Comandos y reglas

Preparar un consumo guarda PREPARED sin descontar. Se conserva el operationId y payload versionado; otra solicitud con la misma clave y otros datos se rechaza. Solo hay un consumo pendiente por run. Al confirmar se releen identidad, unidad, equivalencia, saldo y reservas dentro de la transacción. Reintentar un COMMITTED devuelve sin volver a descontar. Una segunda ejecución explícita usa otra clave.

Se usa exclusivamente cantidad real indicada por el usuario. No se consume al consultar, abrir plantillas, marcar pasos, compartir compras ni cambiar de pestaña. Solo CONSUMABLE explícito puede consumirse. TOOL, REUSABLE y tipos desconocidos se rechazan en esta API. Las unidades/paquetes deben estar revisados; el usuario introduce cantidades en la unidad del stock elegido. No hay conversión oculta en el comando de consumo.

Las reservas son cantidades absolutas por dueño, nunca descuentos de existencia física. La confirmación puede liberar las reservas propias de las líneas consumidas y siempre protege las ajenas. Ejemplo codificado en pruebas: 14 ligas, 10 reservadas para la misma copia, uso real 8 → cantidad 6, reserva propia liberada; otra confirmación conserva 6. Es un caso de prueba escrito: falta ejecutarlo en Room.

Los cambios posteriores a T1 tienen un movimiento con snapshots mínimos de saldo/reservas. El primer movimiento sobre un stock heredado conserva su saldo anterior como apertura; no reconstruye operaciones pasadas. El historial JSON antiguo permanece. Las compras mantienen shopping_v2 y sus receipts; su stock y recibo se guardan juntos y los reintentos mantienen una clave de mutación. Los ajustes existentes también pasan por el ledger. No se suma el ledger a quantity ni se vuelve a restar un consumo histórico.

Los formularios desactualizados se rechazan por revisión, incluso al editar solo notas. La proyección inventoryReservations se reconstruye desde las reservas de Room al guardar notas; no puede autorizar reservas por sí sola. Un formulario agregado no puede reemplazar las reservas de varios proyectos. Estas se editan por dueño en la ficha personal. No se archiva stock con reservas activas.

La reversión añade una compensación ligada al original; no lo elimina. Solo se revierte una operación completa si sigue siendo el último movimiento de cada stock y coinciden saldo y reservas. Las altas y las operaciones con cambios posteriores se rechazan con explicación. No se implementa reversión parcial.

## Controles disponibles en la aplicación existente

Abrir una copia personal de proyecto o experimento → «Reservas y consumo real» → «Preparar reservas o consumo». Cada reserva se confirma por material/dueño. Para consumo se indican cantidades reales y motivo → «Revisar consumo» → se muestra el comando durable → «Confirmar consumo real» o «Cancelar sin descontar».

Los consumos pendientes aparecen también al volver a la ficha. En Mi inventario se mantiene Ajustar existencia; su opción Consumo usa el mismo repositorio. Se añaden controles de recuperación de consumos manuales pendientes. El historial reciente permite pedir una reversión con confirmación. Los errores conservan el comando pendiente y usan el aviso/reintento ya existente.

Este panel es una integración funcional T1 en las pantallas actuales. El portal, catálogo y ficha visual nuevos corresponden a T2 y aún no se entregan. No se ha validado fidelidad visual ni accesibilidad de estos controles en dispositivo.

## Migración y respaldo

`INVENTORY_MIGRATION_2_3` crea tres tablas y seis índices. Conserva MIGRATION_1_2; no hay borrado, reemplazo de tablas antiguas, backfill de stock ni fallback destructivo. El JSON, revisiones, perfil, progresos y documentos existentes no se reescriben durante la migración. Las reservas escalares se adoptan al operar explícitamente; si no tienen dueño se conserva uno interno @legacy, sin atribuirlo a un proyecto ficticio ni liberarlo por intuición.

BackupTransfer exporta formato 3 con las ocho tablas en un snapshot Room. Conserva preferencias, medios, hashes y recuperación del journal de preferencias. Importa formato 2 sin fabricar eventos históricos; si un formato 2 contiene metadatos de la nueva proyección, se retiran porque no incluye sus tablas autoritativas. No afecta al contenido de respaldos 2 genuinos anteriores a T1.

El formato 3 verifica claves, payloadHash, referencias, líneas, cantidades, secuencias sin huecos, último saldo, reservas completas y proyección. Rechaza un evento existente con otros datos o una fusión que añadiría eventos/reservas a un stock local ya presente. El mismo agregado completo puede restaurarse otra vez sin aplicarlo. La política conservadora es conservar lo local o restaurar el agregado completo en una instalación de prueba vacía; no mezclar mitades. No se ofrece todavía un asistente para resolver conflictos manualmente.

## Verificación y pendientes de aceptación

Ejecutadas localmente: 51 pruebas Python (16 estructura/contratos existentes, 8 SQL literal de migración en SQLite, 11 firma con herramientas simuladas, 6 finales de línea, 6 orquestación simulada y 4 contenedores Flow). Se verifican por bytes los recursos/funciones ajenos al alcance y se conservan las 49 declaraciones de pruebas Android previas. El chequeo léxico de 155 archivos Kotlin/KTS no equivale a compilación.

Se añaden 34 pruebas Kotlin: 9 JVM en core:model y 25 instrumentadas (14 ledger, 1 migración, 7 respaldo y 3 disponibilidad). NO EJECUTADAS aquí. La suite dispone ahora de 74 declaraciones instrumentadas en app/core:database; no es un recuento de aprobaciones.

No se ejecutaron Gradle/Kotlin/KSP/lint, Room en Android, APK, emuladores, cierre forzado real del proceso, pruebas de firma instalada, TalkBack, capturas, rendimiento ni regresión de dispositivo. No hay Java/Gradle/adb disponibles en este entorno y no se instalaron herramientas. El test de reapertura cierra y abre la base; no simula un kill real del sistema. La atomicidad al matar el proceso sigue pendiente de prueba instrumental/manual.

No se entrega un 3.json fabricado. Room/KSP debe exportar `core/database/schemas/pe.profelab.core.database.ProfeDatabase/3.json` durante la compilación; las pruebas de migración abren fixtures reales 1/2 y requieren la validación completa del esquema por Room. Conservar el 3.json generado en la siguiente integración una vez verificadas esas pruebas. Las 8 pruebas SQLite ejecutadas no sustituyen esta puerta.

Antes de distribuir: workflow manual con run_emulator=true, todos los jobs Android aprobados, revisión de esquema 3 generado, actualización con la firma permanente existente, respaldo completo y pruebas de datos en dispositivo. No instalar un DEBUG por ser el primer artefacto disponible. No degradar a Room2 ni desinstalar para corregir un fallo. Se requiere corrección hacia delante que mantenga esquema3.

Las cinco pestañas, applicationId, configuración de firma/versión, catálogo de 20 proyectos/experimentos, packs, mascota/Flow, PDF/OCR, Inicio, Sesiones, Cuaderno y Biblioteca se conservan. No se implementa Mis alumnos ni se añaden estudiantes. No se alteran imágenes ni se usa la infografía rechazada. T2 retomará exclusivamente los seis mockups verticales aprobados.
