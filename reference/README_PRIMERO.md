# ProfeLab — PAQUETE MAESTRO consolidado V2 (29 octubre 2026)

**ESTADO:** documentación, cinco mockups aprobados, contenidos editoriales de muestra y PDFs de QA. **NO incluye APK funcional ni PDFs originales MINEDU.** No hay pruebas Android ejecutadas. Es la especificación más completa, no un código compìlado.

## IMPORTANTE: autoridad y continuidad
- Las **cinco imágenes exactas** de `01_REFERENCIAS_UI/` son el diseño **aprobado** de ProfeLab. Se conservan en 864×1536 px sin ediciones; ver `MOCKUP_GALERIA_APROBADA.html` offline.
- El diseño viejo de **Aula en Mano** se conserva en `11_HISTORICO_REFERENCIAS/` **solo por continuidad histórica**. Nunca lo priorices sobre ProfeLab.
- `02_DOCUMENTACION/SDD_MAESTRO_PROFELAB_V2.md` y anexos V2 mandan; la documentación V1.1 se conserva como historial y no autoriza contradicciones.
- **Prompt autorizado para Codex:** `03_CODEX/PROMPT_MAESTRO_CODEX_PROFELAB_V2_DEFINITIVO.md`. El prompt V1 sigue archivado, pero queda sustituido. GitHub del usuario **SOLO LECTURA**, nada de push ni commits remotos.

## Conteo verificable de entregables
- **5** capturas originales aprobadas (`01_REFERENCIAS_UI/`), más galería local.
- **46** filas de pantallas/estados/navegación y acciones (`02_DOCUMENTACION/MATRIZ_PANTALLAS_ACCIONES_V2.csv`). Las vistas secundarias son especificaciones, NO imágenes visualmente aprobadas.
- **12** registros oficiales MINEDU enlazados y anotados en JSON, sin binarios PDF originales. Estado jurídica de PDF `distribution_approved=false` hasta validación humana.
- **12** proyectos didácticos editoriales originales enriquecidos, con **12** miniaturas conceptuales propias.
- **8** experimentos originales de ejemplo con hipótesis, variables, pasos, seguridad y plantillas de observación. **Pendientes de revisión pedagógica** antes de publicación.
- **3** PDF originales de prueba (2 páginas de texto, 1 imagen escaneada, 1 con contraseña) + una imagen fuente, no oficiales.
- **191** casos QA **DEFINIDOS, NO EJECUTADOS** (134 históricos + 57 nuevos).

## Índice del ZIP

```text
01_REFERENCIAS_UI/          5 PNG aprobados + MOCKUP_GALERIA_APROBADA.html
02_DOCUMENTACION/           SDD_MAESTRO_PROFELAB_V2.md, 7 anexos V2, 46 UI y trazabilidad + documentos V1
03_CODEX/                   PROMPT_MAESTRO_CODEX_PROFELAB_V2_DEFINITIVO.md, MENSAJE_CORTO... + V1 histórico
04_QA/                      191 casos, metodología y matriz V1
05_DATOS_EJEMPLO/           fixtures V1, sin datos reales de estudiantes
06_FUENTES/                 12 registros MINEDU, notas derechos, enlaces, tutorial importación
07_SCRIPTS/                 scripts descarga históricos y validador PDFs manuales
08_CONTENIDOS_SEMILLA/      12 proyectos originales V1+V2, 8 experimentos, catalogo, manifiesto packs
09_PDF_PRUEBAS_ORIGINALES/  PDFs solo de QA y hashes
10_ASSETS_PROYECTOS_ORIGINALES/  12 PNG + galería y manifiesto
11_HISTORICO_REFERENCIAS/  prototipos antiguos Aula en Mano (no aprobados ProfeLab)
INVENTARIO_ARCHIVOS.csv    rutas, tamaño, hashes
SHA256SUMS.txt             checksums de archivos del proyecto (excluye el propio SHA256SUMS)
```

## Qué no puede interpretarse como completo
- El servidor MINEDU activó verificación anti-bot y el entorno no pudo descargar PDFs oficiales. **No se incluyen como binarios**, aunque existe investigación de autores, títulos y 12 páginas oficiales. No inventar accesibilidad offline. Ver `06_FUENTES/PENDIENTES_PDF_OFICIALES.md`.
- Un mockup con portada, año, número de páginas o licencia puede contener **información ficticia**. Codex debe leer el catálogo verificado, no copiar datos literales de una imagen.
- ScanPro del amigo: referencia técnica, **sin licencia de reutilización publicada**; no se incluye su código ni firma. DocReader/PDFBox son candidatos bajo revisión técnica/licencia.
- Ninguna afirmación psicológica individual ni los experimentos propuestos equivalen a diagnóstico o validación científica.

## Instrucciones de arranque

1. Abre `01_REFERENCIAS_UI/MOCKUP_GALERIA_APROBADA.html` para revisar imágenes.
2. Lee `02_DOCUMENTACION/SDD_MAESTRO_PROFELAB_V2.md` y el mapa UI.
3. Para Codex, adjunta el ZIP y copia el contenido de `03_CODEX/MENSAJE_CORTO_PARA_CODEX.txt`. El agente debe seguir el prompt maestro V2 y el SDD.
4. Implementación **por hitos H0..H7**; nueva app independiente; GitHub remotos solo lectura; archivos de código en ZIP con rutas exactas; compilar Android mediante GitHub Actions.
5. Los PDF originales del MINEDU, si se obtienen más tarde por descarga legítima, se validan con `07_SCRIPTS/validar_pdfs_minedu.py` y se importan, sin alterar la documentación ni obras originales.
