package ru.finney.pet.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.finney.pet.data.prefs.DataStoreSettingsStorage
import ru.finney.pet.domain.settings.SoundSettings

/** Звук, музыка и вибрация хранятся отдельно и по умолчанию включены. */
class DataStoreSettingsStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val storage by lazy {
        DataStoreSettingsStorage(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb").also { it.delete() } })
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `по умолчанию включено всё`() = runBlocking {
        assertEquals(SoundSettings(sound = true, music = true, haptics = true), storage.sound.first())
    }

    @Test
    fun `вибрация выключается отдельно от звука`() = runBlocking {
        storage.setHaptics(false)
        assertEquals(SoundSettings(sound = true, music = true, haptics = false), storage.sound.first())
        storage.setHaptics(true)
        assertEquals(true, storage.sound.first().haptics)
    }
}
