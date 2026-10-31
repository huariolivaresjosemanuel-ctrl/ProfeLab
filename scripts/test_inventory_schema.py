"""Execute the exact T1 migration SQL in SQLite. This is not a Room/Kotlin test."""
from pathlib import Path
import json,re,sqlite3,unittest

ROOT=Path(__file__).resolve().parents[1]
SQL=re.findall(r'db\.execSQL\("([^"]+)"\)',(ROOT/'core/database/src/main/kotlin/pe/profelab/core/database/InventoryMigration.kt').read_text(encoding='utf-8'))
class InventorySchemaTest(unittest.TestCase):
    def setUp(self):
        self.db=sqlite3.connect(':memory:',isolation_level=None);self.db.execute('PRAGMA foreign_keys=ON')
        schema=json.loads((ROOT/'core/database/schemas/pe.profelab.core.database.ProfeDatabase/2.json').read_text(encoding='utf-8'))['database']
        for e in schema['entities']:
            self.db.execute(e['createSql'].replace('${TABLE_NAME}',e['tableName']))
            for index in e.get('indices',[]):self.db.execute(index['createSql'].replace('${TABLE_NAME}',e['tableName']))
        self.raw='{"quantity":"14.00","reserved":"10.00","steps":[{"id":"stable","done":true}]}'
        self.db.execute("INSERT INTO workspace_records VALUES ('stock','inventory','Keep',?,7,9,0)",(self.raw,))
        self.db.execute("INSERT INTO profiles VALUES (1,'profile','Keep name',6,'Center')")
        for statement in SQL:self.db.execute(statement)
    def tearDown(self):self.db.close()
    def op(self,id='op',key=None,reverts=None):
        self.db.execute('INSERT INTO inventory_operations VALUES (?,?,?,?,?,?,?,?,?,?)',(id,key or id,'run','CONSUMPTION','COMMITTED','{}','hash',reverts,1,1))
    def movement(self,id='m',op='op',sequence=1):
        self.db.execute('INSERT INTO inventory_movements VALUES (?,?,?,?,?,?,?,?,?)',(id,op,'stock','{}','{}','[]','[]',sequence,1))
    def test_migration_is_additive_and_keeps_original_bytes(self):
        self.assertEqual((self.raw,7,9,0),self.db.execute("SELECT json,updatedAt,revision,archived FROM workspace_records WHERE id='stock'").fetchone())
        self.assertEqual(('Keep name',6),self.db.execute('SELECT name,academicCycle FROM profiles').fetchone())
        names={r[0] for r in self.db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        self.assertTrue({'tasks','profiles','drafts','activity','workspace_records','inventory_operations','inventory_movements','inventory_reservations'}<=names)
    def test_operation_key_has_persistent_uniqueness(self):
        self.op(key='same')
        with self.assertRaises(sqlite3.IntegrityError):self.op('different',key='same')
    def test_only_one_reversal_can_reference_an_operation(self):
        self.op();self.op('undo',reverts='op')
        with self.assertRaises(sqlite3.IntegrityError):self.op('undo-again',reverts='op')
    def test_movement_requires_a_real_operation(self):
        with self.assertRaises(sqlite3.IntegrityError):self.movement(op='missing')
    def test_stock_sequence_cannot_be_duplicated(self):
        self.op();self.movement();self.op('op2')
        with self.assertRaises(sqlite3.IntegrityError):self.movement('m2','op2',1)
    def test_reservation_is_unique_per_stock_and_owner(self):
        self.db.execute("INSERT INTO inventory_reservations VALUES ('r','stock','run','10','unidad','ACTIVE',1)")
        self.db.execute("INSERT INTO inventory_reservations VALUES ('other','stock','another-run','1','unidad','ACTIVE',1)")
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute("INSERT INTO inventory_reservations VALUES ('duplicate','stock','run','2','unidad','ACTIVE',1)")
    def test_failed_transaction_rolls_back_stock_and_event_together(self):
        self.db.execute('BEGIN')
        try:
            self.op();self.db.execute("UPDATE workspace_records SET json='changed' WHERE id='stock'");self.movement();self.op()
        except sqlite3.IntegrityError:self.db.execute('ROLLBACK')
        else:self.fail('Expected constraint failure')
        self.assertEqual(self.raw,self.db.execute("SELECT json FROM workspace_records WHERE id='stock'").fetchone()[0])
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM inventory_operations').fetchone()[0])
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM inventory_movements').fetchone()[0])
    def test_migration_sql_is_safe_to_replay_and_remains_non_destructive(self):
        self.assertEqual(9,len(SQL))
        for statement in SQL:
            self.assertRegex(statement,r'^CREATE (?:UNIQUE )?(?:TABLE|INDEX) IF NOT EXISTS ');self.db.execute(statement)
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM inventory_operations').fetchone()[0])

if __name__=='__main__':unittest.main()
