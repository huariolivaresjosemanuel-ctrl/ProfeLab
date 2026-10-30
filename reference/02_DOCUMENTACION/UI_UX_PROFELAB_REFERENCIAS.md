# UI/UX ProfeLab V1 — CONTRATO VISUAL PARA CODEX

## 1. Imágenes fuente: visual de referencia aprobado

No sustituir las cinco imágenes por los antiguos mockups de «Aula en Mano». La marca visible en la referencia es **ProfeLab**; se adoptará como nombre de trabajo para la UI. Las cinco imágenes de `01_REFERENCIAS_UI/` provienen del usuario. Son **mockups**, no código reutilizable ni demostración de sus funciones.

| Imagen | Archivo | Tab | Prioridad visual |
|---|---|---|---|
| Bienvenida, pendientes, accesos | `01_INICIO.png` | Inicio | Navegación, resumen semanal y tarjetas |
| Máquina de sumar, inventario, pasos | `02_TALLER.png` | Taller | Fotografía del proyecto, chips, guía/checklist |
| Clase de matemáticas con propósito/evidencia/criterios | `03_SESIONES.png` | Sesiones | Estructura curricular en tarjetas |
| Apuntes, prácticas, grabadora, foto/OCR | `04_CUADERNO.png` | Cuaderno | Captura rápida y organización de notas |
| Buscador y tarjetas de PDFs | `05_BIBLIOTECA.png` | Biblioteca | Metadatos correctos, lector y continuar lectura |

## 2. Design tokens sugeridos (aproximados a la estética de las imágenes, no muestreados por software)

```yaml
brand: ProfeLab
fonts:
  primary: "Inter or system sans"
  weights: [400, 500, 600, 700]
colors:
  background: "#F8F5EE"   # crema cálido
  surface: "#FFFFFF"
  surfaceMint: "#E7EEE3"
  greenPrimary: "#135B46"
  greenDeep: "#153E35"
  greenMuted: "#7DA992"
  textStrong: "#1D3035"
  textSecondary: "#647170"
  accentPeach: "#F4D5C9"
  accentBlue: "#D9EAF2"
  accentAmber: "#F5E4C7"
  border: "#E7E4DC"
  danger: "#B34B45"
spacing: [4, 8, 12, 16, 20, 24, 32]
radius:
  card: 18
  button: 16
  chip: 100
sizes:
  touchMinDp: 48
  phoneDesignWidthDp: 390
  tabletBreakpointDp: 600
  cardInsetDp: 16
  navTabs: 5
```

Tokens sujetos a ajuste en dispositivos físicos para contraste y accesibilidad. Evitar tipografía diminuta del mockup al adaptarla a un equipo real. No usar pesos 850 que el importador anterior de Penpot rechazaba. Íconos vectoriales coherentes; adornos sin impedir lectura de encabezados.

## 3. Jerarquía visual y componentes

- `TopAppBarProfe`: logo libro + hoja, título ProfeLab, campana de avisos locales, avatar de perfil con iniciales si no hay foto.
- `BottomNavProfe`: 5 ítems Inicio, Taller, Sesiones, Cuaderno, Biblioteca, selección verde + subrayado; áreas ≥48dp.
- `GreetingHero`: saludo y subtítulo de ciclo académico, adorno leve de lápices/plantita.
- `ProgressCard`: numerador real de pendientes, barra de avance, CTA abre lista.
- `SoftActionCard`: fondo pastel según categoría, ícono, descripción, chevron, acción de navegación.
- `ProjectHeroCard`: imagen de material autorizada/propia, título, objetivo, dificultad, duración, grados, CTA Ver guía.
- `InventoryMiniCard`: ícono/miniatura, nombre de insumo, cantidad/unidad real.
- `TaskChecklist`: checks persistentes, progreso calculado y deshacer.
- `SessionSectionCard`: Propósito, Evidencia, Criterios, Secuencia, Materiales; editar cada sección.
- `TimelineMoment`: Inicio/Desarrollo/Cierre, minutos, acción editar, total visible.
- `NotebookTypeCard`: apuntes / prácticas / ideas; crear nota, grabar audio o OCR.
- `DocumentRow`: ícono PDF, título, descripción, fecha/documento/autor verificable, etiqueta de licencia solo cuando se conoce.
- `ReaderContinueCard`: portada solo si el PDF se importó, última página y CTA Abrir.
- `DocumentScanCapture`: foto/importación, marco de recorte; no afirmar que es 'escaner oficial'.
- `OcrReview`: dos modos Imagen/Texto editable, aviso de revisión, botones Repetir OCR / Guardar / Insertar.
- `EmptyState`, `ErrorState`, `PermissionDenied`, `Loading`, `ConfirmDialog`, `BackupProgress`.

## 4. Reglas responsivas

- Android 360–430dp: columna única, barra inferior 5 tabs, tarjetas con contenido ajustado; controles >48dp y textos ≥14sp principales.
- Tablet 600dp+: grillas 2–3 columnas y panel maestro-detalle en biblioteca/sesiones/cuaderno. Si vertical, listas a la izquierda y detalle a la derecha cuando haya espacio real.
- Tableta con lápiz: permitir anotación sobre imagen/nota si se incorpora; no depender del stylus para ninguna función. No declarar trazos vectoriales como soportados sin implementarlos.
- Ventana redimensionable, rotación y teclado de pantalla: formularios permiten scroll, no se pierde borrador al rotar.
- Respeto de barras de sistema, navegación gestual, recortes y safe area.
- Tema oscuro opcional con tokens separados; los cinco mockups son referencia principal de tema claro.

## 5. Contrato de botones (no dejar «clickables» sin destino)

### Inicio
- Campana → listado local de recordatorios; sin recordatorios muestra vacío.
- Avatar → Perfil y Ajustes.
- Tarjeta Avance semanal → Tareas filtradas a la semana; avanza solo con tareas completadas.
- Próximas entregas → Tareas próximas.
- Mi taller → Tab Taller; Sesiones → Tab Sesiones; Prácticas → Cuaderno/Prácticas; Biblioteca MINEDU → Tab Biblioteca filtrada a categoría MINEDU.
- Actividad reciente → historial de entidades abiertas/editadas con botón Ver todo y acceso al elemento.
- Checklist Pendientes hoy → alterna completado/deshacer con guardado persistente.

### Taller
- Nuevo proyecto → editor de proyecto nuevo.
- Buscar → filtrar catálogo local; icono filtros abre panel múltiple, chips aplican filtros reales.
- Proyecto destacado / Ver guía → detalle de proyecto.
- Marcador → favoritos.
- Gestionar inventario → inventario editable, entradas/salidas manuales.
- Checklist Preparación → marca tarea del proyecto, sin descontar stock.
- `¿Qué puedo crear con lo que tengo?` → recomendaciones deterministas de proyectos según cantidades disponibles y lista de faltantes; no inventar inventario.
- Más proyectos / Ver todos → catálogo y Mis creaciones.

### Sesiones
- Grado, Área, Competencia/Capacidad y Duración → selectores y editor; datos oficiales claramente identificados.
- Propósito / Evidencia / Criterios → editor de sección con persistencia.
- Inicio/Desarrollo/Cierre → editor de etapa con minutos y materiales.
- Materiales sugeridos → lista de materiales vinculados; agregar o quitar sin borrar proyecto.
- Exportar borrador → vista previa y compartir/exportar PDF; nunca exportar un ejemplo fijo.

### Cuaderno
- Nueva nota → editor con autoguardado.
- Grabar voz → solicita permiso y pantalla de grabación local; transcripción solo si offline disponible.
- Foto/OCR → abrir captura/importación; pantalla revisión obligatoria.
- Apuntes/Prácticas/Ideas → filtrar lista por tipo.
- Menú ⋮ de nota → editar, duplicar, archivar, eliminar con confirmación.
- Portafolio / Agregar evidencia → selector de ítems e importación de adjuntos; exportación revisada.

### Biblioteca
- Buscar → filtrar título, autor, metadatos y texto indexado disponible; botón de filtros.
- MINEDU, Currículo, PDF, Favoritos → categorías reales, no cambiar etiquetas de fuente por diseño.
- Documento destacado / flecha → lector PDF si archivo local; si no está descargado, mostrar acción explícita para importarlo.
- Más recientes → ordenar por importación; otras opciones título/año.
- Continuar leyendo / Abrir → abrir última página almacenada.
- Guardar → favorito; Añadir nota → nueva nota enlazada a documento y página.
- Etiquetas CC: solo mostrar si está respaldado por metadata verificada.

## 6. Estados: solo texto real y sin fingir integración

- Demostración/fixture: nombres ficticios; overlay opcional `Datos de muestra` hasta que el usuario cree contenido real.
- Si no existen tareas, mostrar `No hay tareas esta semana`; nunca `3/5` fijo.
- Si no hay proyecto importado, mostrar proyecto de demostración con etiqueta de muestra o catálogo propio autorizado.
- Si no hay PDF local, mostrar `Importar documento` y/o `Abrir fuente oficial en navegador` explícitamente; nunca generar portada, año ni número de páginas falsos.
- Si se deniega cámara/micrófono, mostrar alternativas.
- Si OCR no reconoce texto, dar repetir, ajustar recorte o escribir manualmente.
- Si no hay motor STT offline disponible, ofrecer grabadora local; no simular transcripción.

## 7. Handoff a Codex desde Penpot

- Guardar imágenes de referencia sin incluirlas como fondo estático de la UI final.
- Entregar capturas/exportación real de las mesas de trabajo editables, tokens y nombres de componentes.
- Asociar cada pantalla a un ID en `SDD_PROFELAB_V1.md`, y cada botón a su contrato anterior.
- Un diseño no sustituye las reglas curriculares ni los permisos; Codex debe mostrar evidencia de funcionamiento real.
