# Riesgos, acuerdos y pendientes explícitos antes de compilar

| Código | Riesgo real | Decisión obligatoria |
|---|---|---|
| R01 | PDFs oficiales no disponibles como binario en este paquete | Biblioteca muestra 12 fichas y `Importar PDF` con origen oficial; no mostrar `offline disponible` falso |
| R02 | Metadatos CC vs aviso editorial restrictivo | Por defecto `distribution_approved=false`; revisar cada PDF/legal antes de incluirlo en APK comercial |
| R03 | Repositoro ScanPro público sin licencia | Solo referencia, autorización escrita específica si se quiere integrar código; no copiar `app/scanpro.jks` |
| R04 | DocReader ejemplo expone esquema de firma inseguro en README | No copiar su firma/keystore; usar secretos Actions y política release de ProfeLab |
| R05 | Dibujo sobre PDF vs subrayado semántico | Dos herramientas UI diferenciadas y QA independiente; OCR del escaneo requiere corrección |
| R06 | `ACTION_VIEW` URI expira | Leer ahora temporal; guardar copia privada para anotaciones perdurables; error recuperable |
| R07 | PDF renderizado 500 páginas | Render por demanda, límites memoria, jobs cancelables, no rasterizar todo |
| R08 | Experimentos no revisados | Content `reviewStatus=PENDING`; demostración para equipo, publicación exige validación seguridad/pedagogía |
| R09 | Datos de niños/fotos | Anonimato por defecto, preview antes de compartir, no análisis clínico ni perfilado |
| R10 | ML Kit Latin bundled | Probar en modo avión primera ejecución, OCR de texto español y manuscritos limitados |
| R11 | Transcripción de voz | Grabación es función V1; voz→texto offline solo si motor real y QA, nunca fingida |
| R12 | Android 7+ pretendido | Compatibilidad real depende de motor PDF/CameraX/Compose; definir minSdk tras smoke tests |
| R13 | Actualizar packs/sesiones | Nunca sobrescribir datos personales, migraciones Room probadas y rollback |
| R14 | Imágenes de mockup contienen contenido ficticio | Reconstruir UI real sin incrustar capturas, fechas, nombres, licencias falsos |
| R15 | Ausencia de Internet | No descargar modelos OCR en runtime, no depender de MINEDU remoto, informar que PDF no importado no está offline |
| R16 | Copy-paste del diseño en código monolítico | Gradle feature modules, contratos API, tests por feature y dependencias acíclicas |

**Se conserva sin cambios:** cinco imágenes aprobadas, ZIP de ProfeLab V1.1 y archivos base dentro de este paquete, 134 pruebas previas, 12 proyectos originales, flujo GitHub solo lectura y compilación por Actions. Nuevas decisiones V2 sustituyen lo contradictorio, no borran historial.
