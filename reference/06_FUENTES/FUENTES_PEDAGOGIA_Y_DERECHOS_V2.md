# Fuentes y derechos — trazabilidad para ProfeLab

**Revisión:** 29/10/2026. Ningún archivo PDF MINEDU se ha descargado ni incluido como binario. No se inventan páginas, extractos ni licencias de las páginas ilustrativas. Las imágenes de UI son **mockups**, no prueba de licencia real para los documentos que muestran.

## Fuentes MINEDU — 12 fichas oficiales

El archivo `CATALOGO_MINEDU_VERIFICADO.json` contiene 12 documentos (seis conservados de V1.1 y seis añadidos), cada uno con ID, título, año, autor conocido, área, grados, enlace de registro y política jurídica. El repositorio responde actualmente con verificación de seguridad, que impide obtener los bytes PDF desde este entorno. **Todos** los documentos tienen `pdf_binary_included=false` y `distribution_approved=false` hasta revisión final. Ni `openAccess` ni una licencia mencionada en metadatos justifican automáticamente ignorar un aviso restrictivo contenido en el PDF. Nunca empaquetar en APK automáticamente.

**Licencias verificadas en metadatos oficiales:** Guías de alfabetización inicial 2025 y orientaciones 2026 reportan `dc.rights.uri` de CC BY 4.0; las guías tienen avisos editoriales contradictorios que deben resolverse. Los folletos Eureka 2024 y los programas socioemocionales III/V también informan CC BY 4.0 en fichas. La guía de Ciencia y Tecnología 2018 informa CC BY-NC-ND 2.5 PE y un aviso editorial restrictivo; **no** integrarla en una app de pago. Currículo y Programa Curricular requieren revisión/autorización para redistribuir texto completo, aunque pueden usarse como referencias de trabajo.

## Bibliografía externa de apoyo (NO PDFs incluidos)

1. CAST, UDL Guidelines 3.0: https://udlguidelines.cast.org/es/ — Diseño Universal para el Aprendizaje. Verificar permiso antes de copiar elementos gráficos/textuales.
2. Center on the Developing Child, Harvard: https://developingchild.harvard.edu/resources/handouts-tools/activities-guide-enhancing-and-practicing-executive-function-skills/ — actividades por edad, funciones ejecutivas. No presumir eficacia causal de una actividad individual.
3. NSTA, Safety and School Science Instruction: https://www.nsta.org/nstas-official-positions/safety-and-school-science-instruction — marco de seguridad escolar.
4. Ley y normas peruanas de protección de datos personales: consulta jurídica antes de tratar fotografías identificables de menores.

## Modelo de procedencia por afirmación pedagógica

`claimId`, `texto`, `tipo={hecho_comprobado|orientacion_oficial|propuesta_editorial|observacion_del_usuario}`, `sourceId`, `locationVerified?`, `confidenceNote`, `reviewedAt`, `reviewer?`. **No inventar página**; permitir `locationVerified=null`, etiquetar claramente `pendiente de revisión`. Las atribuciones se muestran por cada ficha y en un listado global; los enlaces originales no se eliminan.

## Ingesta de documentos

- Desde página oficial: registrar URL, título, derechos, checksum SHA-256 real y versión. Si no se descarga, estado `REFERENCIA_SIN_ARCHIVO` y botón *Importar PDF descargado*.
- Desde almacenamiento Android: `ACTION_OPEN_DOCUMENT`, `content://` y copia atómica a almacenamiento privado; no guardar la ruta pública como almacenamiento permanente. No requerir acceso masivo al almacenamiento.
- Para paquetes de biblioteca: manifiesto firmado o hash verificado, esquema semántico, cotejar origen/ID, tamaño y límites. La importación es transaccional y no sobrescribe datos del usuario.
- Si existe permiso verificable de redistribución, crear `credits.json` con título, autores, URL, licencia, fechas, cambios y atribución; separar la distribución del ZIP de investigación de la distribución futura en APK.

## Estado exacto

Incluidos: 12 fichas bibliográficas, enlaces oficiales, instrucciones de obtención y validación, PDF de prueba **originales** de ProfeLab. No incluidos: binarios PDF MINEDU; portada/página interior extraída de obras con derechos restrictivos.
