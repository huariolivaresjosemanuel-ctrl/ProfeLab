# Catálogo de materiales y recomendaciones offline

materials_v2.json aporta 45 definiciones derivadas de los nombres de los 12 proyectos y 8 experimentos originales. No incluye existencias, precios ni alternativas sin fuentes. La clasificación de herramientas es organizativa, no una certificación de seguridad. Los JSON originales permanecen intactos; un tipo de uso explícito en un requisito tiene prioridad sobre el valor sugerido por el catálogo.

MaterialDefinition: id permanente, nombre, synonyms, category, unit, materialKind (CONSUMABLE/REUSABLE/TOOL), warnings y alternatives. La cantidad pertenece al requisito de la ficha o a la existencia del usuario, **no** a la definición maestra. MaterialLine agrega ID de requisito, materialId, amount, unit y equivalencia opcional contentAmount/contentUnit.

Nombres se comparan en minúsculas raíz, sin acentos, con espacios normalizados; solo se usan coincidencias exactas o sinónimos declarados inequívocos. No se considera que «cartón corrugado», «cartulina» y «cartón» sean el mismo material sin una definición editorial explícita. Usar las sugerencias del formulario evita identidades duplicadas. Un registro de inventario repetido con igual identidad/unidad/tipo se rechaza; corregirlo o ajustar el existente.

## Cantidades y unidades

- Decimal no negativo, coma o punto, máximo 18 cifras significativas y 8 decimales; cantidades de requisitos deben ser positivas. Vacío significa «por revisar». No hay separador de miles ni notación exponencial.
- 1 m = 100 cm, 1 kg = 1000 g, 1 l = 1000 ml; alias como unidades/uds/piezas se normalizan.
- Hoja, tablero, plancha y otras unidades explícitas solo se comparan con la misma unidad normalizada. No se inventa cuántas hojas son un tablero.
- Un paquete necesita contenido conocido. 2 paquetes de 10 hojas cubren 20 hojas; un paquete desconocido queda por revisar incluso frente a otro paquete desconocido.
- Conversiones usan BigDecimal y hasta 8 decimales, redondeando hacia abajo. Una fracción ínfima puede requerir revisión/adquisición adicional en lugar de declarar stock sobrante.
- Cantidad libre = existencia − reserva. Solo se libera para el cálculo la reserva asignada al proyecto o conjunto de proyectos seleccionado. No se modifica físicamente esa reserva al consultar.
- Consumibles compatibles se suman. Herramientas y reutilizables usan el máximo requerido en proyectos que se preparan **secuencialmente**. No hay planificación de uso simultáneo; si necesita varias herramientas a la vez, registre el requisito real y ajuste la compra.

El motor consolida requisitos repetidos compatibles antes de comparar, evitando que el mismo saldo cubra dos líneas consumibles. No mezcla tipos de uso o dimensiones incompatibles. Para cada línea devuelve requerido, libre compatible, faltante y revisión manual. El orden de recomendación usa proporción cubierta por cantidades, no azar; el porcentaje medio es orientativo y no una predicción pedagógica.

## Seguridad y alternativas

La disponibilidad y la seguridad tienen estados separados. Material completo no significa actividad infantil segura. FORBIDDEN/prohibited/notRecommended mantiene «No recomendado». Sin revisión personal se muestra pendiente; una copia revisada puede requerir intervención adulta. Las recomendaciones revisadas excluyen pendientes y prohibidos; el catálogo puede mostrar «más compatible con mis materiales» conservando claramente el aviso pendiente.

Una alternativa se habilita solo con verified=true, razón y fuente no vacías, factor positivo, destino existente y tipo de uso compatible. La selección es explícita y afecta la copia personal, nunca reescribe silenciosamente los originales. «Verificada» es metadata del autor que debe comprobarse: el software no valida por sí mismo la bibliografía o la seguridad. Advertencias de material y del proyecto permanecen visibles.

Compras conserva la selección por projectId; añadir dos veces el mismo proyecto reemplaza su selección, no multiplica cantidades. Faltantes se recalculan con existencias actuales y con los requisitos actuales de proyectos propios; el catálogo seleccionado guarda snapshot. Una corrección manual afecta la cantidad a adquirir; cada incorporación confirmada limpia esa corrección y la marca adquirido. La lista y su recibo se guardan junto al stock en una transacción. Los artículos manuales incorporados se ocultan mediante stocked; pueden agregarse otra vez de forma explícita. No se calculan precios ni se consumen materiales por checklist.
