# Packs iniciales para ProfeLab

- `PACK_REFERENCIAS_MINEDU_V1.json`: **seis fichas verificables**, sin PDF binario. `embeddedPdf=false`. No inventar páginas, portada ni licencias; la app puede importar el PDF que el usuario haya obtenido legítimamente, con SAF.
- `PACK_TALLER_12_PROYECTOS_ORIGINALES.json`: **12 ideas originales de prueba** para el Taller, con área, grados sugeridos, materiales cuantificados y guía/checklist. No son plantillas oficiales MINEDU. Tiempos y correspondencia curricular se presentan como **sugerencias sujetas a validación docente**; no muestran certificación del Ministerio.

Para insertar nuevas versiones: verificar `schemaVersion`, `packId`, `version`, unicidad de IDs, tamaño de assets, referencias y derechos; modo `PREVISUALIZAR → AÑADIR/SUSTITUIR PACK` que nunca borre notas, favoritos, inventario ni proyectos personales. Una ficha documental se vincula al PDF local por `id`/URI privado, no por título solamente.

`MAPEO_FUENTES_A_MODULOS.json` indica para Codex qué material usar en cada feature, sin rellenar contenido inexistente.
