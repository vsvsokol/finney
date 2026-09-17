package ru.finney.pet.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.finney.pet.domain.game.ActiveProfileStorage

/** Один файл настроек на приложение: сюда же лягут звук и анимации (ТЗ п. 3.6). */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreActiveProfileStorage(private val dataStore: DataStore<Preferences>) : ActiveProfileStorage {

    override val activeProfileId: Flow<Long?> = dataStore.data.map { it[ACTIVE_PROFILE_ID] }

    override suspend fun setActiveProfileId(profileId: Long?) {
        dataStore.edit { prefs ->
            if (profileId == null) prefs.remove(ACTIVE_PROFILE_ID) else prefs[ACTIVE_PROFILE_ID] = profileId
        }
    }

    private companion object {
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
    }
}
