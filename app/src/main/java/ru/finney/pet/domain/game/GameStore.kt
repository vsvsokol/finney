package ru.finney.pet.domain.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.Profile
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.profile.PetNameError
import ru.finney.pet.domain.profile.ProfileRules
import ru.finney.pet.domain.tasks.TaskInput

sealed interface ProfileResult {
    data class Saved(val profileId: Long) : ProfileResult
    data class InvalidName(val error: PetNameError) : ProfileResult
}

/**
 * То, чем пользуются экраны: [Game] плюс сохранение. Каждая команда выполняется над последним
 * сохранённым состоянием и сохраняется сразу — прогресс переживает закрытие приложения (ТЗ п. 2.5.13).
 * Команды выполняются строго по очереди, двойное нажатие не спишет деньги дважды поверх одного состояния.
 */
class GameStore(
    private val game: Game,
    private val storage: GameStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    fun observeProfiles(): Flow<List<Profile>> = storage.observeProfiles()

    fun observeGame(profileId: Long): Flow<SavedGame?> = storage.observeGame(profileId)

    suspend fun createProfile(petName: String, appearance: PetAppearance, isDemo: Boolean = false): ProfileResult {
        val name = when (val result = ProfileRules.normalizeName(petName)) {
            is ProfileRules.Result.Invalid -> return ProfileResult.InvalidName(result.error)
            is ProfileRules.Result.Valid -> result.name
        }
        return mutex.withLock {
            val profile = Profile(id = 0, petName = name, appearance = appearance, isDemo = isDemo, createdAt = clock())
            ProfileResult.Saved(storage.create(profile, game.newGame(isDemo)))
        }
    }

    /** Повторная настройка внешности и имени; прогресс не меняется. */
    suspend fun updateProfile(profileId: Long, petName: String, appearance: PetAppearance): ProfileResult {
        val name = when (val result = ProfileRules.normalizeName(petName)) {
            is ProfileRules.Result.Invalid -> return ProfileResult.InvalidName(result.error)
            is ProfileRules.Result.Valid -> result.name
        }
        mutex.withLock { storage.updateProfile(profileId, name, appearance) }
        return ProfileResult.Saved(profileId)
    }

    /**
     * Выполняет команду [Game] и сохраняет результат, если она прошла.
     * `store.execute(id) { buy(it, "food_apple") }`
     */
    suspend fun execute(profileId: Long, command: Game.(GameState) -> GameResult): GameResult = mutex.withLock {
        val result = game.command(loadState(profileId))
        if (result is GameResult.Ok) storage.save(profileId, result.state)
        result
    }

    suspend fun submitTask(profileId: Long, taskId: String, input: TaskInput): TaskResult = mutex.withLock {
        val result = game.submitTask(loadState(profileId), taskId, input)
        if (result is TaskResult.Submitted) storage.save(profileId, result.state)
        result
    }

    /**
     * Начать заново тем же питомцем: имя, внешность и демо-режим остаются, прогресс — как
     * у только что созданного профиля (ТЗ п. 2.5.13: тестовый профиль сбрасывается к исходному состоянию).
     */
    suspend fun resetProgress(profileId: Long) = mutex.withLock {
        val profile = checkNotNull(storage.load(profileId)) { "Профиль $profileId не найден" }.profile
        storage.replace(profileId, game.newGame(profile.isDemo))
    }

    /** Сброс тестового профиля: профиль удаляется целиком, приложение возвращается к первому запуску. */
    suspend fun deleteProfile(profileId: Long) = mutex.withLock { storage.delete(profileId) }

    /** Удаление всех локальных данных из раздела взрослого (ТЗ п. 3.5). */
    suspend fun deleteAll() = mutex.withLock { storage.deleteAll() }

    private suspend fun loadState(profileId: Long): GameState =
        checkNotNull(storage.load(profileId)) { "Профиль $profileId не найден" }.state
}
