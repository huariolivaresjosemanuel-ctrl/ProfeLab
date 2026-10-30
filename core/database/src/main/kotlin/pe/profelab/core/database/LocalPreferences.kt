package pe.profelab.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import pe.profelab.core.model.*

private val Context.profePreferences by preferencesDataStore("profelab_preferences")
class LocalPreferences(private val store:DataStore<Preferences>):PreferencesRepository {
    constructor(context:Context):this(context.applicationContext.profePreferences)
    private val showCompleted=booleanPreferencesKey("show_completed_tasks")
    private val displayName=stringPreferencesKey("display_name")
    private val role=stringPreferencesKey("local_role")
    private val completed=booleanPreferencesKey("onboarding_completed")
    private val greeting=booleanPreferencesKey("personalized_greeting")
    private val mascot=booleanPreferencesKey("mascot_visible")
    private val motion=booleanPreferencesKey("reduce_motion")
    private val migrated=booleanPreferencesKey("personalization_migrated_v1")
    // Presentation belongs to this installation, not to registration or the portable backup format.
    private val introSeen=booleanPreferencesKey("welcome_intro_seen_v1")
    override val preferences=store.data.map {p->UserPreferences(p[showCompleted] ?: false,p[displayName].orEmpty(),LocalRole.entries.firstOrNull {it.name==p[role]} ?: LocalRole.UNSPECIFIED,p[completed] ?: false,p[greeting] ?: true,p[mascot] ?: true,p[motion] ?: false,p[introSeen] ?: false)}
    override suspend fun completeWelcomeRegistration(name:String,role:LocalRole){store.edit {
        it[displayName]=PersonalizationRules.name(name);it[this.role]=role.name;it[completed]=true;it[migrated]=true;it[introSeen]=true
    }}
    override suspend fun markWelcomeIntroSeen(){store.edit {it[introSeen]=true}}
    override suspend fun setShowCompleted(enabled:Boolean){store.edit {it[showCompleted]=enabled}}
    override suspend fun setPersonalization(name:String,role:LocalRole){store.edit {
        it[displayName]=PersonalizationRules.name(name);it[this.role]=role.name;it[completed]=true;it[migrated]=true
    }}
    override suspend fun setMascotPreferences(visible:Boolean,reduceMotion:Boolean,personalizedGreeting:Boolean){store.edit {
        it[mascot]=visible;it[motion]=reduceMotion;it[greeting]=personalizedGreeting
    }}
    /** Runs once after the old Room profile has loaded. Never clears or renames that profile. */
    suspend fun migrateLegacyProfile(profile:Profile?){store.edit {p->
        if(profile!=null && !p.contains(completed)){if(!p.contains(displayName))p[displayName]=PersonalizationRules.name(profile.name);p[completed]=true}
        if(p[migrated]!=true){
        p[migrated]=true
    }}}
    suspend fun exportSnapshot():JSONObject {
        val p=store.data.first();val result=JSONObject().put("version",1)
        listOf(showCompleted,completed,greeting,mascot,motion,migrated).forEach {key->p[key]?.let {result.put(key.name,it)}}
        listOf(displayName,role).forEach {key->p[key]?.let {result.put(key.name,it)}}
        return result
    }
    fun validateSnapshot(j:JSONObject){
        require(j.getInt("version")==1)
        val booleans=listOf(showCompleted,completed,greeting,mascot,motion,migrated).map {it.name}
        val strings=listOf(displayName.name,role.name)
        require(j.keys().asSequence().all {it=="version" || it in booleans || it in strings})
        booleans.filter {j.has(it)}.forEach {require(j.get(it) is Boolean)}
        if(j.has(displayName.name))require(j.get(displayName.name) is String && j.getString(displayName.name).length<=60)
        if(j.has(role.name))require(j.getString(role.name) in LocalRole.entries.map {it.name})
    }
    /** Restore fills absent keys only, so an existing personalization is never overwritten. */
    suspend fun restoreMissing(j:JSONObject){validateSnapshot(j);store.edit {p->
        listOf(showCompleted,completed,greeting,mascot,motion,migrated).forEach {key->if(!p.contains(key) && j.has(key.name))p[key]=j.getBoolean(key.name)}
        listOf(displayName,role).forEach {key->if(!p.contains(key) && j.has(key.name))p[key]=if(key==displayName)PersonalizationRules.name(j.getString(key.name))else j.getString(key.name)}
    }}
}
