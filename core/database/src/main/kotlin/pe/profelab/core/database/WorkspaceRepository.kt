package pe.profelab.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.*

class WorkspaceRepository(private val db: ProfeDatabase): WorkspaceStore, InventoryOperationStore by InventoryOperationRepository(db) {
    private val dao=db.workspace()
    private val inventoryJournal=InventoryOperationRepository(db)
    /** Additive rewrite: a key or authentication failure aborts the complete transaction. */
    suspend fun protectEducationRecords() {
        db.openHelper.writableDatabase.execSQL("PRAGMA secure_delete=ON")
        db.withTransaction {
            val rows=dao.all();val parents=rows.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet();val privateRevisions=EducationEncryption.preservedRevisionIds(rows)
            rows.filter {it.kind.startsWith("edu_") || (it.kind=="revision" && (it.id in privateRevisions || EducationEncryption.privateParentId(it.id) in parents))}.forEach {row->
                val record=row.domain() // Authenticate existing ciphertext before exposing any workspace.
                if(!EducationEncryption.encrypted(row.json))dao.put(record.entity(parents,privateRevisions))
            }
        }
    }
    override fun observeRecords()=dao.observe().map { rows -> rows.map { it.domain() } }
    override suspend fun record(id: String)=dao.find(id)?.domain()
    override suspend fun saveRecord(record: WorkRecord) {
        require(record.id.isNotBlank() && record.json.length <= 2_000_000)
        db.withTransaction {
            val old=dao.find(record.id)
            val audited=if(record.kind=="inventory" || old?.kind=="inventory")inventoryJournal.journalLegacy(newId(),listOfNotNull(old?.domain()),listOf(record)).single()else record
            if(old!=null && old.revision % 20 == 0 && old.domain().json!=record.json) {
                val history=old.domain().copy(id="history:${old.id}:${old.revision}",kind="revision",archived=true)
                db.workspace().put(EducationEncryption.write(history,old.kind.startsWith("edu_") || (old.kind=="revision" && EducationEncryption.encrypted(old.json))))
            }
            val protectionRows=if(audited.kind=="revision")dao.all()else emptyList()
            dao.put(audited.entity(protectionRows.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet(),EducationEncryption.preservedRevisionIds(protectionRows)).copy(revision=(old?.revision ?: 0)+1))
            if(record.kind in setOf("project","session","note","experiment","observation","document")) {
                val kind=when(record.kind){"session"->EntityKind.SESSION;"note"->EntityKind.NOTE;"document"->EntityKind.DOCUMENT;else->EntityKind.PROJECT}
                val eventId=java.util.UUID.nameUUIDFromBytes(("workspace:"+record.id).toByteArray(Charsets.UTF_8)).toString()
                db.dao().putActivity(ActivityEntity(eventId,kind.name,record.id,"Guardado: ${record.title.ifBlank {"Borrador sin título"}}",record.updatedAt))
            }
        }
    }
    override suspend fun archiveRecord(id: String)=db.withTransaction {
        val row=dao.find(id) ?: return@withTransaction
        if(row.kind=="inventory"){
            val rows=dao.all().map {it.domain()}
            val audited=inventoryJournal.journalLegacy(newId(),rows,listOf(row.domain().copy(archived=true))).single()
            dao.put(audited.entity().copy(revision=row.revision+1))
        }else dao.archive(id)
    }
    override suspend fun saveBatch(records: List<WorkRecord>)=db.withTransaction {
        val raw=dao.all();val rows=raw.map {it.domain()};val parents=EducationEncryption.parentIds(rows+records);val privateRevisions=EducationEncryption.preservedRevisionIds(raw)
        inventoryJournal.journalLegacy(newId(),rows,records).forEach {val old=dao.find(it.id);dao.put(it.entity(parents,privateRevisions).copy(revision=(old?.revision ?: 0)+1))}
    }
    override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>) {
        db.withTransaction {
            val raw=dao.all();val current=raw.map {it.domain()};val next=change(current);val parents=EducationEncryption.parentIds(current+next);val privateRevisions=EducationEncryption.preservedRevisionIds(raw)
            require(next.map {it.id}.distinct().size==next.size)
            inventoryJournal.journalLegacy(newId(),current,next).forEach {row->
                require(row.id.isNotBlank() && row.json.length<=2_000_000);val old=dao.find(row.id);dao.put(row.entity(parents,privateRevisions).copy(revision=(old?.revision ?: 0)+1))
                if(row.kind in setOf("project","session","note","experiment","observation","document")){
                    val kind=when(row.kind){"session"->EntityKind.SESSION;"note"->EntityKind.NOTE;"document"->EntityKind.DOCUMENT;else->EntityKind.PROJECT}
                    val eventId=java.util.UUID.nameUUIDFromBytes(("workspace:"+row.id).toByteArray(Charsets.UTF_8)).toString()
                    db.dao().putActivity(ActivityEntity(eventId,kind.name,row.id,"Guardado: ${row.title.ifBlank {"Borrador sin título"}}",row.updatedAt))
                }
            }
        }
    }
}
