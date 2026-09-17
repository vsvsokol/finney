package ru.finney.pet.domain.game

import kotlinx.coroutines.flow.Flow

/** Какой профиль сейчас открыт. Реализация — `data/prefs` (DataStore). */
interface ActiveProfileStorage {

    /** null — профиль не выбран: первый запуск или профиль удалён. */
    val activeProfileId: Flow<Long?>

    suspend fun setActiveProfileId(profileId: Long?)
}
