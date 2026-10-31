# Diseño propuesto de T1: stock, reserva y ledger

Estado: diseño T0, no código ni migración ejecutable. Room actual2 permanece intacto. Reutilizar WorkRecord, InventoryRules, stock y compras existentes; separar comando de inventario de UI.

## Decisión de arquitectura

Se recomienda un InventoryOperationStore tipado en core:model y su implementación en core:database, con transacción Room única que abarque stock JSON legado, reservas y movimientos. No crear un módulo Gradle. Para unicidad duradera y reservas múltiples consultables, la propuesta base es Room2→3 con tres tablas acotadas: inventory_operations (cabecera/idempotencia/estado/payloadHash), inventory_movements (líneas/snapshot/compensación) e inventory_reservations (dueño/stock/cantidad/estado). Columnas e índices exactos se fijan en T1 contra el esquema exportado; no se inventa un 3.json manual en T0.

Alternativa evaluada: registros WorkRecord con PK derivada de operación dentro de editRecords. Es viable para atomicidad básica, pero requiere igualmente validar idempotencia, búsquedas, reservas, tamaño y fusión de backups. No se adoptan dos ledgers simultáneos. La recomendación de tablas se justifica por integridad/consultas y restricciones únicas, no por estética. T1 debe demostrar la decisión con fixtures antes de activarla.

## Fuente de verdad y contabilidad

quantity de inventory sigue siendo el saldo materializado que entienden las pantallas actuales; ledger audita operaciones posteriores a la adopción. La migración no reproduce consumos anteriores ni vuelve a restarlos. Guardar un saldo inicial de adopción cuando se use ese stock por primera vez, conservar movements original como historial legado y reconciliar solo movimientos nuevos. No sumar ledger y restar otra vez sobre quantity.

Todos los caminos de escritura posteriores a la adopción —adjustInventory, undoInventory, acquire, guardado de cantidades/reservas y restauración— deben pasar por la misma operación de dominio. Conservar el comportamiento de compra y receipts existentes, enlazando sus IDs, en lugar de crear compras paralelas. Una edición externa/importación debe validarse y registrarse como corrección explícita o conflicto; no alterar silenciosamente el saldo auditado.

## Ciclo seguro de operación

1. Usuario pide revisar un consumo real en su copia. Preparar un comando durable con operationId estable y payload versionado; recuperar el mismo comando tras giro/reinicio. No crear comandos al abrir fichas ni consumir templates. Generar otra secuencia solo para otro consumo explícito.
2. Confirmar relee saldos, tipo/unidad y reservas en transacción. Verifica UNIQUE(idempotencyKey) y payloadHash. Misma clave/mismo payload ya confirmado devuelve resultado previo; misma clave/otro payload rechaza conflicto. PREPARED no descuenta; COMMITTED es durable y no se reaplica.
3. Validar todas las líneas antes de escribir. BigDecimal con cantidades serializadas como texto, límites existentes y conversiones explícitas. Saldo insuficiente cancela toda la operación, conserva borrador y permite seguir la actividad sin registrar consumo hasta resolverlo.
4. Aplicar únicamente CONSUMABLE explícito. TOOL/REUSABLE no se descuentan; UNKNOWN/ambigüedad se muestran REVISAR. Actualizar saldo, liberar reserva propia, insertar líneas y marcar COMMITTED atómicamente. Una reserva de otro proyecto sigue protegida.
5. Si se cancela, no cambia stock. Si el proceso muere antes del commit, rollback; si muere después, el comando/resultado guardado permite reconocerlo al volver. Un ID nuevo en cada reintento no sirve.
6. Reversión: nueva operación compensatoria ligada mediante revertsEventId, validada e idempotente, sin borrar el original ni dejar negativos. Primera implementación puede revertir operación completa; cualquier reversión parcial necesita límites acumulados e IDs igualmente probados.

Ejemplo del SDD, no prueba ejecutada:14 ligas,10 reservadas para el mismo proyecto y uso real8 → quantity6, reserva propia liberada; reintentar mantiene6. Hoy adjustInventory exige next>=reserved y rechazaría6<10 sin liberar antes: se necesita una transición atómica de ambas magnitudes, no dos taps separados.

## Migración y compatibilidad

- MIGRATION_2_3 exclusivamente aditiva. Preservar byte lógico de JSON, IDs, revisions, favoritos, steps.done, archivos, tareas, perfil y DataStore; no backfill masivo que invente estados o saldos históricos.
- Reservas antiguas con dueño/cantidad fiables se representan sin duplicarlas. Si falta dueño o unidad, retener reserved/JSON original y marcar REVISAR; no liberar ni asignar por intuición.
- Separar índices de stock, materialId y runId. Clave de operación estable y restricción única en persistencia, no solo en Compose. archive de un proyecto no borra ledger ni libera reservas sin decisión trazable.
- Backup necesita nuevo formato compatible hacia atrás: exportar operaciones+líneas+reservas+stock en snapshot coherente y restaurar backup2 anterior. Una aplicación antigua no debe interpretar parcialmente un backup nuevo.
- No reutilizar INSERT OR IGNORE por fila para el nuevo agregado: mezclar stock local conservado con movimientos importados puede falsificar historia. Previsualizar conflicto por agregado y comparar generación/revisión/digest; mismo evento diferente payload se rechaza. Preservar lo local, permitir restauración completa coherente o rechazo explicado, sin aplicar solo una mitad.
- Instalación APK: misma firma e incremento de código según workflow existente. No cambiar claves ni desinstalar. Un rollback a fuentes con Room2 no abrirá DB3: después de activar T1, preferir corrección hacia delante manteniendo compatibilidad3; jamás borrar DB para revertir.

## Puerta de salida T1

Bloqueantes: pruebas JVM de cantidades/unidades; idempotencia de doble llamada concurrente y tras reapertura Room; kill antes/después de commit; rollback multirregistro; parcial, herramientas/reutilizables y UNKNOWN; reserva propia/ajena y varias ejecuciones; compensación repetida; compras antiguas; migración1→2→3 y reapertura; backup2→3, nuevo roundtrip y conflicto entre stock/eventos; no duplicar movimientos al restaurar. Requerir schema3 exportado por Room y validado, no una simulación SQLite como única evidencia.

T0 no ejecuta estas pruebas ni valida un modelo ya implementado: T1 no está listo para publicar hasta superar esa puerta. La falta de revisión de imágenes no impide probar el dominio; la seguridad de datos sí impide activarlo.
