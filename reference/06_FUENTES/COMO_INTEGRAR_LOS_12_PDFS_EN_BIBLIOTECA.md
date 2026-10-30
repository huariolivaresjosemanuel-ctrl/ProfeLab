# Integración de las 12 fuentes MINEDU sin investigación duplicada

1. Revisar `CATALOGO_MINEDU_VERIFICADO.json`: contiene las 12 fichas, enlaces oficiales completos (`official_page`) y, donde se dispone, URL PDF (`official_pdf_url`).
2. Los PDFs binarios **no están incluidos**: el repositorio activa verificación anti-bot, y el entorno que creó el paquete no cuenta con conectividad de descarga. No inventarlos ni simular que están en assets.
3. El usuario puede descargar legalmente un PDF desde su navegador convencional y abrirlo mediante `Abrir con ProfeLab` o importarlo desde Biblioteca. La aplicación copia el documento a su almacenamiento privado si elige guardar y lo asocia a un `sourceId` por selección/confirmación explícita, checksum y metadatos.
4. Codex: no necesita re-investigar títulos, autores o handles. Debe implementar inicialmente el catálogo **sin bytes** (`REFERENCIA_SIN_ARCHIVO`). Asegurar que se puede importar PDF individual y buscarlo offline después de guardado.
5. Para futuros packs de distribución: revisar el PDF original y licencia de su contenido y materiales incluidos, crear una aprobación documentada y `credits.json`; por defecto `distribution_approved=false`. No copiar portadas/ilustraciones del mockup como oficiales.
6. Validar PDF manual descargado: `python 07_SCRIPTS/validar_pdfs_minedu.py CARPETA_PDFS` (hash y formato; derechos requieren inspección humana).
