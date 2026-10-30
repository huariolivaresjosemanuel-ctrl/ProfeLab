# Fidelidad visual: implementación y verificación pendiente

Autoridad: `reference/01_REFERENCIAS_UI/02_TALLER.png` de main. Se inspeccionaron también las otras cuatro referencias para mantener coherencia. Las láminas conceptuales posteriores «Crea material didáctico» e «Investiga y experimenta» ayudan a explicar flujos; no reemplazan las cinco referencias originales.

| Sección aprobada | Correspondencia en fuentes | Diferencia / comprobación necesaria |
|---|---|---|
| Cabecera ProfeLab y cinco destinos | Contenedor global conservado; Mi taller y Nuevo proyecto en WorkshopCatalog/Components | Renderizado nuevo en teléfono NOT_RUN |
| Crema, verde natural y pastel | Paleta local verde/menta/amarillo/azul/durazno/lavanda, tarjetas redondeadas, jerarquía Material3 y espaciados | No se modificó el tema global; no hay medición de contraste o comparación de píxeles ejecutada |
| Buscador y categorías | Buscador real, Materiales didácticos / Experimentos / Mis ideas, filtros desplegables | Etiquetas extensas se desplazan horizontalmente para no añadir pestañas globales |
| Proyecto destacado con ilustración | PNG por ID, objetivo/metadatos/disponibilidad, favorito y guía real | Altura se adapta al contenido; no es una captura del mockup |
| Materiales disponibles | Tarjetas horizontales de las existencias personales, Gestionar inventario | Primera apertura vacía: no se simulan cantidades de la referencia |
| Preparación | Checklist de un trabajo personal, contador y barra a partir de pasos guardados | Solo aparece si existe un proyecto propio; no se precarga un trabajo ficticio |
| Más proyectos | Miniaturas originales, nombre/metadatos, estados y apertura funcional | Dos columnas en anchura suficiente; una con pantalla estrecha o letra grande |
| Recomendaciones | Orden por compatibilidad real; revisión de seguridad separada | Se distinguen copias revisadas y coincidencia material del catálogo pendiente |
| Ficha y Práctico/Académico | Tarjetas de diez bloques, galería, zoom, filas de materiales, fuentes y límites | La referencia no especifica cada estado extendido; se reutiliza su identidad sin inventar otro módulo |
| Experimentos | Pregunta/hipótesis, pasos/materiales, registros/conclusiones, inclusión/seguridad | Sin dibujo de germinación ficticio: faltan imágenes originales de los experimentos |

Las coincidencias de esta tabla describen el código y los recursos usados. **No son PASS de fidelidad visual.** No se han capturado las pantallas de esta versión ni ejecutado la app. La imagen del usuario con Geoplano documenta el fallo anterior, no la apariencia de esta implementación.

Para cerrar aceptación: capturar Taller, ficha Geoplano, inventario vacío/con datos, Mis ideas y Experimentos en el mismo teléfono de referencia; revisar composición, espaciado, tamaños, interacción y ausencia de recortes. Repetir en 320dp, teléfono normal, tablet, horizontal y escala de letra grande. Ver GUIA_PRUEBAS_USUARIO.md.
