package ru.finney.pet.domain.model

import kotlinx.serialization.Serializable

/** Шкалы питомца, каждая 0..100. */
@Serializable
data class PetStats(
    val satiety: Int,
    val hygiene: Int,
    val mood: Int,
    /** Сон: бодрость питомца. Восстанавливается бесплатно — сном в капсуле, см. [ru.finney.pet.domain.game.Game.sleep]. */
    val energy: Int,
)

enum class PeriodPhase {
    /** Доход начислен, шкалы снижены. Можно выполнять задания и составлять план. */
    PLANNING,

    /** План подтверждён. Можно покупать и пополнять копилку. */
    ACTIVE,

    /** Итоги подведены, очки начислены. */
    CLOSED,
}

data class Plan(
    val budget: Int,
    val needs: Int,
    val wants: Int,
    val savings: Int,
) {
    val planned: Int get() = needs + wants + savings
    val remainder: Int get() = budget - planned
}

data class PeriodFacts(
    val needs: Int,
    val wants: Int,
    /** Пополнения минус снятия. Достижение цели сюда не входит. */
    val savings: Int,
    /** Доход, полученный после подтверждения плана. */
    val unplannedIncome: Int,
)

data class PeriodResult(
    val facts: PeriodFacts,
    val needsCovered: Boolean,
    val planMatched: Boolean,
    val savingsAdded: Boolean,
    /** Игра уровня пройдена. У периодов, закрытых до появления игры уровня, — true. */
    val gamePassed: Boolean,
    val successfulTasks: Int,
    val points: Int,
)

data class Period(
    val number: Int,
    /** Стадия на момент открытия: определяет снижение шкал и доход. */
    val stage: Int,
    val phase: PeriodPhase,
    val plan: Plan? = null,
    val result: PeriodResult? = null,
    /**
     * Игра уровня — мини-игра, которую обязательно пройти в этом периоде. Выбирается
     * случайно при открытии периода, не та же, что в прошлом. null — игры нет:
     * период открыт до её появления или ни одна игра ещё не открыта.
     */
    val levelTaskId: String? = null,
)

enum class EntryType {
    INCOME,
    TASK_REWARD,
    PARENT_BONUS,
    PURCHASE,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAW,
    GOAL_COMPLETE,
}

/**
 * Одна операция. Баланс и накопления нигде не хранятся — это суммы по операциям.
 * Подпись для ребёнка строит UI по [type] и id из контента.
 */
data class LedgerEntry(
    val periodNumber: Int,
    val type: EntryType,
    val balanceDelta: Int,
    val savingsDelta: Int = 0,
    val category: Category? = null,
    val itemId: String? = null,
    val goalId: String? = null,
    val taskId: String? = null,
    val unplanned: Boolean = false,
    val createdAt: Long,
)

data class TaskAttempt(
    val periodNumber: Int,
    val taskId: String,
    val outcome: TaskOutcome,
    /** 0, если награда за это задание уже выдавалась. */
    val reward: Int,
    val createdAt: Long,
)

/** Полное состояние одного профиля. Меняется только через [ru.finney.pet.domain.game.Game]. */
data class GameState(
    val isDemo: Boolean,
    val pet: PetStats,
    val activeGoalId: String?,
    val periods: List<Period>,
    val ledger: List<LedgerEntry>,
    val attempts: List<TaskAttempt>,
    /** Аксессуар, который сейчас на питомце. Только из купленных; null — ничего не надето. */
    val wornItemId: String? = null,
    /** Когда питомец лёг спать, мс; null — не спит. Сколько он уже выспался — [ru.finney.pet.domain.game.Game.energyAt]. */
    val sleepingSince: Long? = null,
    /** Сколько настроения уже дала игра с игрушками в текущей сессии, см. [ru.finney.pet.domain.game.Game.play]. */
    val playMood: Int = 0,
    /** Когда началась текущая сессия игры, мс; null — сессии нет. */
    val playSince: Long? = null,
) {
    val currentPeriod: Period get() = periods.last()

    val balance: Int get() = ledger.sumOf { it.balanceDelta }

    val totalSavings: Int get() = ledger.sumOf { it.savingsDelta }

    val points: Int get() = periods.sumOf { it.result?.points ?: 0 }

    fun goalSaved(goalId: String): Int = ledger.filter { it.goalId == goalId }.sumOf { it.savingsDelta }

    fun isGoalCompleted(goalId: String): Boolean =
        ledger.any { it.type == EntryType.GOAL_COMPLETE && it.goalId == goalId }

    /** Вещь куплена или получена за достигнутую цель. */
    fun owns(itemId: String): Boolean = ledger.any {
        (it.type == EntryType.PURCHASE || it.type == EntryType.GOAL_COMPLETE) && it.itemId == itemId
    }
}
