package pe.profelab.core.model

import kotlinx.coroutines.flow.Flow

data class WorkRecord(val id: String, val kind: String, val title: String, val json: String,
    val updatedAt: Long, val revision: Int = 1, val archived: Boolean = false)
interface WorkspaceStore {
    fun observeRecords(): Flow<List<WorkRecord>>
    suspend fun record(id: String): WorkRecord?
    suspend fun saveRecord(record: WorkRecord)
    suspend fun archiveRecord(id: String)
    suspend fun saveBatch(records: List<WorkRecord>)
    /** Reads current rows and applies a local multi-record operation atomically. No UI snapshot overwrites. */
    suspend fun editRecords(change: (List<WorkRecord>)->List<WorkRecord>)
}
interface LocalTransfer {
    suspend fun exportBackup(destinationUri: String, includeFiles: Boolean)
    suspend fun previewBackup(sourceUri: String): String
    suspend fun restoreBackup(sourceUri: String)
}
