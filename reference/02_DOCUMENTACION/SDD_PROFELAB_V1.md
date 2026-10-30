# SDD — ProfeLab V1.0

**Estado:** especificación funcional para implementación gradual. **Anexo obligatorio P0:** `CONTRATOS_MODULOS_PERMISOS_Y_CONTENIDO.md` (feature-first, importación incremental de contenido, permisos por acción y linterna). **Diseño visual de referencia aprobado:** cinco imágenes de `01_REFERENCIAS_UI/`. No equivale a APK aprobada ni a validación en dispositivo.

**Fecha de documento:** 2026-10-29. **Usuarios:** estudiantes de Educación Primaria, practicantes y docentes en ejercicio en Perú. **Plataforma:** Android, teléfono y tableta. **Operación:** offline-first, sin cuenta ni internet obligatorios. **Nombre mostrado:** ProfeLab (nombre de trabajo inspirado en las referencias; verificar disponibilidad comercial antes de publicar).

## 1. Objetivo y límites

Unificar planificación de sesiones, elaboración guiada de material didáctico, inventario de materiales, apuntes y reflexiones de práctica, biblioteca de PDFs, escáner OCR, tareas y portafolio. Todo debe poder usarse sin conexión y guardar trabajos locales; el usuario decide cuándo exportar o compartir. Una función anunciada en pantalla no será decorativa.

La app **no representa al MINEDU**, no garantiza validez curricular, no reemplaza la valoración profesional ni genera contenidos supuestamente oficiales. No emplear un servicio IA en línea para funciones que se anuncien como offline. La ausencia de conexión no debe inutilizar el trabajo local. Los PDFs oficiales y su licencia deben verificarse antes de incluirlos en el APK.

### 1.1. Principios de producto
1. Capturar una idea en menos pasos que abrir varios programas.
2. Recuperar un borrador tras cierre inesperado.
3. Reutilizar contenidos entre módulos manteniendo original y procedencia.
4. Revisar siempre OCR y transcripciones antes de convertirlos en material final.
5. Diseño adulto, cálido y escolar, sin infantilismo ni sobrecarga visual.
6. Privacidad por defecto: documentos, fotografías y grabaciones únicamente en dispositivo y respaldos elegidos.
7. Tablet con aprovechamiento del espacio, incluyendo stylus donde haya soporte; no depender del lápiz.

## 2. Usuarios, alcance y vocabulario

- **Estudiante de docencia:** registra su ciclo académico (p. ej., 6.º ciclo) y las prácticas académicas.
- **Practicante:** agrega institución, aula, grado escolar, fechas y reflexiones, opcionalmente.
- **Docente:** planifica sesiones y reutiliza materiales.
- **Grado escolar:** 1.º–6.º de Educación Primaria. Nunca confundirlo con el *ciclo académico de estudios* del perfil.
- **Sección curricular:** áreas, competencias, capacidades, desempeños y estándares solo desde catálogo de procedencia verificable o entrada manual. Distinción expresa entre dato oficial y contenido creado por usuario.
- **MVP V1:** funciones nucleares completas descritas; funciones opcionales (dictado offline, OCR manuscrito, enlaces directos o avisos programados) se activarán solo si superan las pruebas y condiciones técnicas indicadas.

## 3. Navegación global aprobada (según imágenes)

**Barra inferior fija:** `Inicio` · `Taller` · `Sesiones` · `Cuaderno` · `Biblioteca`. Icono, etiqueta y estado activo. El tab activo se conserva al volver de un detalle. No se crearán tabs extra en la barra para OCR: se accede desde Cuaderno, Biblioteca, Taller y tareas contextuales.

**Cabecera:** marca ProfeLab, notificaciones locales (solo si existen), avatar/iniciales de perfil editables. Perfil, ajustes, respaldo e información sobre origen de documentos mediante menú accesible.

**Navegación:** tabs principales conservan su posición de desplazamiento cuando tenga sentido; subpantallas cuentan con Volver y navegación consistente. No usar gestos ocultos como única acción. En pantalla chica usar scroll; nunca recortar controles esenciales.

### 3.1. Cinco referencias visuales obligatorias

| Archivo | Pantalla | Elementos que preservar |
|---|---|---|
| `01_INICIO.png` | Inicio | Saludo contextual, perfil académico, avance semanal, próximas entregas, taller, sesiones, prácticas, biblioteca, actividad reciente y checklist de pendientes |
| `02_TALLER.png` | Mi taller | Buscador, chips por área/grado/material, proyecto destacado, imagen de ejemplo, inventario, checklist de preparación, otros proyectos, CTA Crear nuevo proyecto |
| `03_SESIONES.png` | Sesiones | Grado, área, competencia/capacidad seleccionable, duración, propósito, evidencia, criterios, secuencia Inicio–Desarrollo–Cierre, materiales, exportar borrador |
| `04_CUADERNO.png` | Cuaderno | Nueva nota, grabar voz, foto/OCR, pestañas Apuntes–Prácticas–Ideas, tarjetas de notas, portafolio |
| `05_BIBLIOTECA.png` | Biblioteca | Buscar, categorías MINEDU/Currículo/PDF/Favoritos, documento destacado, resultados, metadatos, continuar leyendo, guardar y añadir nota |

**Advertencia:** estas imágenes son maquetas, no pruebas de funciones ni de que las guías, fechas, páginas o licencias mostradas sean reales. No llenar la base con documentos ficticios presentados como oficiales. Texto/métricas de muestras son datos de demostración.

## 4. Requisitos funcionales por módulo

### M01 — Inicio
**M01.01** El saludo cambia según hora local sin requerir conexión; no inferir género ni nombre sin perfil. **M01.02** Mostrar ciclo académico configurado, no grado de clase. **M01.03** `Tu avance esta semana`: numerador = tareas elegibles completadas; denominador = tareas elegibles de esa semana; si denominador cero mostrar estado vacío, nunca inventar 3/5. **M01.04** `Próximas entregas` abre tareas ordenadas por fecha. **M01.05** `Mi taller`, `Sesiones`, `Prácticas` y `Biblioteca MINEDU` abren destinos reales. **M01.06** `Actividad reciente` muestra actividad persistida (sesiones, materiales, notas, documentos, prácticas). **M01.07** `Pendientes de hoy` permite marcar/desmarcar y mantiene estado al reiniciar; acciones deshacer. **M01.08** Notificaciones muestran tareas/recordatorios configurados localmente; sin simulación de push.

### M02 — Taller: proyectos y material didáctico
**M02.01** Buscador por título, área, grado, material disponible, duración, dificultad y etiquetas. **M02.02** Proyecto tiene objetivo de aprendizaje, grado, área, descripción, duración estimada (editable), dificultad, materiales/consumos con unidades, herramientas, costos opcionales, pasos numerados, fotografías propias, seguridad, producto esperado, fuentes y checklist. **M02.03** Nuevo proyecto desde plantilla local o vacío; guardar borrador y duplicar sin sobrescribir. **M02.04** `Ver guía` abre detalle con pestañas Vista general, Materiales, Pasos, Resultado. **M02.05** Lista de materiales admite `Tengo`, `Falta`, `Comprar`, cantidad/unidad y estado de preparación. **M02.06** Inventario de aula/taller es opcional y editable: cantidad disponible, unidad, costo, ubicación, fecha de modificación. **M02.07** Marcar pasos no consume existencias automáticamente; consumo solo por acción explícita con confirmación y transacción reversible. **M02.08** `Más proyectos` abre catálogo local con filtros, favoritos y Mis creaciones; no prometer contenidos ilimitados. **M02.09** Vincular un proyecto a una sesión mediante `Agregar a sesión` con confirmación. **M02.10** `Crear ficha imprimible` prepara PDF con datos, imágenes propias y crédito de fuente; vista previa antes de exportar. **M02.11** Ejemplos de referencia (máquina de sumar, tarjetas numéricas, ruleta de sílabas, geoplano, emociones) como plantillas propias originales y verificadas, sin copiar imágenes externas no licenciadas. **M02.12** `¿Qué puedo crear con lo que tengo?`: recomendar proyectos locales según inventario disponible: unidades compatibles y cantidades suficientes → estado `TODO DISPONIBLE`; faltantes → lista de compra editable; materiales sin equivalencia → `COMPROBAR MANUALMENTE`. Nunca inventar stock ni sustituir materiales peligrosos automáticamente. Ordenar primero por porcentaje de requerimientos satisfechos, luego grado/área/duración; explicar por qué se recomienda. Se puede usar también sin inventario: modo selección manual `Tengo / Necesito` por proyecto.

### M03 — Sesiones de aprendizaje
**M03.01** Listado: Nueva, Buscar, Filtrar (grado 1–6, área, fecha, estado, etiqueta), Ordenar, Duplicar, Archivar, Eliminar con confirmación, Recuperar desde papelera local. **M03.02** Editor con: título; fecha; grado; sección opcional; área; tiempo; situación significativa; propósito; competencia/capacidad/desempeño referenciados o escritos; evidencia; criterios; instrumento; atención a diversidad/adaptaciones; secuencia Inicio, Desarrollo y Cierre; materiales; referencias; reflexión docente opcional. **M03.03** Cada momento de la secuencia tiene duración independiente, descripción, estrategia y recursos; advertir si suma distinta del tiempo total, sin impedir guardar borrador. **M03.04** Campos de evaluación son editables; no generar calificaciones de menores automáticamente. **M03.05** Selección curricular etiqueta edición/fuente/año; no atribuir texto generado a MINEDU. **M03.06** Autosave en borrador de forma segura y visible; versiones anteriores o deshacer en edición importante; guardar también manualmente. **M03.07** `Exportar borrador` genera PDF legible (y opcional DOCX tras QA), sin inventar firmas, sellos o aprobación. **M03.08** Adjuntar notas de Cuaderno y proyectos del Taller con vínculos bidireccionales; copiar contenido si se solicita, sin sobrescribir original. **M03.09** Checklist de planificación y estados `BORRADOR / EN PROGRESO / REVISADA / ARCHIVADA`; estado revisado lo decide el usuario, no la IA. **M03.10** Modo lectura y tableta maestro-detalle.

### M04 — Cuaderno: apuntes, prácticas, ideas, evidencias
**M04.01** Crear nota con título, texto enriquecido simple (párrafos/listas, no editor HTML inseguro), etiquetas, curso, ciclo académico, fecha, adjuntos. **M04.02** Tabs `Apuntes` / `Prácticas` / `Ideas`, con búsqueda y filtros; nota puede etiquetarse sin duplicar. **M04.03** Práctica incluye fecha, institución, grado de primaria, objetivo, actividad, observaciones, reflexión, evidencias opcionales y próximos pasos; no registrar datos identificables de niños por defecto. **M04.04** `Grabar voz`: grabar audio local con permiso solo en uso, reproducir, pausar/detener, adjuntar nota; transcripción offline es opcional y condicionada a motor local real. Nunca enviar audio a nube sin consentimiento. **M04.05** `Foto/OCR` inicia captura o selector de imagen/documento; los resultados se envían a revisión antes de guardar. **M04.06** `Portafolio`: seleccionar notas, sesiones, proyectos, reflexiones y fotos autorizadas; exportar índice/paquete PDF local tras preview, sin incluir documentos privados por accidente. **M04.07** Etiquetas Curso / Reflexión / Idea / Pendiente y personalizadas. **M04.08** Papelera con restauración y eliminación definitiva confirmada.

### M05 — Biblioteca documental
**M05.01** Agregar documento mediante selector de Android (PDF local, opcional imagen) y guardar copia persistente en espacio privado de la app o permiso URI válido; detectar duplicados mediante hash, con opción de conservar ambos. **M05.02** Catalogar título, autor/emisor, año, fuente URL si existe, fecha de consulta opcional, derechos/licencia verificados, categoría (MINEDU, Currículo, PDF propios, Favoritos), etiquetas, área, grado. **M05.03** Lector PDF offline: paginación, zoom, ir a página, marcador, continuar lectura, notas vinculadas a documento/página. **M05.04** Búsqueda local: título/etiqueta siempre; búsqueda en contenido solo cuando el PDF tiene texto indexable o fue procesado con OCR y aprobado. **M05.05** `Continuar leyendo` usa última página persistida y distingue página real/total real, no un número de muestra. **M05.06** Fuente y derechos visibles: no poner `CC BY 4.0`, `Dominio público` ni `MINEDU oficial` sin verificación documental. **M05.07** Abrir biblioteca oficial por enlace requiere decisión del usuario e internet; lectura de archivos importados sigue offline. **M05.08** Guardar favoritas, notas y vínculos a sesión/proyecto. **M05.09** Al borrar el PDF, advertir vínculos y preservar las notas como notas independientes si el usuario lo solicita. **M05.10** Filtrar, ordenar por fecha real de importación/título/año y mostrar errores de PDF corrupto o contraseña.

### M06 — Escáner OCR transversal
**M06.01** Entradas: Cámara, galería/fotos, PDF local. Abrir desde Cuaderno, Taller, Sesiones o Biblioteca, preservando destino de retorno. **M06.02** Solicitar permiso CAMERA solo al abrir cámara; si denegado, ofrecer importar imagen. **M06.03** Captura con vista previa, control de enfoque, flash si disponible, aviso básico de borrosidad si implementado y comprobado. **M06.04** Recorte de esquinas, giro y corrección de perspectiva solo si se implementa con motor auditado; ofrecer recorte manual como respaldo. **M06.05** OCR *completamente local*, modelo disponible desde primera instalación sin descarga obligatoria; español latino (escritura latina) y cifras. **M06.06** Resultados: texto bruto OCR, bloques/orden cuando posible, imagen original, fecha y motor; mensaje visible `Texto reconocido: revise antes de guardar`. **M06.07** Vista de revisión permite edición, comparación con imagen, volver a recortar, seleccionar fragmentos, guardar y enviar a Nota / Proyecto / Sesión. **M06.08** No se genera actividad final ni se sobrescribe contenido hasta confirmación. **M06.09** PDF sin texto: renderizar página(s) seleccionadas a imágenes y aplicar OCR local con límites de páginas/memoria; barra de progreso y opción cancelar. **M06.10** Documentos manuscritos son *mejor esfuerzo*; indicar límites y permitir corrección. **M06.11** Manejar rotación, permisos revocados, error de memoria, página ilegible y cancelación. **M06.12** No registrar ni subir contenidos en logs/crash/analítica. **M06.13** Conservación y borrado explícitos de imagen fuente y texto OCR. **M06.14** OCR no interpreta el currículo ni produce automáticamente competencias a partir de texto.

### M07 — Organización y pendientes
**M07.01** CRUD de tarea con título, descripción, fecha/hora opcional, prioridad, estado y vínculo a sesión/proyecto/práctica. **M07.02** Filtrar `Hoy`, `Próximas`, `Vencidas`, `Completadas`; sin fecha queda `Sin fecha`. **M07.03** Avance semanal calculado de tareas reales, no de contenido demo. **M07.04** Recordatorios locales optativos con explicación de permisos de Android (incluido POST_NOTIFICATIONS donde aplique); la app funciona aunque se deniegue. **M07.05** No crear ni cambiar estados por horas del sistema sin registro claro; al cambiar zona horaria recalcular presentación, no perder fechas.

### M08 — Perfil, ajustes, seguridad y respaldo
**M08.01** Perfil: nombre/apodo opcional, institución/ciclo académico opcional, área principal, tema, avatar local opcional. **M08.02** Preferencias: claro/oscuro/sistema, tamaño fuente, orden de inicio, opciones de privacidad, OCR, exportación. **M08.03** Respaldo local manual: JSON estructurado (versionado) + archivos binarios necesarios, comprimido ZIP; manifest de hashes y esquema, vista previa al restaurar; contraseña/cifrado si implementado y verificado. **M08.04** Restauración: antes de sobrescribir, opción `Fusionar` o `Reemplazar` claramente explicada, transacción y rollback. **M08.05** Exportar una sesión/nota/proyecto/portafolio mediante Android Sharesheet o SAF, sin envío silencioso. **M08.06** Documentación de privacidad y licencia visible. **M08.07** Primer arranque con recorrido opcional; no forzar registro/cuenta.

## 5. Flujos completos y criterios de retorno

**FL-01 Sesión:** Inicio > Sesiones > Nueva > grado/área > propósito/evidencia/criterios > Inicio–Desarrollo–Cierre > enlazar materiales > Guardar borrador > Vista previa > Exportar. Volver y reiniciar mantiene borrador.

**FL-02 Taller:** Inicio > Taller > Buscar/filtro > Proyecto > Tengo/Falta > Pasos checklist > Adjuntar fotos > Marcar listo > Guardar Mis creaciones > Asociar a sesión. `Nuevo proyecto` va a editor, no a pantalla vacía sin persistencia.

**FL-03 OCR ficha:** Cuaderno > Foto/OCR > Cámara/Archivo > Recortar > OCR local > Revisar original/texto > Guardar como Nota. Si viene desde Sesiones, `Insertar en sesión` solicita confirmar destino; si se cancela no cambia la sesión.

**FL-04 Biblioteca:** Biblioteca > Importar PDF > Metadatos y fuente > Abrir > Guardar marcador/página > Añadir nota > Volver > Continuar lectura abre página conservada. Falta PDF: mensaje y opciones reconectar/eliminar entrada sin perder nota.

**FL-05 Dictado/voz:** Cuaderno > Grabar > Autorizar micrófono > Grabar > Detener > Guardar audio. `Transcribir` disponible solo si motor 100% local está instalado y probado; sin motor se mantiene audio y editor de texto manual.

**FL-06 Pendiente:** Inicio > Pendientes > Crear/Editar > marcar completado > aparece progreso recalculado > deshacer; persiste tras reinicio.

**FL-07 Respaldo:** Perfil > Exportar > selección de carpeta > ZIP completo y validado > Restaurar > vista previa > confirmar > recuento y vínculos consistentes. Sin borrar todo por error de archivo corrupto.

**FL-08 Prácticas:** Inicio > Prácticas > Nueva reflexión > campos > adjuntar evidencia con advertencia de privacidad > guardar > seleccionar para portafolio > exportar con preview.

## 6. Catálogo de pantallas, estados y botones

### A. Cinco pantallas raíz
1. `01_INICIO` — tarjetas clicables, tareas, actividad, campana con pendientes reales y avatar.
2. `02_TALLER` — filtros, buscador, proyecto destacado, inventario, checks, más proyectos.
3. `03_SESIONES` — filtros, editor/resumen de sesión y CTA Exportar borrador.
4. `04_CUADERNO` — Nueva nota, Grabar voz, Foto/OCR, tabs, notas, Portafolio.
5. `05_BIBLIOTECA` — búsqueda, filtro oficial/PDF/favoritos, destacado, lista, continuar leyendo.

### B. Detalles y utilitarios (mínimo)
6. `06_LISTA_SESIONES`; 7. `07_EDITOR_SESION`; 8. `08_VISTA_PREVIA_SESION`;
9. `09_CATALOGO_TALLER`; 10. `10_EDITOR_PROYECTO`; 11. `11_GUIA_PROYECTO`;
12. `12_INVENTARIO_MATERIALES`; 13. `13_LISTA_CUADERNO`; 14. `14_EDITOR_NOTA`;
15. `15_EDITOR_PRACTICA`; 16. `16_PORTAFOLIO`; 17. `17_PDF_LECTOR`;
18. `18_IMPORTAR_PDF_FUENTE`; 19. `19_OCR_CAMARA`; 20. `20_OCR_RECORTAR`;
21. `21_OCR_REVISAR_TEXTO`; 22. `22_TAREAS`; 23. `23_PERFIL_AJUSTES`;
24. `24_RESPALDO_RESTAURACION`; 25. `25_GRABADORA_AUDIO`;
26. `26_PREVISUALIZAR_EXPORTACION`; 27. `27_GESTOR_CREACIONES`.

### C. Variantes obligatorias
- `vacío`, `cargando`, `error`, `permiso denegado`, `sin conexión`, `sin PDF`, `PDF corrupto`, `almacenamiento lleno`, `OCR sin texto`, `OCR en revisión`, `guardado`, `confirmación`, `eliminación`, `restauración`.
- Tablet: `master-detail` para Sesiones/Cuaderno/Biblioteca, lector PDF con nota lateral si ancho útil.
- A11y: texto 200% y TalkBack; escala sin clipping, áreas táctiles >=48dp, contraste verificable y etiquetas no ambiguas.

## 7. Datos, integridad y persistencia

La implementación debe seguir `MODELO_DATOS_PROFELAB_V1.md`. Reglas de base:

- IDs tipo UUID estables; claves foráneas donde correspondan; migraciones versionadas.
- Borradores autosalvados con marca visible de `guardado`; transacciones para vínculos y restauración.
- Archivo binario en almacenamiento privado de la aplicación con nombre seguro y hash; tablas con metadatos; no guardar imágenes/PDF completos como BLOB en cada entidad.
- Estado y contenido derivados de datos reales; jamás hardcodear métricas demo en producción.
- Retención: borrado explícito y respaldos conscientemente administrados; política para archivos huérfanos tras adjuntos eliminados.
- Índice de búsqueda sin cargar PDF completo repetidamente a memoria.
- Fechas ISO, presentación local peruana y zona horaria correcta; no interpretar texto OCR como fecha confirmada sin edición.
- Entrada y adjuntos provenientes de SAF/cámara validados por tipo real, tamaño y errores de lectura; nombres saneados; compartir sin URIs abiertos permanentemente.
- Al restaurar, mostrar esquema, recuento, tamaños, duplicados y daños antes de aplicar.

## 8. Arquitectura técnica propuesta

**Preferencia:** Kotlin + Jetpack Compose + Material 3 adaptado a ProfeLab + Room + DataStore + CameraX + ML Kit Text Recognition v2 *bundled* para escritura latina (OCR español impreso). PdfRenderer o componente local con licencias compatibles; Android SAF, FileProvider para exportación; corrutinas y repositorios con ViewModels. Mínimo Android propuesto API 26 (verificar cobertura objetivo en fase de implementación); versiones definitivas fijadas con compatibilidad oficial y registradas. No incorporar dependencia JavaScript, internet, cuenta ni servidor a flujos básicos.

**OCR de ScanPro:** estudiar su enfoque e interfaz, pero no copiar código fuente del ZIP ni recursos/licencias sin autorización explícita del autor y auditoría de dependencias. La ausencia de LICENSE pública es un bloqueo para reutilización directa, no para inspirarse en funciones. Si se autoriza, cualquier integración debe superar pruebas de recursos, rendimiento y permisos.

**Recorte/corrección perspectiva:** implementar mínimo recorte y rotación manual local. Mejora automática basada en algoritmo licenciado/propio solo tras QA; no depender de Google Document Scanner si el primer uso debe ser totalmente offline. **Dictado voz:** no confundir API de grabación local con SpeechRecognizer general que podría usar servicios en línea. Comprobar disponibilidad on-device y modelos instalados; sin disponibilidad, grabar audio local sin prometer transcripción.

**Seguridad:** permisos cámara/micrófono solo al entrar; fotos de estudiantes y documentos de terceros requieren consentimiento y minimización; ninguna telemetría; sin publicar materiales privados automáticamente; configuración de backup cifrado deseable. Nunca mostrar datos personales reales en fixtures ni capturas de QA.

**Flujo de entrega:** proyecto independiente; GitHub solo lectura para repos existentes, no empujar, abrir PR ni modificar código remoto; entregas como ZIP de archivos con rutas exactas/inventario; aplicar cambios manualmente y compilar con GitHub Actions según flujo aprobado por el usuario. No instalar SDK/Gradle/Java local en la PC sin solicitud explícita. Reportar si no se puede ejecutar un tipo de prueba.

## 9. Fuentes curriculares y licencias

Fuentes a registrar con URL, fecha de consulta y versión exacta:
- Programa curricular de Educación Primaria: https://repositorio.minedu.gob.pe/handle/20.500.12799/4549
- Recursos del Currículo Nacional: https://www.minedu.gob.pe/curriculo/recursos.php
- Cartilla de planificación curricular: https://repositorio.minedu.gob.pe/handle/20.500.12799/5310

Solo distribuir documentos íntegros dentro del APK si los permisos de redistribución exactos están verificados; `acceso abierto` o `PDF disponible` no equivalen automáticamente a licencia comercial libre. Por defecto: catalogar enlaces oficiales y permitir importación local por el usuario; uso offline de documentos que el usuario haya importado. No copiar las etiquetas `CC BY 4.0`, años y páginas de los mockups como hechos.

La aplicación debe permitir al usuario editar plantillas para su contexto; ejemplos oficiales de MINEDU son materiales de referencia flexibles, no modelos obligatorios. No inventar un campo oficial cuando la fuente no lo contiene.

## 10. QA y bloqueo de aceptación

La suite detallada está en `MATRIZ_QA_PROFELAB_V1.csv`. Al menos:
- Inicio offline desde instalación limpia con modo avión, incluyendo OCR de foto impresa sin descarga inicial.
- Crear, editar, cerrar, reiniciar, rotar y recuperar datos por módulo.
- Subir imágenes dañadas/PDF protegidos, negar permisos, almacenamiento saturado.
- Ver PDF en tableta con posición de lectura; anotación por página preservada.
- OCR en español con diacríticos y ejercicios numéricos, revisión previa y jamás guardar sin confirmación.
- Vincular nota→sesión y material→sesión, editar o borrar origen y no romper integridad.
- Control de inventario no altera cantidades por marcar checklist.
- Restablecer respaldo tras desinstalación simulada en dispositivo de prueba; referencias y adjuntos completos.
- No publicar fuentes MINEDU sin licencia; no falsificar vínculos o sellos oficiales.
- Prueba accesibilidad en móvil/tableta y permisos Android 13+.

Cada prueba: `PASS / FAIL / NO EJECUTADA / REQUIERE PRUEBA FÍSICA`, capturas/logs sin datos personales y evidencia; NO confundir tests automáticos con prueba real de cámara/lector. Cualquier FAIL de pérdida de datos, red obligatoria para OCR, botón inerte en flujo principal o PDF falso impide declarar V1 completa.

## 11. Reglas de priorización, hitos y no incluidos

**H1 Fundación:** estructura, DB, navegación cinco tabs, perfil, backups básicos, UI con placeholders funcionales limitados a estados explícitamente no disponibles. **H2 Sesiones y tareas:** completo. **H3 Taller e inventario:** completo. **H4 Cuaderno/prácticas/portafolio:** completo. **H5 Biblioteca/PDF:** completo. **H6 Captura/OCR:** completo y QA offline. **H7 Integración, exportación y regresión.** En cada hito entregar ZIP, reporte QA e instrucciones.

**No incluidos en V1 sin fase nueva:** nube, login forzoso, chat IA generativa que prometa trabajar sin modelo, clasificación automática de niños, reconocimiento facial, evaluación automática, tienda, pagos, marketplace, reconocimiento infalible de manuscritos, sincronización multiusuario, voz a texto que requiera red. Dictado offline podría evaluarse como H8 opcional.

## 12. Condiciones de aprobación

Esta SDD documenta *lo deseado*, no afirma funciones ya implementadas. Solo implementar versiones o fases con pruebas y evidencia; la lógica prevalece sobre capturas ilustrativas. La decisión de stack se justifica en `ARQUITECTURA_Y_DECISIONES.md`; cambios de alcance deben registrarse antes de integrar.

## 13. Requisito transversal aprobado — módulos independientes, contenido ampliable y permisos Android

Es obligatorio aplicar integralmente `02_DOCUMENTACION/CONTRATOS_MODULOS_PERMISOS_Y_CONTENIDO.md` a los requisitos M01–M08 y los hitos H0–H7. Prevalece ante cualquier propuesta de código monolítico o permisos solicitados innecesariamente al iniciar. Los catálogos de proyectos, ideas y PDFs podrán crecer con importaciones individuales y paquetes versionados validados, conservando vínculos y trabajos de usuario. **Cámara** al escanear; **linterna** como capacidad de CameraX si hay flash (no es permiso runtime separado); **micrófono** al grabar; **notificaciones** si el usuario habilita avisos; **PDF/fotos** con selectores del sistema sin acceso general a almacenamiento. Toda ampliación y cambio por módulo debe pasar regresión y migración sin pérdidas.


## ANEXO NUEVO — Biblioteca MINEDU precatalogada y packs originales (29/10/2026)

La versión V1 trae un **catálogo local preconfigurado de seis documentos oficiales** (metadatos y enlaces, no archivos PDF binarios por el bloqueo de descarga y revisión de derechos) en `08_CONTENIDOS_SEMILLA/PACK_REFERENCIAS_MINEDU_V1.json`. Para el programador no hay investigación pendiente de títulos, URLs o categorías: `06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json` documenta cada referencia. La importación de PDFs originales por usuario se realiza con SAF y lectores offline. Se adjunta downloader/validador en `07_SCRIPTS/descargar_minedu.py`.

Asimismo, `08_CONTENIDOS_SEMILLA/PACK_TALLER_12_PROYECTOS_ORIGINALES.json` aporta **12 ideas originales** con área, grados sugeridos, cantidades y checklist, expresamente no oficiales. Integrarlas como pack versionado, no como tablas dispersas ni HTML fijo; conservan CRUD del usuario al actualizar.

`02_DOCUMENTACION/ANALISIS_SCANPRO_OCR_Y_REUTILIZACION.md` detalla módulos y arquitectura observados en GitHub, contratos de integración, restricciones de licencia y una alerta de keystore incluido en el ZIP del tercero. La V1 se basa en ML Kit OCR Latin bundled, con recorte independiente y revisión obligatoria. No copiar código de ScanPro sin autorización.

**Regla de publicación:** sin validar licencia y colofón, un PDF no se distribuye dentro de APK; las referencias y la importación de archivos propios sí están previstas.
