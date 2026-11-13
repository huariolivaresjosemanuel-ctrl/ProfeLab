"""Execute existing Room2+T1 SQL with private JSON rows. Not a Kotlin/Room or privacy-validator test."""
from pathlib import Path
import json,re,sqlite3,unittest

ROOT=Path(__file__).resolve().parents[1]
DATABASE=ROOT/'core/database/src/main/kotlin/pe/profelab/core/database'
MIGRATION=re.findall(r'db\.execSQL\("([^"]+)"\)',(DATABASE/'InventoryMigration.kt').read_text(encoding='utf-8'))

def database():
    db=sqlite3.connect(':memory:',isolation_level=None);db.execute('PRAGMA foreign_keys=ON')
    schema=json.loads((ROOT/'core/database/schemas/pe.profelab.core.database.ProfeDatabase/2.json').read_text(encoding='utf-8'))['database']
    for entity in schema['entities']:
        db.execute(entity['createSql'].replace('${TABLE_NAME}',entity['tableName']))
        for index in entity.get('indices',[]):db.execute(index['createSql'].replace('${TABLE_NAME}',entity['tableName']))
    for sql in MIGRATION:db.execute(sql)
    return db

class EducationStorageTest(unittest.TestCase):
    def setUp(self):
        self.db=database()
        self.rows=[('class-a','edu_classroom','A',{'schemaVersion':1,'name':'Aula QA','grade':4,'section':'A','schoolYear':2026}),
            ('class-b','edu_classroom','B',{'schemaVersion':1,'name':'Aula QA','grade':4,'section':'B','schoolYear':2026}),
            ('student-a','edu_student','QA',{'schemaVersion':1,'classroomId':'class-a','displayName':'Participante de prueba'}),
            ('run-a','edu_run','QA',{'schemaVersion':1,'classroomId':'class-a','participants':[{'studentId':'student-a','displayName':'Participante de prueba'}]}),
            ('observation-a','edu_observation','QA',{'schemaVersion':1,'runId':'run-a','classroomId':'class-a','targetId':'student-a','note':'Comparó dos materiales.'})]
        self.values=[(id,kind,title,json.dumps(body,ensure_ascii=False,separators=(',',':')),7,1,0) for id,kind,title,body in self.rows]
        self.db.executemany('INSERT INTO workspace_records VALUES (?,?,?,?,?,?,?)',self.values)
    def tearDown(self):self.db.close()
    def test_existing_schema_preserves_private_json_and_identifiers_without_new_tables(self):
        self.assertEqual(sorted(self.values),self.db.execute('SELECT * FROM workspace_records ORDER BY id').fetchall())
        names={row[0] for row in self.db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        self.assertFalse(any(name.startswith('edu_') for name in names))
        self.assertIn('workspace_records',names);self.assertIn('inventory_operations',names)
    def test_actual_archive_query_keeps_participants_runs_and_observations(self):
        source=(DATABASE/'WorkspaceEntity.kt').read_text(encoding='utf-8')
        sql=re.search(r'@Query\("(UPDATE workspace_records SET archived=1 WHERE id=:id)"\)',source).group(1)
        before=self.db.execute("SELECT id,json FROM workspace_records WHERE id!='class-a' ORDER BY id").fetchall()
        self.db.execute(sql,{'id':'class-a'})
        self.assertEqual(1,self.db.execute("SELECT archived FROM workspace_records WHERE id='class-a'").fetchone()[0])
        self.assertEqual(before,self.db.execute("SELECT id,json FROM workspace_records WHERE id!='class-a' ORDER BY id").fetchall())
    def test_generic_backup_row_transport_is_idempotent_for_identical_private_records(self):
        columns=[row[1] for row in self.db.execute('PRAGMA table_info(workspace_records)')]
        snapshot=[dict(zip(columns,row)) for row in self.db.execute('SELECT * FROM workspace_records ORDER BY id')]
        target=database()
        try:
            for _ in range(2):
                with target:
                    for row in snapshot:
                        query='INSERT OR IGNORE INTO workspace_records ('+','.join('`'+key+'`' for key in row)+') VALUES ('+','.join('?' for _ in row)+')'
                        target.execute(query,list(row.values()))
            self.assertEqual(self.db.execute('SELECT * FROM workspace_records ORDER BY id').fetchall(),target.execute('SELECT * FROM workspace_records ORDER BY id').fetchall())
            self.assertEqual(0,target.execute('SELECT count(*) FROM inventory_movements').fetchone()[0])
        finally:target.close()

if __name__=='__main__':unittest.main()
