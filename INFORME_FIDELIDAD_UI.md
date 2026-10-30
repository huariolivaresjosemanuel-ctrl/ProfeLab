# Fidelidad a los cinco mockups

Autoridad visual: los cinco PNG originales, intactos en `reference/01_REFERENCIAS_UI/`. Se inspeccionaron visualmente los originales y la composición Compose. **No se renderizó la app Android, no existen capturas reales nuevas y no se acredita fidelidad exacta/píxel ni aprobación visual.** No se incrustan capturas como pantallas.

| Referencia | Correspondencia en fuentes | Diferencias y aceptación pendiente |
|---|---|---|
| 01_INICIO.png | HomeScreen.kt, Theme.kt, BrandArt.kt; saludo/ciclo, progreso, cuatro accesos y Biblioteca, actividad/pendientes. Se conserva la base H1. | Datos vacíos reales en lugar de 3/5 ficticio; avatar por inicial, no foto. Tamaños/tipografía/espaciado requieren captura real. |
| 02_TALLER.png | WorkshopScreen.kt; título, nuevo proyecto, ficha destacada con miniatura original, inventario, progreso de preparación de creación propia, catálogo y filtros desplegables. | Subapartados adicionales internos aprobados; inventario en texto en lugar de todos los pequeños pictogramas del PNG. Catálogo en tarjetas de lista, no cuadrícula compacta exacta. Checklist editable está en ficha; resumen muestra avance real. |
| 03_SESIONES.png | SessionWorkspace.kt; encabezado, grado/área/estado en filtros, resumen de sesión con Propósito/Evidencia/Criterios en pasteles, secuencia y exportación. | Inicialmente vacío sin sesión ficticia. Editor en formulario, filtros desplegables y plantillas temporales. Tarjetas pedagógicas apiladas; no reproducción exacta de toda la densidad del mockup. |
| 04_CUADERNO.png | NotebookWorkspace.kt; tres accesos Nueva nota/Grabar voz/Foto OCR, Apuntes/Prácticas/Ideas, notas, portafolio. | Sin reflexión ficticia pregrabada; institución opcional dentro de nota. Tarjetas y portafolio se dimensionan por contenido. Sin transcripción de voz. |
| 05_BIBLIOTECA.png | LibraryWorkspace.kt; búsqueda, MINEDU/Currículo/PDF propios/Favoritos, catálogo y continuar lectura. | No se reproducen portadas oficiales ni un PDF destacado ausente. Se indica Solo referencia; importar es explícito. Filas más amplias y acciones de derechos/favoritos visibles. |

La paleta marfil/verde petróleo/menta/coral/azul, navegación inferior con cinco pestañas en el orden aprobado, iconos Material y dibujos Canvas existentes se mantienen. Herramientas PDF/Cámara/OCR/Portafolio son rutas internas. No se añadieron cuentas, IA, anuncios o nube.

Adaptación escrita: Page limita ancho a 1000dp y permite desplazamiento; Inicio distribuye bloques en tablet; RecordEditor y revisión OCR usan dos columnas desde 700dp, y el lector añade panel de comentarios. No se ha validado Lenovo Tab Plus, orientación, fuentes grandes, TalkBack, contraste, presión de stylus ni interacción entre gestos y desplazamiento. El trazo registra puntos del puntero, sin presión/inclinación/palm rejection específicos. Estas diferencias no equivalen a aprobación visual del usuario.

Para cerrar la aceptación: capturar cada raíz vacía y con datos propios en móvil y tablet, comparar con el PNG correspondiente y ajustar medidas sobre la APK compilada. No usar fotomontajes ni capturas de la referencia como evidencia de ejecución.
