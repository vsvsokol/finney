package ru.finney.pet.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.finney.pet.domain.settings.SettingsStorage
import ru.finney.pet.domain.settings.SoundSettings

class DataStoreSettingsStorage(private val dataStore: DataStore<Preferences>) : SettingsStorage {

    override val sound: Flow<SoundSettings> = dataStore.data.map { prefs ->
        SoundSettings(
            sound = prefs[SOUND] ?: true,
            music = prefs[MUSIC] ?: true,
            haptics = prefs[HAPTICS] ?: true,
        )
    }

    override suspend fun setSound(enabled: Boolean) {
        dataStore.edit { it[SOUND] = enabled }
    }

    override suspend fun setMusic(enabled: Boolean) {
        dataStore.edit { it[MUSIC] = enabled }
    }

    override suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { it[HAPTICS] = enabled }
    }

    private companion object {
        val SOUND = booleanPreferencesKey("sound")
        val MUSIC = booleanPreferencesKey("music")
        val HAPTICS = booleanPreferencesKey("haptics")
    }
}
