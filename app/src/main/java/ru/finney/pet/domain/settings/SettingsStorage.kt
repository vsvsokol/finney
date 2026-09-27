package ru.finney.pet.domain.settings

import kotlinx.coroutines.flow.Flow

/** Звук и музыка выключаются отдельно (ТЗ п. 3.6). По умолчанию включено всё. */
data class SoundSettings(
    val sound: Boolean = true,
    val music: Boolean = true,
)

/** Настройки приложения, общие для всех профилей. Реализация — `data/prefs` (DataStore). */
interface SettingsStorage {

    val sound: Flow<SoundSettings>

    suspend fun setSound(enabled: Boolean)

    suspend fun setMusic(enabled: Boolean)
}
