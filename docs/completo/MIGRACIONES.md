# Persistencia y migración de la versión integrada

Base H1: Room1, `tasks`, `profiles`, `drafts`, `activity`. Se conserva su esquema original en `core/database/schemas/pe.profelab.core.database.ProfeDatabase/1.json`.

Room2 añade exclusivamente:

```sql
CREATE TABLE IF NOT EXISTS workspace_records (
 id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, title TEXT NOT NULL,
 json TEXT NOT NULL, updatedAt INTEGER NOT NULL,
 revision INTEGER NOT NULL, archived INTEGER NOT NULL
)
```

`ProfeDatabase.MIGRATION_1_2` está registrada con `addMigrations`. No borra/recrea las tablas H1 ni usa fallback destructivo. No se cambió el nombre `profelab.db`. Profile/ciclo sigue separado del grado escolar. Preferencias siguen en DataStore.

WorkRecord/WorkspaceStore forman el contrato nuevo entre features; Room solo aparece en core/database. JSON por kind representa sesiones/proyectos/experimentos/notas/inventario/documentos/marcas/portafolios/versiones/packs. Es un compromiso de implementación: las entidades nuevas no tienen cada una su tabla tipada del SDD. Para futuras evoluciones se debe versionar cada payload, tipar validadores y crear migraciones explícitas sin perder IDs.

`saveRecord` escribe registro/actividad en transacción, historia muestreada cada20 cambios y versiones explícitas. Archivar conserva registros y archivos; quitar vínculo no borra el origen. Plantillas de assets inmutables, copias con UUID nuevo; catálogo importado separado por prefijo pack.

La comprobación Python ejecutó SQL del esquema1 y la migración, manteniendo tarea/perfil. MigrationTest Android escrito usa SQLite y centinela, aún sin ejecutar. Falta MigrationTestHelper y aceptación de actualización real. Se recuperó el `2.json` generado realmente por KSP en el intento17 (commit9c2a09cc0d5651b9eca36f61c4bfcddbee05d4b3), artefacto informes-V1.0-17. Se incluye sin alterar su identityHash; evidencia en docs/qa/correccion-ci-run17/evidencia.json. Ese intento falló después/en paralelo en core:files; schema generado no prueba migración. El workflow publica `core/database/schemas/**`; cotejar también en la siguiente compilación exitosa. No sustituir schema1 por2 ni editar su identidad a mano.

Backup no copia SQLite abierto: toma las cinco tablas en transacción y exporta JSON+hashes. Restauración fusión `INSERT OR IGNORE`, columnas permitidas y validación acotada; IDs existentes prevalecen. Copias de archivos con conflicto de hash se rechazan. Rollback de DB y limpieza de nuevos archivos ante excepción; no atomicidad de filesystem frente a proceso abruptamente terminado. No descifrar ni leer datos de otras aplicaciones.
