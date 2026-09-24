package ru.finney.pet.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskReward
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError

/** Вступление → игра → итог. Итог показывается при любом исходе (ТЗ п. 2.5.8). */
enum class TaskPhase { INTRO, PLAY, RESULT }

/** [input] — ответ ребёнка: итог показывает по нему, что взято в корзину, какие монеты дал. */
data class TaskOutcomeUi(val outcome: TaskOutcome, val details: TaskDetails, val reward: Int, val input: TaskInput)

sealed interface TaskUiState {
    data object Loading : TaskUiState
    data object NotFound : TaskUiState

    data class Ready(
        val task: TaskDefinition,
        val character: PetCharacter,
        val balance: Int,
        /** false — задание откроется в следующих периодах. */
        val available: Boolean,
        /** Уже пройдено успешно: награды за успех больше не будет, играть можно. */
        val completed: Boolean,
        val reward: TaskReward,
        val phase: TaskPhase,
        /** Номер попытки: новая попытка — игра с чистого листа. */
        val attempt: Int,
        val result: TaskOutcomeUi?,
        val inputError: TaskInputError?,
    ) : TaskUiState
}

/** Одна мини-игра или задание. Правила и награду считает ядро, экран только показывает. */
class TaskViewModel(
    private val taskId: String,
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    private data class Local(
        val phase: TaskPhase = TaskPhase.INTRO,
        val attempt: Int = 0,
        val result: TaskOutcomeUi? = null,
        val inputError: TaskInputError? = null,
        val submitting: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<TaskUiState> =
        combine(session.activeGame.filterNotNull(), local, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState.Loading)

    fun start() = local.update { it.copy(phase = TaskPhase.PLAY, inputError = null) }

    fun replay() = local.update { Local(phase = TaskPhase.PLAY, attempt = it.attempt + 1) }

    fun inputSeen() = local.update { it.copy(inputError = null) }

    fun submit(input: TaskInput) {
        if (local.value.submitting) return
        local.update { it.copy(submitting = true) }
        viewModelScope.launch {
            when (val r = session.submitTask(taskId, input)) {
                is TaskResult.Submitted -> local.update {
                    it.copy(
                        phase = TaskPhase.RESULT,
                        result = TaskOutcomeUi(r.outcome, r.details, r.reward, input),
                        inputError = null,
                        submitting = false,
                    )
                }
                // Ввод не принят — не исход: ребёнок остаётся в игре и исправляет.
                is TaskResult.Rejected -> local.update {
                    it.copy(inputError = (r.reason as? Rejection.InvalidTaskInput)?.error, submitting = false)
                }
            }
        }
    }

    private fun toUiState(saved: SavedGame, local: Local): TaskUiState {
        val task = content.task(taskId) ?: return TaskUiState.NotFound
        val state = saved.state
        return TaskUiState.Ready(
            task = task,
            character = saved.profile.appearance.character,
            balance = state.balance,
            available = game.isTaskAvailable(state, task),
            completed = state.attempts.any { it.taskId == taskId && it.outcome == TaskOutcome.SUCCESS },
            reward = task.reward ?: content.economy.taskReward,
            phase = local.phase,
            attempt = local.attempt,
            result = local.result,
            inputError = local.inputError,
        )
    }

    companion object {
        fun factory(taskId: String) = viewModelFactory {
            initializer { appContainer().let { TaskViewModel(taskId, it.session, it.game, it.content) } }
        }
    }
}
