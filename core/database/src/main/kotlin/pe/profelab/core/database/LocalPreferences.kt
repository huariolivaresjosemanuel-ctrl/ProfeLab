package pe.profelab.core.database

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.PreferencesRepository
import pe.profelab.core.model.UserPreferences

private val Context.profePreferences by preferencesDataStore("profelab_preferences")
class LocalPreferences(context: Context) : PreferencesRepository {
    private val store = context.applicationContext.profePreferences
    private val showCompleted = booleanPreferencesKey("show_completed_tasks")
    override val preferences = store.data.map { UserPreferences(it[showCompleted] ?: false) }
    override suspend fun setShowCompleted(enabled: Boolean) { store.edit { it[showCompleted] = enabled } }
}
