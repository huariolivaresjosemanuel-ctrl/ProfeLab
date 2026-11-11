# Banco visual integrado — 10 de noviembre de 2026

Base remota de trabajo: 783d863f4173cd7cd0ebd5410662d721219122f8. El banco ya forma parte de los assets Android: 199 WebP, 27.540.172 bytes, con 229 relaciones verificadas. No hay que importar manualmente el ZIP editorial en la aplicación.

- Catálogo/ficha:20+20 imágenes asignadas por contentId y versión.
- Materiales:69 relaciones,45 identidades; compartir imagen no comparte existencias ni cantidades.
- Procedimiento:80 fotografías independientes asignadas por actividad y stepId, con huella del texto; N pasos genéricos.
- Resultados:20 referencias ilustrativas, separadas de observaciones y evidencias privadas.
- Guía docente:20 apoyos visibles en la presentación.

WorkshopBundledMedia separa carga/validación del catálogo y la UI. Verifica archivos en IO, conserva una caché de hechos físicos por hash y valida cada asociación con sus dimensiones, MIME, actividad, versión, material y paso. Una referencia inválida se omite de forma aislada y permite continuar con el estado alternativo. Las ventanas reutilizan WorkshopMediaSlot y conservan zoom, caché de bitmaps y portada privada explícita.

Los medios se proyectan en definiciones y overlays de solo lectura. No crean duplicados ni escriben en registros personales, inventario, reservas, observaciones o progreso. Una copia de versión desconocida o con un paso modificado no recibe por intuición la foto del procedimiento anterior. Room3, los respaldos, el ledger y los datos guardados mantienen sus contratos. No se requiere migración ni desinstalación.

Los estados PENDING de los manifiestos fuente se preservan como historia en BANCO_VISUAL_INTEGRADO.json. La orden explícita del usuario autoriza la integración editorial; no es una certificación de seguridad de materiales reales o de resultados. Los catálogos originales conservan sus advertencias y cantidades. Ninguna imagen se convierte en evidencia del usuario ni precarga medidas, conclusiones o alumnos.

El generador scripts/integrate_work_visual_bank.py reproduce esta selección desde el ZIP original y exige una aprobación explícita suministrada mediante argumento. Es una herramienta de esta entrega, no un nuevo importador de la aplicación. Para incorporar nuevas actividades compatibles se mantiene el importador versionado existente; no hay que crear nuevas pantallas. Cocina/FOOD y categorías incompatibles continúan bloqueadas.

La revisión local de archivos/imágenes y las pruebas Python tienen evidencias en el ZIP incremental. Cinco pruebas Android nuevas comprueban decodificación real del banco, resolución de los229 slots, fallos controlados, conservación de copias personales y las imágenes reales de mat-001 en Compose. Las capturas W00–W06 esperan al bitmap cargado. Su ejecución, compilación y comparación visual real requieren Android/Actions y no se declaran realizadas en este entorno.
