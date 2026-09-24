package ru.finney.pet.domain.game

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.tasks.TaskInput

/**
 * [GameStore], привязанный к открытому профилю. Экраны игры работают через него и не передают
 * profileId: какой профиль открыт, знает только сессия.
 */
class Session(
    private val store: GameStore,
    private val activeProfile: ActiveProfileStorage,
) {
    private val mutex = Mutex()

    /** Состояние открытого профиля. null — профиль не выбран или удалён. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGame: Flow<SavedGame?> = activeProfile.activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(null) else store.observeGame(id)
    }

    /**
     * Есть ли профиль, с которым можно продолжить игру. Если сохранённый id устарел
     * (профиль удалён, база и настройки восстановлены из разных копий), открывает первый
     * из оставшихся профилей.
     */
    suspend fun restore(): Boolean = mutex.withLock {
        val profiles = store.observeProfiles().first()
        val savedId = activeProfile.activeProfileId.first()
        val id = profiles.firstOrNull { it.id == savedId }?.id ?: profiles.firstOrNull()?.id
        if (id != savedId) activeProfile.setActiveProfileId(id)
        id != null
    }

    /** Создаёт профиль и сразу открывает его. */
    suspend fun createProfile(petName: String, appearance: PetAppearance, isDemo: Boolean = false): ProfileResult =
        mutex.withLock {
            store.createProfile(petName, appearance, isDemo).also { result ->
                if (result is ProfileResult.Saved) activeProfile.setActiveProfileId(result.profileId)
            }
        }

    suspend fun updateProfile(petName: String, appearance: PetAppearance): ProfileResult =
        store.updateProfile(requireProfileId(), petName, appearance)

    /** `session.execute { buy(it, "food_apple") }` — см. [GameStore.execute]. */
    suspend fun execute(command: Game.(GameState) -> GameResult): GameResult =
        store.execute(requireProfileId(), command)

    suspend fun submitTask(taskId: String, input: TaskInput): TaskResult =
        store.submitTask(requireProfileId(), taskId, input)

    /** Прогресс открытого профиля — к исходному состоянию, питомец остаётся. См. [GameStore.resetProgress]. */
    suspend fun resetProgress() = store.resetProgress(requireProfileId())

    /** Сброс тестового профиля. После него открыт другой профиль, если он есть; иначе — первый запуск. */
    suspend fun deleteActiveProfile() = mutex.withLock {
        store.deleteProfile(requireProfileId())
        activeProfile.setActiveProfileId(store.observeProfiles().first().firstOrNull()?.id)
    }

    /** Удаление всех локальных данных. Приложение возвращается к первому запуску. */
    suspend fun deleteAll() = mutex.withLock {
        store.deleteAll()
        activeProfile.setActiveProfileId(null)
    }

    private suspend fun requireProfileId(): Long =
        checkNotNull(activeProfile.activeProfileId.first()) { "Нет открытого профиля" }
}
