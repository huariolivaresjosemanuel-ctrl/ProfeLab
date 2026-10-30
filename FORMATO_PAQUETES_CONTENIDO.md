# Paquetes de contenido ProfeLab V2 / extensión 2.1

Nombre convencional: `PROFELAB_CONTENIDO_V2.zip`. manifest.json debe estar en la raíz. El ZIP contiene solo archivos declarados: no entradas de directorio explícitas, ejecutables, claves ni archivos auxiliares no listados. Se mantiene schemaVersion=2 y compatibilidad con los paquetes mínimos V2 de proyectos/experimentos; contentContractVersion agrega 2.1 para materiales/layouts/imágenes.

## Manifiesto

```json
{
  "schemaVersion": 2,
  "contentContractVersion": "2.1",
  "packId": "mi-taller-autorizado",
  "version": "1.0.0",
  "minimumAppSchema": 2,
  "maxAppSchema": 2,
  "minimumAppVersion": "1.0.1",
  "items": [
    {"path":"projects/mi-proyecto.json", "type":"project", "sha256":"<64 hex del archivo final>"},
    {"path":"images/vista.png", "type":"image", "id":"vista", "sha256":"<64 hex del PNG>",
     "source":"Autor y procedencia comprobables", "license":"Licencia autorizada", "distributionApproved":true}
  ]
}
```

Los campos entre <...> son marcadores documentales; este ejemplo no es un paquete instalable. Para un fixture ejecutable consulte WorkshopContentPackTest.kt (test escrito, NOT_RUN).

IDs: `[a-zA-Z0-9][a-zA-Z0-9._:-]{0,127}`. Versiones: tres enteros de hasta seis cifras cada uno, sin sufijos. minimumAppSchema entre 1 y 2; maxAppSchema, si existe, debe incluir 2. minimumAppVersion no puede exceder 1.0.1 para este importador. Los SHA-256 de item deben coincidir exactamente en minúsculas.

## Tipos y cuerpos JSON

| type | Cuerpo |
|---|---|
| project / experiment | id, title; materials, steps, images, visualTemplate, layoutId y contenido pedagógico opcionales; completar editorialmente los campos de la guía antes de distribuir |
| material | id, name, unit, materialKind; synonyms, category, warnings y alternatives opcionales |
| layout | id, title, template; sectionOrder opcional |
| image | Bytes png/jpg/jpeg/webp; id, source, license y distributionApproved=true en el item del manifiesto |

Todo cuerpo JSON debe declarar `contentOrigin: "EDITORIAL_PROFELAB"` o `"USER_AUTHORED"` y `distributionApproved: true`, o los equivalentes antiguos `contentRights.origin`/`approvedForDistribution`. No se importan automáticamente obras externas no autorizadas. Es una declaración del autor, no una auditoría jurídica ni una firma digital.

Requisito nuevo recomendado:
```json
{"id":"req-1","materialId":"carton-autor-001","name":"Cartón definido por el autor",
 "amount":"2","unit":"unidad","materialKind":"CONSUMABLE"}
```
El material maestro conserva ese mismo id lógico. Su fila Room instalada tiene clave `material:<id>` pero el materialId de requisitos usa el **id lógico sin añadir ese prefijo**. Para reutilizar un material incluido en APK use su ID exacto de materials_v2.json; no cree otro ID con el mismo nombre o sinónimo.

Cantidades como texto decimal positivo o desconocidas vacías; role CONSUMABLE/REUSABLE/TOOL. contentAmount/contentUnit especifican contenido conocido por paquete. Las cantidades y unidades ausentes no hacen realizable un proyecto. Se sigue leyendo materials heredado, pero nuevos autores deben usar objetos tipados.

Paso recomendado: `{"id":"paso-1","title":"Acción documentada","materialsUsed":"Materiales usados","notes":"Orientación editorial","images":["vista"]}`. Máximo 100 pasos y 4000 caracteres de descripción por paso. El importador reinicia done=false y safetyReviewedByUser=false. Las fotos referenciadas por images deben declararse en el mismo paquete; pasos y portada pueden compartirlas.

templates: CONSTRUCTION, DIDACTIC, EXPERIMENT, CREATIVE, ACADEMIC.
sectionOrder admite purpose, inquiry, materials, preparation, steps, classroom, variants, safety, sources, evidence. Se aceptan hasta 12 entradas; el renderizador elimina repetidas y mantiene obligatorios materiales/pasos/seguridad/fuentes/evidencias. layoutId se refiere al ID local de un layout del mismo paquete. El importador lo convierte a `pack:<packId>:layout:<id>`.

Los campos pedagógicos existentes (pedagogy/practical/academic, objectiveSuggested, researchQuestion, grades, area, estimatedMinutes, safety, inclusion, variants, source/sources) se conservan. Nuevas asociaciones curriculares requieren fuente/ubicación y revisión humana; un flag verified no constituye aval MINEDU. Riesgos prohibidos permanecen restringidos aun después de confirmar una revisión personal.

## Límites y protección

Hasta 200 entradas ZIP, 180 items, 40 MiB descomprimidos totales, 2 MiB por JSON y 8 MiB por imagen. Imágenes de hasta 10.000 píxeles por lado y 25 millones de píxeles; se comprueba decodificación muestreada. Hasta 100 requisitos y 20 imágenes por ficha/paso. Se rechazan rutas absolutas, barras invertidas, dos puntos, segmentos vacíos, `.`/`..`, duplicados y archivos no declarados. No hay carga de scripts, código nativo, DEX, HTML ejecutable ni plugins.

Se valida primero y se confirma después desde Perfil. Los recursos se copian a archivos privados por UUID; imágenes idénticas por hash se comparten dentro de la instalación. Se comprueban conflictos de material por ID, nombre/sinónimo, dimensión física y tipo. La transacción confirma todas las filas o ninguna. Ante error se retiran los archivos recién copiados; un cierre abrupto del proceso puede dejar huérfanos no referenciados. No se borra contenido previo para resolverlos.

## Actualización y protección del trabajo

Misma packId + mismo digest de manifiesto: no hace cambios. Misma versión con bytes distintos o una versión inferior: rechazo. Mayor versión: reemplaza por sus IDs las filas de catálogo incluidas; conserva copias personales y recursos anteriores. Omitir un item de una nueva versión **no lo elimina automáticamente** del catálogo anterior. No hay un protocolo de tombstones o desinstalación destructiva en esta entrega.

Proyectos/experimentos se guardan como `pack:<packId>:<id>`. El catálogo es inmutable en Taller; al comenzar se crea UUID con snapshot. Ningún paquete puede escribir stock, compras, progresos o fotos personales. El respaldo existente conserva estos registros y archivos según su motor; probar restauración completa sigue pendiente.

JSON Schema en `core/content/contracts/workshop-content-v2.1.schema.json` documenta manifiesto y definiciones de autor. No se instala ni ejecuta una biblioteca JSON Schema en Android: ContentPackValidator aplica validaciones Kotlin adicionales y conserva compatibilidad V2. Aquí se comprobó JSON/referencias/enums de ese contrato, no conformidad completa con un validador externo (jsonschema no disponible). El futuro editor debe validar tanto los $defs correspondientes como el paquete real en Android.
