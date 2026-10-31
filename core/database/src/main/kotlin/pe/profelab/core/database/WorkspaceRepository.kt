package pe.profelab.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.*

class WorkspaceRepository(private val db: ProfeDatabase): WorkspaceStore, InventoryOperationStore by InventoryOperationRepository(db) {
    private val dao=db.workspace()
    private val inventoryJournal=InventoryOperationRepository(db)
    override fun observeRecords()=dao.observe().map { rows -> rows.map { it.domain() } }
    override suspend fun record(id: String)=dao.find(id)?.domain()
    override suspend fun saveRecord(record: WorkRecord) {
        require(record.id.isNotBlank() && record.json.length <= 2_000_000)
        db.withTransaction {
            val old=dao.find(record.id)
            val audited=if(record.kind=="inventory" || old?.kind=="inventory")inventoryJournal.journalLegacy(newId(),listOfNotNull(old?.domain()),listOf(record)).single()else record
            if(old!=null && old.revision % 20 == 0 && old.json!=record.json) {
                db.workspace().put(old.copy(id="history:${old.id}:${old.revision}",kind="revision",archived=true))
            }
            dao.put(audited.entity().copy(revision=(old?.revision ?: 0)+1))
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
        val rows=dao.all().map {it.domain()}
        inventoryJournal.journalLegacy(newId(),rows,records).forEach {val old=dao.find(it.id);dao.put(it.entity().copy(revision=(old?.revision ?: 0)+1))}
    }
    override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>) {
        db.withTransaction {
            val current=dao.all().map {it.domain()};val next=change(current)
            require(next.map {it.id}.distinct().size==next.size)
            inventoryJournal.journalLegacy(newId(),current,next).forEach {row->require(row.id.isNotBlank() && row.json.length<=2_000_000);val old=dao.find(row.id);dao.put(row.entity().copy(revision=(old?.revision ?: 0)+1))}
        }
    }
}
private fun WorkspaceEntity.domain()=WorkRecord(id,kind,title,json,updatedAt,revision,archived)
private fun WorkRecord.entity()=WorkspaceEntity(id,kind,title,json,updatedAt,revision,archived)
