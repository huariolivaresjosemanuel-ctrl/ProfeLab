# Manual UI/UX — capturas exactas aprobadas, mockups y reglas reales

**Las imágenes de `01_REFERENCIAS_UI/01_INICIO.png`, `02_TALLER.png`, `03_SESIONES.png`, `04_CUADERNO.png`, `05_BIBLIOTECA.png` son las **cinco referencias visuales aprobadas**. Cada una es una imagen compuesta con teléfono/fondo, por lo que debe usarse para reproducir el estilo, **no colocarse como bitmap que simule app**. No se deben usar portadas MINEDU ni fotos de personas tomadas de esas capturas como imágenes oficiales.

## Navegación inferior
1. Inicio: saludo y ciclo académico, progreso semanal real, entregas, accesos a Taller/Sesiones/Prácticas/Biblioteca, actividad reciente, pendientes.
2. Taller: búsqueda, filtros, proyecto destacado, cantidad de materiales, lista de preparación, recomendaciones; subapartados `Materiales`, `Experimentos`, `Mis ideas` y cada ficha con dos modos pedagógicos.
3. Sesiones: filtros de grado/área/competencia/duración; editor de propósito, evidencia, criterios, Inicio/Desarrollo/Cierre; recurso del Taller, exportación.
4. Cuaderno: Nueva nota/Grabar voz/Foto+OCR, Apuntes/Prácticas/Ideas, notas y portafolio.
5. Biblioteca: búsqueda y filtros MINEDU/Currículo/PDF/Favoritos, documento destacado, historial, continuar leyendo y lector anotable; nombres/páginas/licencias de las tarjetas se muestran solo si verificados.

## Design tokens iniciales sugeridos (ajustables tras revisión visual)
- Verde petróleo: `#175C47` o `#12553F`; fondo marfil `#FAF7F1`; verde menta `#E1EEE6`; coral claro `#FBE5DD`; azul pálido `#DFECF4`; amarillo papel `#FFF0D8`; texto `#23352E`.
- Margen horizontal 16dp en móvil; gap 8/12/16; radios 12–20dp; alto mínimo de toque 48dp; tipografía dinámica con accesibilidad.
- Ilustración escolar sutil, estilo adultos jóvenes; paleta no infantilizante. Logos/ilustraciones de capturas se vuelven assets originales de ProfeLab solo si permiso y propiedad verificados, en caso contrario reconstruir vector propio.
- Respetar insets de sistema, gestos, IME, Android 13+, modo oscuro si se habilita; en tableta usar ancho adaptable y doble panel PDF+notas/Editor+materiales.

## Mockups offline
`01_REFERENCIAS_UI/MOCKUP_GALERIA_APROBADA.html` muestra las 5 capturas originales en una galería local, sin conectividad. Es herramienta de referencia para Codex, **no mockup interactivo funcional**. Los HTML históricos de Aula en Mano en `11_HISTORICO_REFERENCIAS` NO son autoridad visual.

## Estados indispensables
- Sin datos (inicio y catálogo); carga local; no PDF descargado; archivo dañado; sin permiso cámara/micrófono; modo avión; fallo exportación; actualización contenido; resolución de conflictos; borrador restaurado; PDF protegido con contraseña; accesibilidad letra grande.
- Botones reales: no mostrar `Progreso 3/5` fijo ni foto de perfil falsa, no inventar años de documentos o porcentaje de licencia.

## Mapa funcional
CSV `02_DOCUMENTACION/MATRIZ_PANTALLAS_ACCIONES_V2.csv` especifica ruta, acción, módulo, estado vacío y criterio de aceptación para las pantallas principales y derivadas. Cada pantalla se realiza dentro de su feature; no implementar en un solo componente gigante.
