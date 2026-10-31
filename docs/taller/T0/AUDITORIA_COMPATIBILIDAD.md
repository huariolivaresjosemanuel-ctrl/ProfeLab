# Taller · T0 · Auditoría de compatibilidad

Fecha: 2026-10-31, America/Lima. HEAD real de main: `f22480902b4a7ca92e57b85533a365cfa1e045b2`, árbol28668aa2dc8761ced4bd73df3c5018200e863915; coincide con el ZIP. Se preparó una copia aislada ProfeLab_T0 con399 blobs comprobados mediante SHA Git. GitHub solo GET; no commits, push, PR ni Actions. La entrega es documental: no implementa T1 ni modifica código Android.

## Lecturas y autoridad

Se leyó primero 00_REFERENCIA_VISUAL_VINCULANTE.md y después las instrucciones, SDD, flujos, inventario, puente, política de imágenes, prompt maestro, seis JSON y matriz51. ZIP con49 archivos, CRC válido;47 hashes y47 filas del inventario coinciden. Los dos archivos fuera del manifiesto son el propio manifiesto y el CSV de inventario. Se decodificaron28 PNG/WebP y se inspeccionaron los seis PNG verticales individualmente. La infografía rechazada no está en el paquete y no se utilizó.

Los seis PNG son autoridad visual de A0/A2/A3/A4/A5/A6. A1 sigue sin mockup autónomo aprobado. Los WebP sirven para consulta, no para sustituir el PNG canónico. Las ocho imágenes de contenido tienen humanApproved=false: ninguna pasa al APK en T0. Las fotos, cantidades, progreso, retratos y nombres de las maquetas no son datos iniciales.

## F0 integrado: no repetir

Los399 archivos de la base coinciden byte a byte con el árbol preparado de F0. Sus9 cambios ya están en main. En el [run41229357427](https://github.com/huariolivaresjosemanuel-ctrl/ProfeLab/actions/runs/41229357427), build y ambos jobs Android son SUCCESS. Los XML descargados contienen43 casos app y6 database sin fallos/errores/omitidos por API;49 por API. Las tres pruebas antes fallidas pasan y se recuperan10 PNG por API, incluidas las cinco secciones separadas. Se conservan esos controles y no se vuelven a aplicar fixes anteriores.

El run global es FAIL por el job release: falta configuración PROFELAB_KEYSTORE_PASSWORD. No es un error de Taller ni de los tests API26/36. Cleanup pasó y no se publicó release. No se cambia código para omitir la comprobación, ni se pide enviar una contraseña al agente. La actualización real del teléfono continúa sin acreditarse por estos tests DEBUG.

## Modelo vigente comprobado

- appId pe.profelab.app; versión1.0.2, code base5; min26/target36. Cinco tabs y16 módulos app/core/feature. Sin nueva dependencia, red o módulo Gradle para este encargo.
- Room2: tasks, profiles, drafts, activity, workspace_records. WorkRecord conserva id/kind/title/json/updatedAt/revision/archived. WorkspaceRepository.editRecords relee filas dentro de una transacción y permite mutación atómica de varias filas; esto es base reutilizable, no un ledger completo.
- WorkshopCodec.PAYLOAD_VERSION=2; copia y normaliza sin reescribir el original al consultar. Las plantillas se leen de assets y las copias personales reciben UUID. Identificadores de20 originales intactos:12 proyectos,8 experimentos. Hay45 definiciones materiales locales.
- MaterialKind contiene CONSUMABLE/REUSABLE/TOOL, no UNKNOWN. El decodificador puede inferir CONSUMABLE por ausencia de flags. El nuevo flujo no debe usar esa inferencia como autorización de consumo de un registro ambiguo: presentar REVISAR sin alterar la clasificación histórica.
- InventoryRules usa BigDecimal,18 cifras/8 decimales, unidades comparables y equivalencia explícita de paquete. Consolida consumibles por suma y herramientas/reutilizables por máximo entre proyectos. Reutilizarlo; no duplicar un motor paralelo.
- Reservas actuales: quantity/reserved/reservationProjectId en JSON inventory. Un propietario por registro; WorkshopCodec.inventory puede excluir la reserva propia al evaluar un proyecto. No hay reservas múltiples individualizadas con historial.
- Consumo actual: ajuste manual confirmado en InventoryForm→adjustInventory, con control de tipo/saldo y movimiento JSON. Cada llamada crea newId; no hay clave persistente de operación docente. El bloqueo del botón/cola no prueba idempotencia tras reinicio o doble confirmación.
- Reversión actual: compensa solo el último ADJUST/CONSUME si el saldo sigue igual; enlaza el antecedente por texto, no mediante revertsEventId exigido por el contrato nuevo.
- Compras: shopping_v2, selections/excluded/overrides/manual/purchased/stocked/receipts. Repetir proyecto actualiza selección. acquire usa receiptId persistido y actualiza inventario+lista atómicamente; se conserva, no se reemplaza por otra lista. No hay clipboard ni ACTION_SEND en feature/taller.
- Progreso actual: steps[].id/done, status BORRADOR/EN_PROGRESO/COMPLETADO, cola/flush y copias persistentes. Hay guía larga y editor, no pager guiado con último stepId persistente como contrato de ejecución.
- Enseñanza: TeacherGuides versión1 con guion, preguntas, secuencia, inclusión, fuentes y formativeEvidence en texto. No hay criterionId/ActivityRun de clase ni alumnos; no convertir las cadenas en evaluaciones individuales automáticamente.
- Packs: schema2/3, contentContract2.x/3.0, semver, hashes, IDs con namespace, staging y validadores. guided_workshop ya existe. Se protegen copias personales frente a importación; FOOD se rechaza por falta de validación. WorkshopCategories ya acepta dance/heritage/music/theater y otros tipos: el nuevo portal no debe ocultar packs válidos existentes.
- Imágenes:12 ilustraciones conceptuales de proyectos; experiment_media_v3 deja las8 portadas experimentales pendientes. WorkshopImages usa decode por tamaño y LruCache12MiB; reutilizar, ampliando metadatos aprobados por actividad/paso en su fase. No cargar PNG grandes ni recortar partes del mockup.
- Backup2 usa lista fija de5 tablas, merge INSERT OR IGNORE y diario de preferencias. Una nueva tabla de ledger no se respalda sola: T1 debe evolucionar backup y conflictos, no limitarse a crear entidades.

## Resultado T0

La base es aprovechable y no requiere reconstrucción. Las ventanas A0–A6 separadas, reservas múltiples, consumo de actividad idempotente, compartir texto, progreso guiado y puente docente son ampliaciones pendientes. No son funciones presentes por aparecer en los JSON de ejemplo.

T1 queda diseñado y condicionado a pruebas reales de migración/backup; no implementado ni validado en Room3. Las limitaciones visuales y editoriales no bloquean diseñar el ledger, pero sí impiden declarar terminadas imágenes/ventanas sin sus revisiones. T2–T6 se entregarán por separado, tras confirmar su HEAD.
