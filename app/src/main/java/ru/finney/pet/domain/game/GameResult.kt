package ru.finney.pet.domain.game

import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.tasks.TaskInputError

sealed interface GameResult {
    data class Ok(val state: GameState) : GameResult

    /** Команда не выполнена, состояние не изменилось. UI объясняет причину ребёнку. */
    data class Rejected(val reason: Rejection) : GameResult
}

sealed interface Rejection {
    /** Покупки и копилка до подтверждения плана. */
    data object PlanNotConfirmed : Rejection
    data object PlanAlreadyConfirmed : Rejection
    data class PlanExceedsBudget(val budget: Int, val planned: Int) : Rejection

    /** Сумма ≤ 0 там, где нужна положительная, или отрицательная часть плана. */
    data object InvalidAmount : Rejection

    data class InsufficientFunds(val needed: Int, val balance: Int) : Rejection {
        val shortage: Int get() = needed - balance
    }

    data object AlreadyOwned : Rejection
    data class UnknownItem(val id: String) : Rejection

    data class UnknownGoal(val id: String) : Rejection
    data object NoActiveGoal : Rejection
    data object GoalAlreadyCompleted : Rejection
    data class InsufficientSavings(val requested: Int, val saved: Int) : Rejection
    data class GoalNotReached(val price: Int, val saved: Int) : Rejection

    data class BonusNotOnStep(val step: Int) : Rejection
    data class BonusLimitExceeded(val left: Int) : Rejection

    data class UnknownTask(val id: String) : Rejection
    data object TaskLocked : Rejection
    data class InvalidTaskInput(val error: TaskInputError) : Rejection
}
