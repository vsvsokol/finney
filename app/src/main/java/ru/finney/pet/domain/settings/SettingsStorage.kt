package ru.finney.pet.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Звук, музыка и анимации выключаются отдельно (ТЗ п. 3.6), вибрация — тоже: системная
 * настройка «вибрация при касании» её не заменяет. По умолчанию включено всё.
 */
data class SoundSettings(
    val sound: Boolean = true,
    val music: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
)

/** Настройки приложения, общие для всех профилей. Реализация — `data/prefs` (DataStore). */
interface SettingsStorage {

    val sound: Flow<SoundSettings>

    suspend fun setSound(enabled: Boolean)

    suspend fun setMusic(enabled: Boolean)

    suspend fun setHaptics(enabled: Boolean)

    suspend fun setAnimations(enabled: Boolean)
}
