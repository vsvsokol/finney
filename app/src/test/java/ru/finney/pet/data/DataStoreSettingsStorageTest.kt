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

/**
 * Звук, музыка, вибрация и анимации хранятся отдельно и по умолчанию включены.
 *
 * В каждом тесте не больше одной записи: на Windows DataStore не может переименовать
 * `.tmp` поверх уже записанного файла (`IOException: Unable to rename`), и вторая
 * запись подряд там падает. На Android и в CI такого нет.
 */
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
        assertEquals(SoundSettings(sound = true, music = true, haptics = true, animations = true), storage.sound.first())
    }

    @Test
    fun `вибрация выключается отдельно от звука`() = runBlocking {
        storage.setHaptics(false)
        assertEquals(SoundSettings(sound = true, music = true, haptics = false), storage.sound.first())
    }

    @Test
    fun `анимации выключаются отдельно, ТЗ 3-6`() = runBlocking {
        storage.setAnimations(false)
        assertEquals(SoundSettings(animations = false), storage.sound.first())
    }
}
