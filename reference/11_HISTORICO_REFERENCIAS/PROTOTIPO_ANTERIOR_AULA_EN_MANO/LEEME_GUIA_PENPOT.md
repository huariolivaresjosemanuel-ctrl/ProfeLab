# Aula en Mano — prototipo visual Fase 1 (Penpot)

**Estado:** maqueta para aprobación, no aplicación funcional. Sin JavaScript, fuentes externas, datos personales ni llamadas a internet. **No requiere usar ScanPro.**

## Archivos

- `01_PROTOTIPO_4_PANTALLAS_1880x1050.html` — **primer archivo recomendado**; cuatro pantallas móviles juntas.
- `02_INICIO.html` — pantalla por separado.
- `03_SESIONES.html` — pantalla por separado.
- `04_MATERIAL_DIDÁCTICO.html` — pantalla por separado.
- `05_ESCÁNER_OCR.html` — pantalla por separado.
- `06_REVISION_TEXTO_OCR.html` — estado adicional necesario para la seguridad del OCR.
- `07_TABLETA_INICIO_800x1140.html` — propuesta responsiva para tableta (pantalla Inicio).

## Importar en Ultimate HTML to Penpot

1. Abre Penpot → archivo de diseño → Ultimate HTML to Penpot.
2. Abre `01_PROTOTIPO_4_PANTALLAS_1880x1050.html` con Bloc de notas o VS Code, presiona `Ctrl+A` y `Ctrl+C`.
3. En `HTML or page URL`, pega **todo** el código. No pegues una ruta de disco ni una URL `file://`.
4. **Base URL vacío**; usa **una sola board Desktop de 1880 × 1050**; elimina Tableta y Móvil para esta importación.
5. `Run trusted page scripts`: **desactivado**. `Native flex layouts (experimental)`: **desactivado** para priorizar fidelidad de posiciones.
6. Pulsa `Analyze page`, comprueba el número de capas y que no aparezcan errores rojos; después `Import to Penpot`.
7. Amplía el zoom, expande `artboard` y `phone` en Capas y prueba cambiar textos/tarjetas.
8. Para el archivo 06 usa Desktop **520 × 1020**; para el archivo 07 usa Desktop **960 × 1280**.

## Compatibilidad del plugin

- Todos los font-weight usados son **400, 700 u 800**, válidos en Penpot; no se usa 850.
- Algunas combinaciones de esquinas redondeadas y bordes pueden dar **advertencias amarillas** por límites del plugin; no necesariamente bloquean importación.
- Si falla una pantalla al importar, usa su HTML individual y comparte captura del diagnóstico: corregiríamos **esa incompatibilidad específica**.
- Los botones y campos son **elementos estáticos de diseño**, no interacciones ejecutables.

## Estilo propuesto según SDD v0.1

- Fondo: `#F7F5F0`
- Superficie: `#FFFFFF`
- Texto: `#26333B`
- Primario azul petróleo: `#355E70`
- Acento cálido: `#E8AE69`
- Verde suave: `#82A995`
- Móvil: **390 × 844**; tableta: **800 de ancho**, adaptar en lugar de estirar.
- Navegación: **Inicio · Sesiones · Crear · Biblioteca · Más**.

## Revisión y aprobación antes de Codex

- ¿Es lo suficientemente adulto/profesional y escolar sin ser infantil?
- ¿Se identifican inmediatamente «Sesiones», «Materiales», «OCR» y «Biblioteca»?
- ¿Hay un botón evidente para empezar una sesión o una ficha?
- ¿Los filtros de sesiones ayudan a encontrar grado/área/estado? (Se debe diseñar estado completo en la Fase 2.)
- ¿Es obvio que el texto OCR se debe **revisar** antes de guardar?
- ¿Los campos y botones son legibles en teléfono y tableta?
- ¿Funcionaría sin conexión? (La implementación deberá probarlo; el mockup no lo demuestra.)

## Próximo hito después de aprobación

Diseñar las ocho pantallas móviles restantes (editor/vista previa de sesión, ficha paso a paso, Apuntes, Biblioteca, Lector PDF, Mis creaciones/Organización) y sus estados. Documentar componentes y tokens aprobados, y adjuntar exportaciones de Penpot al SDD antes de Codex.
