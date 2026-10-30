package pe.profelab.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.*

class WorkspaceRepository(private val db: ProfeDatabase): WorkspaceStore {
    private val dao=db.workspace()
    override fun observeRecords()=dao.observe().map { rows -> rows.map { it.domain() } }
    override suspend fun record(id: String)=dao.find(id)?.domain()
    override suspend fun saveRecord(record: WorkRecord) {
        require(record.id.isNotBlank() && record.json.length <= 2_000_000)
        db.withTransaction {
            val old=dao.find(record.id)
            if(old!=null && old.revision % 20 == 0 && old.json!=record.json) {
                db.workspace().put(old.copy(id="history:${old.id}:${old.revision}",kind="revision",archived=true))
            }
            dao.put(record.entity().copy(revision=(old?.revision ?: 0)+1))
            if(record.kind in setOf("project","session","note","experiment","observation","document")) {
                val kind=when(record.kind){"session"->EntityKind.SESSION;"note"->EntityKind.NOTE;"document"->EntityKind.DOCUMENT;else->EntityKind.PROJECT}
                val eventId=java.util.UUID.nameUUIDFromBytes(("workspace:"+record.id).toByteArray(Charsets.UTF_8)).toString()
                db.dao().putActivity(ActivityEntity(eventId,kind.name,record.id,"Guardado: ${record.title.ifBlank {"Borrador sin título"}}",record.updatedAt))
            }
        }
    }
    override suspend fun archiveRecord(id: String)=dao.archive(id)
    override suspend fun saveBatch(records: List<WorkRecord>)=db.withTransaction { records.forEach { dao.put(it.entity()) } }
}
private fun WorkspaceEntity.domain()=WorkRecord(id,kind,title,json,updatedAt,revision,archived)
private fun WorkRecord.entity()=WorkspaceEntity(id,kind,title,json,updatedAt,revision,archived)
