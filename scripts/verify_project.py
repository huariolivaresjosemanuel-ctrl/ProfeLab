"""Offline structural checks. Does not compile Kotlin, run Room or test Android UI."""
from pathlib import Path
import csv, hashlib, json, re, sqlite3, sys, tempfile, unittest, xml.etree.ElementTree as ET, zipfile

ROOT=Path(__file__).resolve().parents[1]
def read(path): return (ROOT/path).read_text(encoding='utf-8')
def rows(path): return list(csv.DictReader((ROOT/path).open(encoding='utf-8-sig',newline='')))

class DeliveryChecks(unittest.TestCase):
    def test_sixteen_modules_and_acyclic_dependency_graph(self):
        modules=re.findall(r'include\("(:[^\"]+)"\)',read('settings.gradle.kts'))
        self.assertEqual(16,len(modules))
        graph={}
        for module in modules:
            folder=module[1:].replace(':','/')
            build=read(folder+'/build.gradle.kts')
            graph[module]=re.findall(r'project\("(:[^\"]+)"\)',build)
            for dependency in graph[module]:
                self.assertIn(dependency,modules)
                if module.startswith(':feature:'): self.assertTrue(dependency.startswith(':core:'))
        def visit(module,stack):
            self.assertNotIn(module,stack)
            for dependency in graph[module]: visit(dependency,stack+[module])
        for module in modules: visit(module,[])

    def test_no_feature_imports_another_feature_or_database(self):
        for source in (ROOT/'feature').rglob('*.kt'):
            owner=source.relative_to(ROOT/'feature').parts[0]
            text=source.read_text(encoding='utf-8')
            for imported in re.findall(r'^import pe\.profelab\.feature\.([a-z]+)',text,re.M): self.assertEqual(owner,imported)
            self.assertNotIn('import pe.profelab.core.database',text)

    def test_android_manifest_is_independent_and_has_only_contextual_permission(self):
        manifest=ET.fromstring(read('app/src/main/AndroidManifest.xml'))
        ns='{http://schemas.android.com/apk/res/android}'
        permissions={p.attrib[ns+'name'] for p in manifest.findall('uses-permission')}
        self.assertEqual({'android.permission.POST_NOTIFICATIONS'},permissions)
        self.assertEqual('false',manifest.find('application').attrib[ns+'allowBackup'])
        self.assertIn('applicationId = "pe.profelab.app"',read('app/build.gradle.kts'))
        self.assertNotIn('android.intent.action.VIEW',read('app/src/main/AndroidManifest.xml'))
        for xml in (ROOT/'app/src/main/res').rglob('*.xml'): ET.parse(xml)

    def test_all_twelve_minedu_references_remain_without_pdf_bytes(self):
        source=json.loads(read('reference/06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json'))
        bundled=json.loads(read('core/content/src/main/assets/catalog/minedu_v2.json'))
        self.assertEqual(source,bundled)
        docs=bundled['docs']; self.assertEqual(12,len(docs)); self.assertEqual(12,len({d['id'] for d in docs}))
        for d in docs:
            self.assertFalse(d['distribution_approved']); self.assertFalse(d['pdf_binary_included'])
            self.assertIsNone(d['pages_verified']); self.assertIsNone(d['sha256'])
        self.assertFalse(list((ROOT/'core/content/src/main/assets').rglob('*.pdf')))

    def test_seed_integrity_and_safety_not_published_as_validated(self):
        v1=json.loads(read('reference/08_CONTENIDOS_SEMILLA/PACK_TALLER_12_PROYECTOS_ORIGINALES.json'))['projects']
        v2=json.loads(read('reference/08_CONTENIDOS_SEMILLA/PACK_TALLER_12_ENRIQUECIDO_PEDAGOGIA_V2.json'))['projects']
        experiments=json.loads(read('reference/08_CONTENIDOS_SEMILLA/PACK_EXPERIMENTOS_8_ORIGINALES_V1.json'))['projects']
        self.assertEqual(12,len(v2)); self.assertEqual({x['id'] for x in v1},{x['id'] for x in v2})
        self.assertEqual(8,len(experiments))
        for item in v2:
            self.assertIn('VALIDACION',item['safety']['reviewStatus'])
            self.assertTrue((ROOT/'reference'/item['thumbnail']).is_file())
        for item in experiments: self.assertIn('PENDIENTE',item['reviewStatus'])

    def test_all_ui_and_qa_rows_preserved(self):
        ui=rows('reference/02_DOCUMENTACION/MATRIZ_PANTALLAS_ACCIONES_V2.csv')
        qa1=rows('reference/04_QA/MATRIZ_QA_PROFELAB_V1.csv')
        qa2=rows('reference/04_QA/MATRIZ_QA_PROFELAB_V2_TOTAL.csv')
        self.assertEqual(46,len(ui)); self.assertEqual(191,len(qa2)); self.assertEqual(134,len(qa1))
        self.assertEqual(qa1,qa2[:134])
        self.assertEqual({'NO_EJECUTADO'},{q['ESTADO'] for q in qa2})

    def test_navigation_routes_and_five_tabs(self):
        tabs=re.findall(r'TopDestination\("([a-z]+)","([^\"]+)"',read('app/src/main/kotlin/pe/profelab/app/NavigationBar.kt'))
        self.assertEqual(['Inicio','Taller','Sesiones','Cuaderno','Biblioteca'],[label for _,label in tabs])
        nav=read('app/src/main/kotlin/pe/profelab/app/ProfeApp.kt')
        for route,_ in tabs: self.assertIn('composable("'+route+'")',nav)
        for route in ['practices','profile','activity','about','task/{id}','tasks/{filter}']: self.assertIn('composable("'+route+'"',nav)

    def test_no_mockup_bitmap_or_fixture_data_in_android(self):
        for module in [ROOT/'app',ROOT/'core',ROOT/'feature']:
            for source in module.rglob('*'):
                if source.is_file() and '/src/main/' in source.as_posix():
                    self.assertNotIn(source.name,['01_INICIO.png','fixtures_profelab.json'])
                    self.assertNotEqual('.pdf',source.suffix)
                    if source.suffix=='.kt':
                        text=source.read_text(encoding='utf-8')
                        self.assertNotIn('fallbackToDestructiveMigration',text)
                        self.assertNotIn('TODO(',text)

    def test_wrapper_matches_official_checksum_and_distribution_is_pinned(self):
        record=json.loads(read('docs/audit/wrapper_provenance.json'))
        for item in record['files']:
            self.assertEqual(item['sha256'],hashlib.sha256((ROOT/item['file']).read_bytes()).hexdigest())
        self.assertIn('distributionSha256Sum='+record['distributionSha256'],read('gradle/wrapper/gradle-wrapper.properties'))
        with zipfile.ZipFile(ROOT/'gradle/wrapper/gradle-wrapper.jar') as jar:
            self.assertIn('org/gradle/wrapper/GradleWrapperMain.class',jar.namelist())

    def test_dependency_metadata_supports_selected_compile_sdk_and_agp(self):
        for item in json.loads(read('docs/audit/dependency_metadata.json')):
            self.assertEqual('PASS',item['status'],item['name'])
            metadata=item.get('aar_metadata','')
            props=dict(line.split('=',1) for line in metadata.splitlines() if '=' in line)
            self.assertLessEqual(int(props.get('minCompileSdk',0)),36)
            required=tuple(map(int,props.get('minAndroidGradlePluginVersion','0.0.0').split('.')))
            self.assertLessEqual(required,(8,11,1))

    def test_dao_sql_parses_against_entity_columns(self):
        # Executes exact @Query strings in SQLite, not Room-generated code.
        entities=read('core/database/src/main/kotlin/pe/profelab/core/database/Entities.kt')
        db=sqlite3.connect(':memory:')
        for table,body in re.findall(r'@Entity\(tableName = "([a-z]+)"[^\n]*\)\s*data class \w+\((.*?)\)\s*(?=@Entity|\Z)',entities,re.S):
            cols=re.findall(r'val (\w+): (String|Long|Int|Boolean)(\?)?',body)
            db.execute('CREATE TABLE "'+table+'" ('+','.join('"'+name+'" '+('TEXT' if typ=='String' else 'INTEGER') for name,typ,_ in cols)+')')
        self.assertEqual(4,db.execute("SELECT count(*) FROM sqlite_master WHERE type='table'").fetchone()[0])
        queries=re.findall(r'@Query\("([^\"]+)"\)',read('core/database/src/main/kotlin/pe/profelab/core/database/ProfeDao.kt'))
        self.assertGreaterEqual(len(queries),9)
        for query in queries:
            params={key:None for key in re.findall(r':(\w+)',query)}
            db.execute('EXPLAIN '+query,params).fetchall()
        db.close()

    def test_ci_is_read_only_and_generates_apk_and_reports(self):
        workflow=read('.github/workflows/android.yml')
        self.assertIn('contents: read',workflow); self.assertIn('persist-credentials: false',workflow)
        for expected in ['assembleDebug','lintDebug','connectedDebugAndroidTest','app-debug.apk','workflow_dispatch']:
            self.assertIn(expected,workflow)
        self.assertNotIn('contents: write',workflow)
        self.assertNotIn('git push',workflow)

if __name__=='__main__':
    suite=unittest.defaultTestLoader.loadTestsFromTestCase(DeliveryChecks)
    result=unittest.TextTestRunner(verbosity=2).run(suite)
    report={'scope':'Static source/artifact checks only; no Kotlin compilation or Android runtime',
            'tests_run':result.testsRun,'passed':result.testsRun-len(result.failures)-len(result.errors),
            'failures':[{'test':str(t),'trace':err} for t,err in result.failures],
            'errors':[{'test':str(t),'trace':err} for t,err in result.errors]}
    output=ROOT/'docs/qa/static-checks.json';output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2),encoding='utf-8')
    sys.exit(not result.wasSuccessful())
