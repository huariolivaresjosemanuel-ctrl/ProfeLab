# Formato instalable de contenido V2

El ejemplo del paquete original es orientativo (hash pendiente, contador items), no un ZIP instalable. Para cerrar la ambigüedad se define este formato concreto; las plantillas originales ya están disponibles offline sin importarlas.

ZIP sin directorios explícitos. Únicamente `manifest.json` y los archivos JSON listados. Nombres relativos sin `..`, `.`, barras inversas, `:`, absolutos o duplicados. Máximo20MiB descomprimidos,2MiB por entrada,200entradas y100fichas. Los checksums se calculan sobre bytes UTF8 finales, no texto reformateado.

```json
{
  "schemaVersion": 2,
  "minimumAppSchema": 2,
  "packId": "mi-pack",
  "version": "1.0.0",
  "items": [
    {"path":"projects/p1.json","type":"project","sha256":"HASH_SHA256_DE_64_HEX"}
  ]
}
```

Ficha mínima: `id`, `title`, `contentOrigin` (`EDITORIAL_PROFELAB` o `USER_AUTHORED`), `distributionApproved:true`. Es una declaración de derechos del autor, no autorización MINEDU ni validación pedagógica. Tipo solo project/experiment. Campos complementarios como los JSON originales: materials,steps,safety,pedagogy,inclusion,etc. No admite PDFs oficiales/binarios/miniaturas adicionales.

Instalación: vista previa antes de confirmar, validación íntegra antes de escribir, batch Room transaccional. Identidad catálogo `pack:<packId>:<id>`, marker `pack:<packId>`. Mismo manifiesto idempotente; misma versión con bytes distintos o downgrade se rechazan. Nueva versión actualiza catálogo; creaciones personales con nuevo UUID no se sobrescriben. Copias originales retiradas de un pack no se borran automáticamente. La revisión/seguridad se fuerza pendiente; un flag del pack no lo convierte en aprobado.

Checksums detectan corrupción, no acreditan identidad del editor ni son firma digital. Revisa derechos y contenido antes de instalar. Tests Android de traversal/idempotencia preparados, no ejecutados aquí.
