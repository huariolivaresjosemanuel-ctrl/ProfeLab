# Cambios y pendientes V1.0.1

## Implementación en fuentes

- Taller construido con Compose: cabecera, búsqueda y filtros combinables, categorías originales, destacado con imagen, inventario horizontal, preparación real, recomendaciones por cantidades y cuadrícula adaptable. Permanecen las cinco pestañas globales.
- Fichas con presentación, galería y zoom, modos Práctico/Académico, materiales estructurados, preparación adulta, pasos persistentes, uso en aula, adaptaciones, seguridad, fuentes, evidencias y exportación por el motor existente. Se retira el editor de materiales crudos separado por barras.
- Inventario vacío inicialmente, CRUD con archivo recuperable, categorías y materiales propios, tres tipos de uso, reservas opcionales por proyecto, ajustes confirmados, motivo e historial; reversión del último ajuste compatible. No se descuenta stock al marcar pasos.
- Reglas offline compartidas: cantidades parciales, equivalencias m/cm, kg/g, l/ml, paquetes con contenido explícito, unidades incompatibles separadas, herramientas/reutilizables y advertencias independientes de disponibilidad.
- Compras consolidadas por proyecto sin acumulación al añadirlo otra vez, edición y exclusión de líneas, artículos manuales, adquirido separado de incorporado, comprobante idempotente y transacción de stock/lista. Una compra parcial limpia adquirido/override y recalcula el faltante.
- Copias propias y Mis ideas con UUID, formularios, pasos ordenables, borradores, versiones recuperables, duplicación de estructura y archivo confirmado. La duplicación reinicia avances, evidencias y resultados; no fabrica otra ejecución del experimento.
- Experimentos con pregunta/hipótesis/variables editables, observaciones fechadas, medición, vínculos de fotos, análisis/limitaciones/conclusión escritos por el usuario. Cámara/OCR se reutilizan mediante rutas existentes.
- Catálogo maestro ampliable de 45 materiales derivados de los nombres semilla; índice APK; cinco diseños configurables; contratos V2.1 y ampliación real del importador offline con imágenes/materiales/diseños. Editor PC solamente documentado.
- Mismo Room 2, esquema y migración 1→2; payload aditivo y preservación del original heredado. Sin instalaciones de herramientas y sin cambios remotos.

## Límites que siguen abiertos

- Compilación Kotlin, resolución Gradle, lint y ejecución de los 26 tests nuevos: pendientes. No existe evidencia de APK 1.0.1 generada en este entorno.
- Aceptación visual en dispositivo frente a las cinco PNG, rotación, letra grande, tablet, TalkBack y rendimiento con cientos de fichas: pendientes. No se afirma fidelidad píxel a píxel.
- Persistencia tras reinicio real/proceso terminado, actualización firmada desde V1.0, respaldo/restauración con nuevas imágenes y regresiones de PDF/cámara/OCR: pendientes de teléfono/emulador.
- Los ocho experimentos conservan su revisión pedagógica y de seguridad pendiente. Revisar una copia personal no publica ni acredita el catálogo.
- Los doce PNG originales son ilustraciones conceptuales. No existen fotos originales por paso ni imágenes específicas para los ocho experimentos; se muestra ausencia de imagen. No se descargaron sustitutos ni se inventaron fuentes MINEDU.
- No se suministran alternativas materiales «verificadas» inventadas. El contrato y selector requieren razón, fuente, factor positivo y material destino compatible. Los datos deben ser revisados por sus autores.
- La validación técnica de un ZIP y sus declaraciones de derechos no autentican a su autor: no hay firma digital de paquetes. El usuario revisa el origen y confirma la instalación. El futuro editor debe cerrar el circuito editorial antes de distribución.
- Archivos privados de versiones previas y evidencias desvinculadas se conservan. Una interrupción abrupta entre copiar imágenes e insertar registros puede dejar un archivo huérfano; no destruye el trabajo previo, pero no se implementó un recolector automático de huérfanos.
- No se entrega un editor visual PC ni un servicio online. Los diseños son cinco variantes del mismo conjunto de bloques Compose; no un motor HTML ni una herramienta de arrastrar elementos.

Estas limitaciones impiden certificar la aceptación final del Taller; no se sustituyen por resultados históricos ni por la mera presencia de clases o botones.
