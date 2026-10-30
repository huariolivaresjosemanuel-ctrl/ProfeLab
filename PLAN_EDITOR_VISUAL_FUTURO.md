# Editor visual PC futuro — contratos preparados

No se implementó la aplicación de escritorio. El alcance actual es un contrato JSON versionado, el importador Android, cinco diseños Compose y una política de identidad/datos que el editor podrá reutilizar.

Entradas del futuro editor: formularios de título/objetivo/área/grados, catálogo maestro compartido, requisitos tipados, pasos con IDs, imágenes autorizadas, fuentes, riesgos y revisión pedagógica. El orden de bloques debe restringirse a la allowlist de la versión 2.1. Un proyecto editable del editor no debe contener stock ni avances personales de Android.

Salida: ZIP PROFELAB_CONTENIDO_V2.zip con manifest.json y los items declarados; hashes sobre bytes finales; mismos IDs al corregir una ficha, versión semántica creciente; derechos y procedencia por contenido/imagen. Referencia técnica: `core/content/contracts/workshop-content-v2.1.schema.json` y FORMATO_PAQUETES_CONTENIDO.md.

Trabajo posterior:
1. Elegir plataforma PC y guardar borradores de autor con esquema independiente del ZIP publicado.
2. Formularios y catálogo que impidan renombrar/reusar IDs como otro material; búsqueda de sinónimos y conflictos.
3. Biblioteca de medios con licencia/fuente, orientación y compresión; miniaturas y previsualización sin deformación.
4. Editor de pasos/galería/plantilla, validación de cantidades/unidades y fuentes verificables.
5. Flujo editorial de revisión humana pedagógica/seguridad/derechos, con historial de responsables. El check de metadata no sustituye esa revisión.
6. Validador independiente contra JSON Schema y contra las restricciones Android; ensayos de ida/vuelta y actualización de paquetes.
7. Previsualización compatible con el renderizador Compose y pruebas comparadas; el contrato preparado no es una previsualización ejecutable de PC.
8. Evaluar autenticación criptográfica de paquetes si se necesita una lista de editores autorizados; esta versión solo comprueba integridad y declaraciones, no identidad del autor.

Criterios para desarrollarlo: exportar un paquete aceptado sin tocar Kotlin; instalar/actualizar en Android conservando una creación iniciada; rechazar una ruta peligrosa/imagen inválida/ID conflictivo; no publicar sin las revisiones requeridas. Todo ese desarrollo PC continúa PENDIENTE.
