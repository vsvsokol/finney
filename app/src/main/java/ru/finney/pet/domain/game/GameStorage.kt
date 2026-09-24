package ru.finney.pet.domain.game

import kotlinx.coroutines.flow.Flow
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.Profile
import ru.finney.pet.domain.model.SavedGame

/** Хранилище профилей и игрового состояния. Реализация — `data/` (Room). */
interface GameStorage {

    fun observeProfiles(): Flow<List<Profile>>

    /** null — профиль удалён. */
    fun observeGame(profileId: Long): Flow<SavedGame?>

    suspend fun load(profileId: Long): SavedGame?

    /** [profile].id игнорируется. Возвращает id созданного профиля. */
    suspend fun create(profile: Profile, state: GameState): Long

    /**
     * Сохраняет новое состояние. Операции и попытки заданий только дописываются:
     * [state] должен быть продолжением сохранённого.
     */
    suspend fun save(profileId: Long, state: GameState)

    /** Заменяет состояние целиком, профиль остаётся тем же. Для сброса прогресса: [save] только дописывает. */
    suspend fun replace(profileId: Long, state: GameState)

    suspend fun updateProfile(profileId: Long, petName: String, appearance: PetAppearance)

    suspend fun delete(profileId: Long)

    suspend fun deleteAll()
}
