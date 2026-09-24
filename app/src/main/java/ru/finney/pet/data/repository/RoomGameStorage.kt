package ru.finney.pet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.finney.pet.data.db.GameDao
import ru.finney.pet.data.db.toDomain
import ru.finney.pet.data.db.toEntity
import ru.finney.pet.data.db.toRows
import ru.finney.pet.domain.game.GameStorage
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.Profile
import ru.finney.pet.domain.model.SavedGame

class RoomGameStorage(private val dao: GameDao) : GameStorage {

    override fun observeProfiles(): Flow<List<Profile>> =
        dao.observeProfiles().map { profiles -> profiles.map { it.toDomain() } }

    override fun observeGame(profileId: Long): Flow<SavedGame?> =
        dao.observeGame(profileId).map { it?.toDomain() }

    override suspend fun load(profileId: Long): SavedGame? = dao.loadGame(profileId)?.toDomain()

    override suspend fun create(profile: Profile, state: GameState): Long =
        dao.createGame(profile.copy(id = 0).toEntity(), state.toRows(profileId = 0))

    override suspend fun save(profileId: Long, state: GameState) = dao.writeGame(profileId, state.toRows(profileId))

    override suspend fun replace(profileId: Long, state: GameState) = dao.replaceGame(profileId, state.toRows(profileId))

    override suspend fun updateProfile(profileId: Long, petName: String, appearance: PetAppearance) =
        dao.updateProfile(profileId, petName, appearance.character, appearance.bodyColor, appearance.eyes)

    override suspend fun delete(profileId: Long) = dao.deleteProfile(profileId)

    override suspend fun deleteAll() = dao.deleteAllProfiles()
}
