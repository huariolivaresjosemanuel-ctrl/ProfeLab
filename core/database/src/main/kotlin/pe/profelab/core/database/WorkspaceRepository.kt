package pe.profelab.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.*
import pe.profelab.core.model.education.EducationProtectionStage
import kotlinx.coroutines.CancellationException

class WorkspaceRepository internal constructor(private val db:ProfeDatabase,private val keys:EducationKeyAccess):WorkspaceStore,InventoryOperationStore by InventoryOperationRepository(db) {
    constructor(db:ProfeDatabase):this(db,AndroidEducationKeyAccess)
    private val dao=db.workspace()
    private val inventoryJournal=InventoryOperationRepository(db)
    private suspend fun authenticateProtectedRows(){
        val all=dao.all();val parents=all.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet()
        all.filter {it.kind.startsWith("edu_") || (it.kind=="revision" && (EducationEncryption.encrypted(it.json) || EducationEncryption.privateParentId(it.id) in parents))}.forEach {it.domain(keys).also(::validateEducationPayload)}
    }
    /** Additive rewrite: a key or authentication failure aborts the complete transaction. */
    suspend fun protectEducationRecords(onStage:(EducationProtectionStage)->Unit={})=protectEducationRecordsWithKeys(keys,onStage)
    internal suspend fun protectEducationRecordsWithKeys(keys:EducationKeyAccess,onStage:(EducationProtectionStage)->Unit={}) {
        var stage=EducationProtectionStage.OPEN_DB
        fun phase(value:EducationProtectionStage){stage=value;onStage(value)}
        try {
            phase(EducationProtectionStage.OPEN_DB)
            val database=db.openHelper.writableDatabase
            phase(EducationProtectionStage.SET_SECURE_DELETE)
            // This PRAGMA returns a row: execSQL is invalid on Android SQLite.
            database.query("PRAGMA secure_delete=ON").use {cursor->
                check(cursor.moveToFirst() && cursor.getInt(0)==1){"Secure deletion was not enabled"}
            }
            database.query("PRAGMA secure_delete").use {cursor->check(cursor.moveToFirst() && cursor.getInt(0)==1){"Secure deletion could not be verified"}}
            db.withTransaction {
                phase(EducationProtectionStage.READ_LEGACY)
                val rows=dao.all();val parents=rows.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet();val privateRevisions=EducationEncryption.preservedRevisionIds(rows)
                val protected=rows.filter {it.kind.startsWith("edu_") || (it.kind=="revision" && (it.id in privateRevisions || EducationEncryption.privateParentId(it.id) in parents))}
                // Authenticate every existing ciphertext before any encryption may create a new key.
                phase(EducationProtectionStage.VERIFY_EDU)
                val originals=protected.associate {it.id to it.domain(keys).also(::validateEducationPayload)}
                phase(EducationProtectionStage.ENCRYPT_EDU)
                protected.filterNot {EducationEncryption.encrypted(it.json)}.forEach {row->
                    dao.put(requireNotNull(originals[row.id]).entity(parents,privateRevisions,keys))
                }
                phase(EducationProtectionStage.VERIFY_EDU)
                protected.forEach {original->
                    val stored=requireNotNull(dao.find(original.id))
                    check(EducationEncryption.encrypted(stored.json) && stored.domain(keys).also(::validateEducationPayload)==originals[original.id]){"Private migration verification failed"}
                }
            }
        }catch(cancelled:CancellationException){throw cancelled}
        catch(error:Exception){throw protectionFailure(stage,error)}
    }
    override fun observeRecords()=dao.observe().map { rows -> rows.map { it.domain(keys) } }
    override suspend fun record(id: String)=dao.find(id)?.domain(keys)
    override suspend fun saveRecord(record: WorkRecord) {
        require(record.id.isNotBlank() && record.json.length <= 2_000_000)
        db.withTransaction {
            val old=dao.find(record.id)
            val privateRevision=record.kind=="revision" && (old?.let {EducationEncryption.encrypted(it.json)}==true || EducationEncryption.privateParentId(record.id)?.let {dao.find(it)?.kind?.startsWith("edu_")}==true)
            if(record.kind.startsWith("edu_") || old?.kind?.startsWith("edu_")==true || privateRevision){
                try {
                    authenticateProtectedRows()
                    validateEducationPayload(record)
                }catch(cancelled:CancellationException){throw cancelled}
                catch(error:Exception){throw protectionFailure(EducationProtectionStage.VERIFY_EDU,error)}
            }
            val audited=if(record.kind=="inventory" || old?.kind=="inventory")inventoryJournal.journalLegacy(newId(),listOfNotNull(old?.domain(keys)),listOf(record)).single()else record
            if(old!=null && old.revision % 20 == 0 && old.domain(keys).json!=record.json) {
                val history=old.domain(keys).copy(id="history:${old.id}:${old.revision}",kind="revision",archived=true)
                db.workspace().put(EducationEncryption.write(record=history,forcePrivate=old.kind.startsWith("edu_") || (old.kind=="revision" && EducationEncryption.encrypted(old.json)),keys=keys))
            }
            val protectionRows=if(audited.kind=="revision")dao.all()else emptyList()
            dao.put(audited.entity(protectionRows.filter {it.kind.startsWith("edu_")}.map {it.id}.toSet(),EducationEncryption.preservedRevisionIds(protectionRows),keys).copy(revision=(old?.revision ?: 0)+1))
            if(record.kind in setOf("project","session","note","experiment","observation","document")) {
                val kind=when(record.kind){"session"->EntityKind.SESSION;"note"->EntityKind.NOTE;"document"->EntityKind.DOCUMENT;else->EntityKind.PROJECT}
                val eventId=java.util.UUID.nameUUIDFromBytes(("workspace:"+record.id).toByteArray(Charsets.UTF_8)).toString()
                db.dao().putActivity(ActivityEntity(eventId,kind.name,record.id,"Guardado: ${record.title.ifBlank {"Borrador sin título"}}",record.updatedAt))
            }
        }
    }
    override suspend fun archiveRecord(id: String)=db.withTransaction {
        val row=dao.find(id) ?: return@withTransaction
        if(row.kind.startsWith("edu_") || row.kind=="revision"){
            try {authenticateProtectedRows()}
            catch(cancelled:CancellationException){throw cancelled}
            catch(error:Exception){throw protectionFailure(EducationProtectionStage.VERIFY_EDU,error)}
        }
        if(row.kind=="inventory"){
            val rows=dao.all().map {it.domain(keys)}
            val audited=inventoryJournal.journalLegacy(newId(),rows,listOf(row.domain(keys).copy(archived=true))).single()
            dao.put(audited.entity(keys=keys).copy(revision=row.revision+1))
        }else dao.archive(id)
    }
    override suspend fun saveBatch(records: List<WorkRecord>)=db.withTransaction {
        val raw=dao.all();val rows=raw.map {it.domain(keys)};val parents=EducationEncryption.parentIds(rows+records);val privateRevisions=EducationEncryption.preservedRevisionIds(raw)
        inventoryJournal.journalLegacy(newId(),rows,records).forEach {val old=dao.find(it.id);dao.put(it.entity(parents,privateRevisions,keys).copy(revision=(old?.revision ?: 0)+1))}
    }
    override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>) {
        db.withTransaction {
            val raw=dao.all();val current=raw.map {it.domain(keys)};val next=change(current);val parents=EducationEncryption.parentIds(current+next);val privateRevisions=EducationEncryption.preservedRevisionIds(raw)
            require(next.map {it.id}.distinct().size==next.size)
            inventoryJournal.journalLegacy(newId(),current,next).forEach {row->
                require(row.id.isNotBlank() && row.json.length<=2_000_000);val old=dao.find(row.id);dao.put(row.entity(parents,privateRevisions,keys).copy(revision=(old?.revision ?: 0)+1))
                if(row.kind in setOf("project","session","note","experiment","observation","document")){
                    val kind=when(row.kind){"session"->EntityKind.SESSION;"note"->EntityKind.NOTE;"document"->EntityKind.DOCUMENT;else->EntityKind.PROJECT}
                    val eventId=java.util.UUID.nameUUIDFromBytes(("workspace:"+row.id).toByteArray(Charsets.UTF_8)).toString()
                    db.dao().putActivity(ActivityEntity(eventId,kind.name,row.id,"Guardado: ${row.title.ifBlank {"Borrador sin título"}}",row.updatedAt))
                }
            }
        }
    }
}
