# Decisiones UX — Aula en Mano · Fase 1

Referencia: `SDD_APP_DOCENTES_PRIMARIA_v0.1.md`. **Nombre provisional.**

| Pantalla | CTA principal | Datos mostrados | Acción real que habrá que implementar después |
|---|---|---|---|
| Inicio | Continuar borrador | Herramientas, recordatorio, sesión reciente | Abrir borrador persistido sin internet |
| Sesiones | Nueva | Grado, área, fecha, estado, secciones completas | Crear sesión con guardado local y filtros reales |
| Material Didáctico | Crear nuevo material | Idea, progreso, lista de materiales, Mis creaciones | Plantilla editable con checks persistidos, fotos y pasos |
| Escáner OCR | Capturar documento | Cámara/selección, idioma y advertencia | Permisos, capturar/importar imagen, ejecutar OCR offline |
| Revisión OCR (estado) | Confirmar texto y guardar | Imagen fuente + texto editable y advertencia | Guardar únicamente después de revisión y confirmación |

La interfaz **no representa** integración con MINEDU ni garantiza aprobación curricular. Textos y contenido de ejemplo son ficticios. Nada se comparte sin intervención del usuario. No usar la librería o el código de ScanPro sin autorización y auditoría de licencias.

**Propuesta de componentes a crear en Penpot:** AppBar, NavItem, TileAcceso, SessionCard, MaterialGuideCard, CheckboxItem, StatusChip, PrimaryButton, SecondaryButton, SearchField, OCRCameraFrame, OCRReviewPanel, EmptyState, ConfirmDialog.

**Aprobaciones solicitadas:** paleta, títulos, tamaño de tarjetas, navegación, escáner/confirmación de OCR, tablet responsiva, y nombre comercial.
