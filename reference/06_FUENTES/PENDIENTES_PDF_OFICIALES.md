# Pendientes de descarga legal de PDF MINEDU

No se incluyó ningún PDF MINEDU original: las páginas oficiales activan verificación anti-bot y el entorno de generación no tiene salida de red para descargarlos. Esto es una limitación **real**, no una tarea ejecutada. Para no frenar el proyecto:

1. Codex **no debe buscar nuevamente títulos o inventar rutas**: están en `CATALOGO_MINEDU_VERIFICADO.json` con los enlaces oficiales y notas jurídicas. Los seis iniciales incluyen `official_pdf_url` cuando fue localizado; otros solo `official_page`.
2. En navegador convencional abrir los registros enlazados, descargar manualmente cuando sea legal y guardarlos por `filename` declarado.
3. Ejecutar `07_SCRIPTS/validar_pdfs_minedu.py CARPETA_PDFS`: verifica cabecera, lectura, hash y vinculación por nombre. Requiere dependencias estándar de Python y opcionalmente `pypdf`; nunca imprime contenido protegido.
4. Actualizar solo el catálogo y `credits.json` cuando un responsable haya comprobado las licencias, los avisos del PDF y la redistribución dentro de una APK potencialmente comercial.
5. Si faltan PDFs: la Biblioteca debe funcionar con el catálogo y el importador, distinguiendo claramente `Disponible offline` y `Referencia: importar PDF`.
