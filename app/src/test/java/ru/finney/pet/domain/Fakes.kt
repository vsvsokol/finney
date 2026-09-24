package ru.finney.pet.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import ru.finney.pet.domain.game.ActiveProfileStorage
import ru.finney.pet.domain.game.GameStorage
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.Profile
import ru.finney.pet.domain.model.SavedGame

/** Хранилище в памяти. yield() в каждом методе даёт корутинам перемешаться, как с настоящей базой. */
class FakeStorage : GameStorage {
    val all = MutableStateFlow<Map<Long, SavedGame>>(emptyMap())
    var saves = 0
    private var nextId = 1L

    override fun observeProfiles(): Flow<List<Profile>> = all.map { games -> games.values.map { it.profile } }
    override fun observeGame(profileId: Long): Flow<SavedGame?> = all.map { it[profileId] }

    override suspend fun load(profileId: Long): SavedGame? {
        yield()
        return all.value[profileId]
    }

    override suspend fun create(profile: Profile, state: GameState): Long {
        yield()
        val id = nextId++
        all.value += id to SavedGame(profile.copy(id = id), state)
        return id
    }

    override suspend fun save(profileId: Long, state: GameState) {
        yield()
        saves++
        all.value += profileId to all.value.getValue(profileId).copy(state = state)
    }

    override suspend fun replace(profileId: Long, state: GameState) {
        yield()
        all.value += profileId to all.value.getValue(profileId).copy(state = state)
    }

    override suspend fun updateProfile(profileId: Long, petName: String, appearance: PetAppearance) {
        val saved = all.value.getValue(profileId)
        all.value += profileId to saved.copy(profile = saved.profile.copy(petName = petName, appearance = appearance))
    }

    override suspend fun delete(profileId: Long) {
        all.value -= profileId
    }

    override suspend fun deleteAll() {
        all.value = emptyMap()
    }
}

/** DataStore в памяти. */
class FakeActiveProfile(initial: Long? = null) : ActiveProfileStorage {
    override val activeProfileId = MutableStateFlow(initial)

    override suspend fun setActiveProfileId(profileId: Long?) {
        activeProfileId.value = profileId
    }
}
