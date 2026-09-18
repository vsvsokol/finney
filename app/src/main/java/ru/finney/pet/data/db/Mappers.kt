package ru.finney.pet.data.db

import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.LedgerEntry
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.Period
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.PeriodResult
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.domain.model.Profile
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.TaskAttempt

// domain ⇄ Room. Для пустого профиля profileId = 0, настоящий id проставляет GameRows.withProfileId.

fun Profile.toEntity() = ProfileEntity(
    id = id,
    petName = petName,
    petCharacter = appearance.character,
    bodyColor = appearance.bodyColor,
    eyes = appearance.eyes,
    activeGoalId = null,
    isDemo = isDemo,
    createdAt = createdAt,
)

fun ProfileEntity.toDomain() = Profile(
    id = id,
    petName = petName,
    appearance = PetAppearance(petCharacter, bodyColor, eyes),
    isDemo = isDemo,
    createdAt = createdAt,
)

fun GameState.toRows(profileId: Long) = GameRows(
    activeGoalId = activeGoalId,
    pet = PetStateEntity(profileId, pet.satiety, pet.hygiene, pet.mood),
    periods = periods.map { it.toEntity(profileId) },
    ledger = ledger.map { it.toEntity(profileId) },
    attempts = attempts.map { TaskAttemptEntity(0, profileId, it.periodNumber, it.taskId, it.outcome, it.reward, it.createdAt) },
)

fun ProfileWithGame.toDomain(): SavedGame {
    val pet = checkNotNull(pet) { "У профиля ${profile.id} нет состояния питомца" }
    return SavedGame(
        profile = profile.toDomain(),
        state = GameState(
            isDemo = profile.isDemo,
            pet = PetStats(pet.satiety, pet.hygiene, pet.mood),
            activeGoalId = profile.activeGoalId,
            periods = periods.sortedBy { it.number }.map { it.toDomain() },
            ledger = ledger.sortedBy { it.id }.map { it.toDomain() },
            attempts = attempts.sortedBy { it.id }.map {
                TaskAttempt(it.periodNumber, it.taskId, it.outcome, it.reward, it.createdAt)
            },
        ),
    )
}

private fun Period.toEntity(profileId: Long) = PeriodEntity(
    profileId = profileId,
    number = number,
    stage = stage,
    phase = phase,
    budget = plan?.budget,
    plannedNeeds = plan?.needs,
    plannedWants = plan?.wants,
    plannedSavings = plan?.savings,
    factNeeds = result?.facts?.needs,
    factWants = result?.facts?.wants,
    factSavings = result?.facts?.savings,
    unplannedIncome = result?.facts?.unplannedIncome,
    needsCovered = result?.needsCovered,
    planMatched = result?.planMatched,
    savingsAdded = result?.savingsAdded,
    successfulTasks = result?.successfulTasks,
    pointsEarned = result?.points,
)

private fun PeriodEntity.toDomain() = Period(
    number = number,
    stage = stage,
    phase = phase,
    plan = budget?.let { Plan(it, plannedNeeds!!, plannedWants!!, plannedSavings!!) },
    result = pointsEarned?.let {
        PeriodResult(
            facts = PeriodFacts(factNeeds!!, factWants!!, factSavings!!, unplannedIncome!!),
            needsCovered = needsCovered!!,
            planMatched = planMatched!!,
            savingsAdded = savingsAdded!!,
            successfulTasks = successfulTasks!!,
            points = it,
        )
    },
)

private fun LedgerEntry.toEntity(profileId: Long) = LedgerEntryEntity(
    id = 0,
    profileId = profileId,
    periodNumber = periodNumber,
    type = type,
    balanceDelta = balanceDelta,
    savingsDelta = savingsDelta,
    category = category,
    itemId = itemId,
    goalId = goalId,
    taskId = taskId,
    unplanned = unplanned,
    createdAt = createdAt,
)

private fun LedgerEntryEntity.toDomain() = LedgerEntry(
    periodNumber = periodNumber,
    type = type,
    balanceDelta = balanceDelta,
    savingsDelta = savingsDelta,
    category = category,
    itemId = itemId,
    goalId = goalId,
    taskId = taskId,
    unplanned = unplanned,
    createdAt = createdAt,
)
