# PROMPT MAESTRO PARA CODEX — CONSTRUIR PROFELAB V1.0

## 0. TU ROL Y CONTRATO DE TRABAJO

Actúa como equipo senior integrado: arquitecto Android Kotlin, ingeniero OCR/PDF offline, desarrollador UX Jetpack Compose, especialista en persistencia SQLite/Room, QA, accesibilidad y seguridad de datos, además de asesor para flujos curriculares de Educación Primaria en Perú.

**Objetivo:** construir una aplicación Android funcional, offline-first, para estudiantes de docencia de primaria, practicantes y docentes, tomando como contrato visual cinco capturas aportadas por el usuario donde aparece la marca **ProfeLab**. El diseño visual está aprobado como referencia inicial. Tu tarea NO es producir solo una maqueta ni una pantalla Home: debes implementar los flujos y los datos de todos los módulos documentados, por hitos controlables y con pruebas.

**Paquete adjunto:** `PROFELAB_CODEX_MINEDU_SCANPRO_FUENTES_INTEGRADAS_V1_1.zip`. Lee primero `README_PRIMERO.md` y todos los documentos de `02_DOCUMENTACION/`, además de la matriz QA y las cinco imágenes en `01_REFERENCIAS_UI/`. No empieces a improvisar sin leerlos. El SDD y la especificación UX prevalecen sobre textos ficticios que se vean en las imágenes.

**Nombre de trabajo:** ProfeLab; no afirmar que está registrada ni que es producto oficial MINEDU.

### 0.1 Restricciones absolutas

- **GitHub SOLO LECTURA** para repositorios del usuario, especialmente Control Personal DNI y ScanPro. No hacer push, crear PR, merge, tags, releases ni mutar repositorios. Preparar código en copia local y entregar archivos ZIP con rutas completas e inventario. El usuario aplica cambios en GitHub y compila mediante GitHub Actions.
- Proyecto independiente, no reemplazar paquetes de Control Personal DNI ni alterar su appId, firma, JavaScript, licencias o compilación.
- No instalar Java, SDK, Gradle o Node en la PC del usuario ni prometer compilación local. Preparar workflow reproducible de GitHub Actions y pruebas de código disponibles sin toolchain; distinguir resultados ejecutados vs no ejecutados. No afirmar `APK compilada` sin evidencia real de Actions.
- Funciones básicas operativas sin internet; OCR funcionando desde primera instalación offline mediante **modelo empaquetado**, no descarga oculta. No añadir permisos innecesarios.
- **OBLIGATORIO:** desarrollar por módulos independientes y contratos versionados; jamás un `MainActivity` o archivo central que mezcle todos los módulos. Toda actualización debe preservar datos mediante migraciones no destructivas.
- **OBLIGATORIO:** importar posteriormente ideas, materiales, catálogos y PDFs MINEDU desde archivos elegidos por el usuario o paquetes versionados validados, sin reinstalar app ni sobrescribir sus creaciones.
- **OBLIGATORIO:** solicitar permiso CAMERA al usar escáner y RECORD_AUDIO al grabar; linterna con `hasFlashUnit()` + `CameraControl.enableTorch()` (sin permiso separado), Photo Picker/SAF para archivos y POST_NOTIFICATIONS solo si corresponde. Denegaciones no bloquean módulos restantes.
- No copiar código fuente, recursos ni assets de ScanPro sin autorización explícita del autor y validación de licencias; usar solo como inspiración funcional mientras ese permiso no conste. No integrar su ZIP en el proyecto automáticamente.
- No incluir PDFs de MINEDU en el APK, ni decir `CC BY 4.0` o `dominio público` por leerlos en un mockup, sin evidencia de licencia verificada y derechos de redistribución.
- No atribuir a MINEDU materiales o sesiones creados por la app. No introducir IA en línea o supuesta generación autónoma sin modelo real y permiso claro. No generar evaluaciones automáticas de menores.
- Usar **solo datos de muestra ficticios** en demos y QA, sin nombres de estudiantes reales. No incrustar capturas de referencia como fondos de pantallas finales; reproducir capas, contenido y navegación real en Compose.
- Ningún botón visual de pantalla final debe ser inerte o falso. Si una función no está disponible, ocultarla o mostrar estado `En preparación` explícito, y NO declararla terminada.

## 1. FUENTES OBLIGATORIAS DEL PROYECTO

Leer en este orden:

1. `02_DOCUMENTACION/SDD_PROFELAB_V1.md`: requisitos M01–M08, flujos FL01–FL08, 27 pantallas y estados.
2. `02_DOCUMENTACION/UI_UX_PROFELAB_REFERENCIAS.md`: diseño, componentes y contrato de botones.
3. `02_DOCUMENTACION/MODELO_DATOS_PROFELAB_V1.md`: entidades, vínculos, integridad y backup.
4. `02_DOCUMENTACION/ARQUITECTURA_Y_DECISIONES.md`: stack propuesto y alternativas.
4a. **OBLIGATORIO** `02_DOCUMENTACION/CONTRATOS_MODULOS_PERMISOS_Y_CONTENIDO.md`: límites entre features, expansión incremental de PDFs/ideas y permisos/linterna Android por uso.
5. `04_QA/MATRIZ_QA_PROFELAB_V1.csv`: casos de aceptación.
6. `05_DATOS_EJEMPLO/fixtures_profelab.json`: datos completamente ficticios para tests y previews.
7. Las cinco referencias visuales `01_REFERENCIAS_UI/01_INICIO.png`, `02_TALLER.png`, `03_SESIONES.png`, `04_CUADERNO.png`, `05_BIBLIOTECA.png`.
8. Referencias normativas/enlaces `06_FUENTES/FUENTES_Y_LICENCIAS.md`.

Los artefactos antiguos de `Aula en Mano` son borradores históricos; no sustituir el nombre visual, barra inferior ni layouts acordados ahora. Conservar cualquier regla útil que no contradiga la SDD V1 actual.

## 2. ALCANCE FUNCIONAL: CINCO TABS, TODOS COMPLETOS

### Inicio
- Saludo y perfil local; ciclo académico del estudiante distinto del grado de sus clases.
- Avance semanal real de tareas; Próximas entregas, Mi taller, Sesiones, Prácticas, Biblioteca MINEDU.
- Actividad reciente; lista editable de tareas pendientes con checks persistentes y deshacer.
- Campana: recordatorios locales reales. Avatar: Perfil/Ajustes. Estados vacíos honestos.

### Taller
- Biblioteca local de proyectos didácticos (propios o plantillas licenciadas), buscador, filtros por área, grado, dificultad, duración y materiales.
- Proyecto destacado con descripción, objetivo, foto autorizada/propia, lista de pasos y checklist.
- Inventario de cartón, tapas, tijeras, etc., cantidades, unidades y ajustes manuales. `Tengo / Necesito / Comprar`.
- **Recomendaciones tipo despensa:** `Qué puedo crear con lo que tengo`, basadas en cantidades/unidades del inventario; lista de faltantes. No inventar materiales, nunca descontar stock por marcar checklist; consumo solo con confirmación.
- Editor de proyecto, duplicar, favoritos, guardar Mis creaciones, adjuntar fotos, vincular material a sesión y exportar ficha.

### Sesiones
- Listado, crear, editar, duplicar, buscar, archivar; filtros grado 1–6, área, fecha, estado.
- Editor curricular: título, grado, sección, área, fecha, duración, propósito, competencia/capacidad/desempeño/fuente verificada o manual, evidencia, criterios, instrumento, secuencia Inicio–Desarrollo–Cierre, materiales y referencias.
- Fases editables con tiempos y advertencia por suma diferente de duración; guardar borrador sin rellenar datos desconocidos. Autoguardado y recuperación tras cierre.
- Vincular materiales del Taller, notas del Cuaderno y documentos de Biblioteca. Exportar/compartir borrador PDF tras preview, no fingir aprobación.

### Cuaderno
- Nueva nota, ideas, prácticas, reflexiones; etiquetas, curso/ciclo académico, búsqueda, adjuntos, duplicar, archivar y eliminar con confirmación.
- Práctica docente: objetivo, fecha, grado, institución opcional, observaciones, reflexión, actividades y evidencias sin exposición innecesaria de menores.
- **Foto/OCR** → cámara/galería → recorte manual/rotación → reconocimiento local → revisar imagen vs texto → corregir → guardar o insertar en nota, sesión o proyecto.
- **Grabar voz** → audio local + reproducción + adjunto; texto por dictado solo si se demuestra que motor STT es 100% local en dispositivo; no confundir SpeechRecognizer con grabación de voz.
- Portafolio: elegir evidencias vinculadas y exportar seleccionadas, con revisión previa.

### Biblioteca
- Catálogo de PDFs importados legítimamente; filtros MINEDU, Currículo, PDF, Favoritos; búsqueda, favoritos, metadatos verificables.
- Lector PDF offline con ir a página, zoom, marcador, continuar lectura, añadir nota y enlace a sesión/proyecto.
- Captura de página a OCR cuando PDF no tenga texto, con confirmación y límites de memoria.
- Fuentes MINEDU consultadas en el sitio oficial, sin redistribución automática ni valores falsos de año/páginas/licencia. Abrir enlace solo con acción explícita, fuera de flujos críticos offline.

### Herramientas transversales
- Perfil + Ajustes + Tareas + Mis creaciones + Backup/Restauración con checksum + Exportación + selector local de archivos + permisos por uso + estado offline.

## 3. UI/UX APROBADA

Recrear con componentes nativos (no fondo PNG) la estética de las 5 capturas: fondo crema, verde bosque, tarjetas de colores pastel suaves, fotografía de proyecto cuando sea legítima, tipografía legible, chips redondeados, iconografía coherente y ayudas escolares discretas. Navegación inferior EXACTA en orden:

**INICIO | TALLER | SESIONES | CUADERNO | BIBLIOTECA**

El usuario aprobó estas imágenes como punto de partida. No volver a un dashboard oscuro industrial ni reutilizar la app de seguridad. Los estados reales deben corresponder a datos locales, no a frases `3/5`, libros `132 páginas`, fotos o licencias de demo. En dispositivos pequeños priorizar legibilidad y accesibilidad; pantallas con scroll, botones ≥48dp, contraste verificable, tema oscuro opcional sin cambiar identidad de tema claro.

Usar layout adecuado a tablet 600dp+ (grillas y panel maestro-detalle) en lugar de estirar móvil. Validar con la Lenovo Tab Plus o emulador de tamaño comparable, si es accesible, y reportar pruebas no ejecutadas.

Diseñar y conectar las 27 pantallas del SDD y los estados de error/vacío/cargando/permiso denegado. Si una pantalla necesita subestado adicional, agregarlo al catálogo y a QA.

## 4. MOTOR OCR Y DIGITALIZACIÓN

Implementar usando un pipeline modular:

`Input (cámara/SAF/PDF) -> validación -> preview -> crop/rotate -> OCR local -> preview comparado -> edición manual -> confirmación -> guardado/vínculo -> exportación opcional`.

Requisitos:
- Preferir ML Kit Text Recognition v2 **bundled** para Latin, con dependencias fijadas y verificación first-run offline; alternativas Tesseract spa empaquetado solo si justificadas con pruebas y licencias.
- El API Document Scanner de ML Kit no tiene modelo bundled según documentación oficial; no usarlo como requisito del primer uso offline.
- Cámara CameraX con permiso solo al entrar; si se niega, selector de imágenes. No bloquear resto de la app.
- El OCR debe devolver texto editable, registrar imagen de origen y conservar texto bruto distinto del corregido; no inferir que OCR manuscrito es exacto.
- Ofrecer recorte, giro, reintentar, manejo de imagen borrosa o PDF protegido; evitar quedarse colgado en PDF multipágina.
- No guardar información sensible en Logcat ni enviarla a servidores. Retornar a módulo invocante conservando borrador.
- No crear automáticamente sesiones/cursos completos con texto OCR; insertar texto como referencia editable tras aprobación.
- Integración ScanPro: mantén interfaz del módulo OCR desacoplada para reemplazar opcionalmente pipeline de recorte más adelante si hay permiso y QA.

## 5. CURRÍCULO Y CONTENIDOS

Para grado de primaria 1–6 (no confundir con ciclo de universidad), aplicar fuentes oficiales:
- https://repositorio.minedu.gob.pe/handle/20.500.12799/4549
- https://www.minedu.gob.pe/curriculo/recursos.php
- https://repositorio.minedu.gob.pe/handle/20.500.12799/5310

Estos enlaces son referencia de estructura curricular y orientación, no autorización de redistribución. La V1 puede funcionar con campos manuales, catálogos locales verificados y plantillas **originales** con fuentes; no inventar una matriz oficial completa de competencias. Si aún no hay catálogo verificable, presentar `Agregar manualmente` y registrar origen.

Todo documento real debe tener metadatos comprobables (título, autor, año, fuente, derechos); el mockup no valida licencias, autores ni número de páginas.

## 6. ARQUITECTURA Y BASE DE DATOS

Preferencia inicial: Kotlin + Compose + Material 3 + Room + DataStore + CameraX + ML Kit bundled + PdfRenderer/lector PDF local revisado + SAF + FileProvider; capas `ui`, `domain`, `data`, `infra`, `ocr`, `pdf`, `backup`. Si propones otro stack, explica con evidencia comparativa antes de implementarlo.

Persistir sesiones, partes de sesión, materiales, inventario, checklist, notas, prácticas, portafolio, PDFs, posición de lectura, tareas, escaneos, adjuntos y respaldos, con UUID y migraciones. No guardar binarios gigantes dentro de DB; almacenar archivos privados, metadatos y hash. Borrados controlados con papelera cuando corresponda. Progreso de tareas calculado, no fijo.

Crear copias de seguridad ZIP con `manifest`, `schemaVersion`, JSON/archivos y SHA-256, validación antes de restaurar, operación atómica o reversión en caso de error, y preview al usuario antes de sobrescribir. No enviar automáticamente a nube.

Respetar requisitos de permisos, batería, API Android y almacenamiento por versión; justificar cada permiso en `AndroidManifest.xml`.

## 7. CONSTRUIR POR HITOS (NO TODO EN UNA ENTREGA OPACA)

### H0 — Auditoría inicial y trazabilidad
- Inventario de adjuntos/repo, READMEs/SDD/UI/QA; tabla requisito -> pantalla -> entidad -> test.
- Arquitectura final y decisiones de SDK/librerías; matriz de licencias; mapa de navegación; plan de código.
- No reescribir SDD sin registro de diferencias. Si algo es imposible con requisitos, presentar solución local que preserve el objetivo.

### H1 — Fundaciones Android e Inicio
- Esqueleto Android real compilable, workflow CI, Room/DataStore, navegación 5 tabs, tema ProfeLab, perfil, tareas, Inicio con estados y datos persistentes.
- Tests de arranque offline, pantallas y tareas.

### H2 — Sesiones
- CRUD completo, editor, timeline con tiempos, vínculo a materiales y notas, preview/export PDF, persistencia y pruebas.

### H3 — Taller
- Catálogo y editor de proyectos, inventario, recomendador local con lista faltante, checklist, creaciones, exportación, pruebas.

### H4 — Cuaderno, prácticas, portafolio, grabación de voz
- Editor de notas, adjuntos, tabs, registro de práctica, grabadora real local, selección de portafolio, exportación y pruebas.

### H5 — Biblioteca MINEDU y PDF
- SAF PDF, metadatos, lector, posiciones, bookmarks, favoritos, notas enlazadas y búsqueda; fuentes y licencias verificadas, tests de PDFs dañados.

### H6 — OCR transversal
- Cámara/importación/PDF, recorte, OCR bundled, revisión obligatoria, destino/contexto, cancelación, tests unitarios y físicos pendientes.

### H7 — Integración y QA final
- Consolidación, navegación, seguridad, accesibilidad, responsive móvil/tablet, respaldo/restore, tests negativos y CI. Corrección de errores sin romper funcionalidad.

Cada hito debe **entregar software real correspondiente**, testear lo ejecutable y actualizar matriz QA; si ambiente no permite pruebas, declarar pendiente. No marcar un hito completo por solo mostrar mockups. Evitar stubs en UI de funciones comprometidas de ese hito.

## 8. PRUEBAS Y CRITERIOS DE ACEPTACIÓN

Implementar pruebas unitarias + instrumentadas y plan manual según CSV adjunto. Requisitos de bloqueo:

1. App arranca en dispositivo limpio con modo avión, sin internet ni cuenta.
2. OCR está listo y reconoce texto impreso español desde primera instalación offline; no se considera apto solo por compilar.
3. Pantallas principales poseen funciones reales y guardan estado al cerrar/reabrir/rotar.
4. `Guardar/Compartir/Exportar` no pierde datos ni inventa PDFs/fuentes.
5. Inventario no se descuenta silenciosamente; guía checklist reversible.
6. Borrado de PDF o proyecto no destruye notas o vínculos accidentalmente.
7. La biblioteca conserva página de lectura real, importación y derechos verificados.
8. Restauración revalida hashes, versiones y adjuntos antes de sobrescribir.
9. UI legible en teléfono (390dp), tablet (>=600dp), horizontal y tamaño de fuente ampliado.
10. Sin registro/transferencia de datos sensibles no autorizada; no reutilizar proyectos del usuario.
11. Sin botones inertes; acciones inaccesibles se anuncian como pendientes, sin falsa habilitación.
12. Tests con fixtures ficticios; QA de cámara, OCR, microfono, PDF y restauración real marcados como prueba física cuando no ejecutados.

Estados QA: `PASS`, `FAIL`, `NO_EJECUTADO`, `REQUIERE_DISPOSITIVO`. Nunca dar `PASS` sin evidencia. Capturar logs depurados y método de reproducir. Si una regresión bloquea el flujo principal, detener entrega final, arreglar y repetir test; si falta dispositivo, entregar honesta y claramente pendiente, sin fingir resolución.

## 9. ENTREGABLES FINALES O POR HITO

Carpeta `PROFELAB_H{n}_ENTREGA/` con:

- `PROFELAB_H{n}_CODIGO.zip`: archivos fuente nuevos/modificados completos con rutas relativas desde raíz propuesta del nuevo proyecto.
- `INVENTARIO_ARCHIVOS.md`: tipo NUEVO/MODIFICADO, ruta exacta, explicación.
- `INSTRUCCIONES_APLICACION.md`: dónde subir, cómo crear proyecto/repo nuevo y compilar con GitHub Actions (sin modificar GitHub tú).
- `ARQUITECTURA_IMPLEMENTADA.md` y `DECISIONES_TECNICAS.md`.
- `MATRIZ_REQUISITOS_ESTADO.csv`: pantalla->acciones->datos->test->estado y pendientes.
- `RESULTADOS_QA.md`: comandos/CI comprobados, PASS/FAIL, evidencia y pruebas de dispositivo pendientes.
- `DEPENDENCIAS_Y_LICENCIAS.md`: versión, fuente, permisos, uso offline y licencias de código y contenido.
- `SHA256SUMS.txt` verificando archivos del ZIP.

Al completar H7: `PROFELAB_V1_COMPLETA.zip` y guía de instalación/uso, APK únicamente si se compiló realmente por Actions (no afirmar que existe por incluir un workflow).

## 10. ORDEN DE EJECUCIÓN INMEDIATO

Lee y valida todo el paquete, presenta resumen ejecutivo de arquitectura y trazabilidad y empieza H1 (fundaciones, navegación y datos locales). **No me hagas repetir los requisitos ya documentados**; usa defaults técnicos razonables y registra decisiones. No hagas cambios remotos ni ejecutes herramientas que modifiquen GitHub. Para cada hito produce artefactos concretos y solo declara terminadas funcionalidades realmente probadas. El objetivo final sigue siendo TODA la app V1, no una demo de Home.

**RESULTADO ESPERADO:** una ProfeLab real, usable offline, visualmente consistente con las imágenes aprobadas, cuyos módulos estén conectados y que permita al estudiante/practicante crear, consultar, escanear, revisar, guardar, recuperar y exportar su trabajo.

## 11. CONDICIONES ADICIONALES IRRENUNCIABLES — MODULARIDAD, CONTENIDO Y PERMISOS

Antes de H1, **lee íntegro** `02_DOCUMENTACION/CONTRATOS_MODULOS_PERMISOS_Y_CONTENIDO.md`, registra una matriz de dependencias y define los contratos entre features. El texto completo del anexo es parte obligatoria de este prompt.

**Secciones independientes:** Inicio, Taller, Sesiones, Cuaderno, Biblioteca, OCR, Tareas, Perfil y Respaldo se implementan separadas con pruebas independientes y sin estados globales mutables. La única APK integra las secciones mediante interfaces/versiones y navegación. Si cambias OCR o PDF, ejecuta las regresiones de Sesiones y Cuaderno; si cambias inventario, prueba Taller y vínculos existentes. No convertir todo a un solo archivo enorme.

**Escalabilidad de materiales y MINEDU:** crear `ContentPack`/`ContentPackItem`, importación local de JSON/ZIP seguro y PDFs sueltos; detectar duplicados, verificar metadatos/derechos, mostrar previsualización, aceptar/rechazar conflictos, escribir transaccionalmente. La actualización de un paquete no modifica contenido creado por el usuario; la app sigue funcionando offline incluso sin packs preinstalados. Nada de código externo ejecutable cargado dinámicamente.

**Android permisos por necesidad:** botón Escanear solicita cámara entonces, no al arrancar. Botón Linterna usa la cámara ya autorizada, verifica flash físico y apaga torch al cerrar/pausar/cancelar; sin permiso flash adicional. Grabación solicita micrófono entonces. Avisos solicitan POST_NOTIFICATIONS en Android 13+ si el usuario los habilita. Archivos por SAF y fotos por Photo Picker, sin acceso global a memoria. Si usuario deniega, revoca o dispositivo carece de cámara/flash, ofrecer alternativa de selección de archivos y conservar borradores; nunca pantalla negra ni cierre inesperado.

**No afirmar ausencia absoluta de bugs:** aplicar tests automáticos de cada feature, contratos, migraciones Room, integración, denegaciones, pantallas pequeñas/tablet y pruebas físicas. La matriz QA del paquete se amplió con casos `QA-088` en adelante, todos inicialmente sin ejecutar. Bloquear cierre si falla un P0, documentar evidencia y retests.

**Entrega adicional por hito:** `MAPA_MODULOS_Y_DEPENDENCIAS.md`, `CONTRATOS_API_Y_ESQUEMAS.md`, `REGISTRO_MIGRACIONES.md`, `MATRIZ_PERMISOS_ANDROID.md`, `CATALOGO_CONTENIDO_Y_PACKS.md`, `RESULTADOS_REGRESION_POR_MODULO.md`; no eliminar los entregables originales.


## 12. ANEXO V1 — FUENTES Y CONTENIDO YA INVESTIGADOS (OBLIGATORIO)

**Sustituye el trabajo de búsqueda inicial:** Revisa el nuevo `06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json` (6 fichas con títulos, autores, años, URLs oficiales, política de derechos), `06_FUENTES/MINEDU_BIBLIOTECA_E_INTEGRACION.md`, `08_CONTENIDOS_SEMILLA/PACK_REFERENCIAS_MINEDU_V1.json` y `PACK_TALLER_12_PROYECTOS_ORIGINALES.json` (12 ideas didácticas originales listas para parseo/importación). No vuelvas a buscar estos títulos salvo para confirmar enlaces o cambios de derechos.

- Los archivos **PDF originales no están incluidos** en el ZIP actual: descarga externa impedida por protección del sitio de origen. No fingir lo contrario. Para obtenerlos durante desarrollo, corre `python 07_SCRIPTS/descargar_minedu.py --descargar` desde un entorno con acceso oficial, inspecciona `ESTADO_DESCARGAS_MINEDU.json`; si bloquea, utiliza los enlaces registrados para descarga manual y `--validar-directorio`. No copiar un HTML de checkpoint con extensión PDF.
- Por ahora **la biblioteca integra las fichas offline** y deja que el usuario incorpore documentos obtenidos legalmente mediante SAF; NO incrusta PDFs restringidos en APK comercial. El CNEB y Programa Curricular contienen aviso reservado; las fichas de Guías 1-3 reportan CC BY 4.0 pero algunas portadas también dicen derechos reservados. Bloquear automatismo de redistribución hasta revisar esa contradicción.
- Adapta el catálogo a formato Room/importable packs versionados. El lector ofrece favoritos, buscar en títulos/metadatos, marcador, página y notas y está listo para mostrar PDFs asociados de manera local. No prometer texto completo buscable de documentos no descargados.
- Importa los 12 proyectos originales como semilla reversible con referencias propias, materiales cuantificados, checklists, no oficial MINEDU. Las ideas de muestra pueden editarse y ampliarse con nuevos packs sin migraciones destructivas.
- Lee `02_DOCUMENTACION/ANALISIS_SCANPRO_OCR_Y_REUTILIZACION.md`: existe `scanprov2-android.zip`, con `web/scanpro-web.zip` anidado, MainActivity Java y `scripts/fetch-ocr.sh`. NO copiar ni ejecutar el keystore `.jks` incluido en ZIP de tercero. No integrar fuentes con licencia no definida ni afirmar que el código web se auditó. Preferir ML Kit bundled para ProfeLab V1, contratos `OcrEngine` y `DocumentCapture` para posible sustitución con permiso.
- La QA se amplió a **134 casos**. Asegura test por licencia de PDF, archivo descargado real vs respuesta HTML, importación SAF, hash, packs originales, permiso, no perder favoritos ni notas, y modo avión.

**Estado de entrega ahora: paquete documental, packs JSON y downloader; no incluye PDFs MINEDU binarios, aplicación compilada, motor OCR de ScanPro ni APK.** Codex debe mantener esta distinción en informes.
