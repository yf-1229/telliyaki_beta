package com.telliyaki.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "programs")

@Serializable
data class SavedProgram(
    val name: String,
    val json: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class ProgramStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val PROGRAMS_KEY = stringPreferencesKey("programs")
    private val AUTO_SAVE_KEY = stringPreferencesKey("auto_save")

    suspend fun saveProgram(name: String, blocklyJson: String) {
        context.dataStore.edit { prefs ->
            val existingPrograms = getProgramsList(prefs)
            val existingIndex = existingPrograms.indexOfFirst { it.name == name }

            val newProgram = if (existingIndex >= 0) {
                existingPrograms[existingIndex].copy(
                    json = blocklyJson,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                SavedProgram(name = name, json = blocklyJson)
            }

            val updatedPrograms = if (existingIndex >= 0) {
                existingPrograms.toMutableList().apply { set(existingIndex, newProgram) }
            } else {
                existingPrograms + newProgram
            }

            prefs[PROGRAMS_KEY] = json.encodeToString(updatedPrograms)
        }
    }

    suspend fun loadProgram(name: String): String? {
        return context.dataStore.data.map { prefs ->
            val programs = getProgramsList(prefs)
            programs.find { it.name == name }?.json
        }.first()
    }

    suspend fun deleteProgram(name: String) {
        context.dataStore.edit { prefs ->
            val programs = getProgramsList(prefs)
            val updatedPrograms = programs.filter { it.name != name }
            prefs[PROGRAMS_KEY] = json.encodeToString(updatedPrograms)
        }
    }

    suspend fun renameProgram(oldName: String, newName: String) {
        context.dataStore.edit { prefs ->
            val programs = getProgramsList(prefs)
            val updatedPrograms = programs.map {
                if (it.name == oldName) it.copy(name = newName, updatedAt = System.currentTimeMillis())
                else it
            }
            prefs[PROGRAMS_KEY] = json.encodeToString(updatedPrograms)
        }
    }

    suspend fun getProgramNames(): List<String> {
        return context.dataStore.data.map { prefs ->
            getProgramsList(prefs).map { it.name }
        }.first()
    }

    suspend fun getAllPrograms(): List<SavedProgram> {
        return context.dataStore.data.map { prefs ->
            getProgramsList(prefs)
        }.first()
    }

    suspend fun autoSave(blocklyJson: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_SAVE_KEY] = blocklyJson
        }
    }

    suspend fun getAutoSavedJson(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[AUTO_SAVE_KEY]
        }.first()
    }

    private fun getProgramsList(prefs: Preferences): List<SavedProgram> {
        val jsonString = prefs[PROGRAMS_KEY] ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedProgram>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
