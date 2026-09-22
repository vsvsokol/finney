package ru.finney.pet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.PurchasePreview
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.pet.Emotion

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Всё, что ТЗ п. 2.5.3 требует показать на главном одновременно. */
    data class Ready(
        val petName: String,
        val appearance: PetAppearance,
        val isDemo: Boolean,
        val stats: PetStats,
        val emotion: Emotion,
        val level: Int,
        /** Доля до следующего уровня, 0..1 — дуга вокруг значка уровня. */
        val levelProgress: Float,
        val stage: Int,
        val balance: Int,
        val totalSavings: Int,
        /** null — цель не выбрана. */
        val goal: GoalProgress?,
        val periodNumber: Int,
        val phase: PeriodPhase,
        /** Сколько стоит закрыть нужное при текущих шкалах. */
        val needsHint: Int?,
        /** Первое открытое и ещё не пройденное задание. null — всё пройдено. */
        val nextTask: TaskDefinition?,
        /** Чем покормить: всё из магазина, что поднимает сытость. */
        val food: List<PurchasePreview>,
        /** Чем помыть: всё, что поднимает чистоту. */
        val care: List<PurchasePreview>,
    ) : HomeUiState {
        /** Период закрывается только после подтверждения плана. */
        val canClosePeriod: Boolean get() = phase == PeriodPhase.ACTIVE
    }
}

/** Какая панель ухода открыта поверх комнаты. */
enum class CareTarget(val title: String) {
    FOOD("Покормить"),
    BATH("Помыть"),
}

sealed interface HomeEvent {
    /** Открыть итоги: `PeriodResultRoute(periodNumber)`. */
    data class PeriodClosed(val periodNumber: Int) : HomeEvent

    data class Rejected(val reason: Rejection) : HomeEvent
}

class HomeViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = session.activeGame
        .filterNotNull()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun closePeriod() {
        viewModelScope.launch {
            val event = when (val result = session.execute { closePeriod(it) }) {
                is GameResult.Ok -> HomeEvent.PeriodClosed(result.state.periods.dropLast(1).last().number)
                is GameResult.Rejected -> HomeEvent.Rejected(result.reason)
            }
            _events.send(event)
        }
    }

    /** Купить предмет ухода. Состояние обновится само — оно читается из сохранённой игры. */
    fun buy(itemId: String) {
        viewModelScope.launch {
            when (val result = session.execute { buy(it, itemId) }) {
                is GameResult.Ok -> Unit
                is GameResult.Rejected -> _events.send(HomeEvent.Rejected(result.reason))
            }
        }
    }

    private fun toUiState(saved: SavedGame): HomeUiState.Ready {
        val state = saved.state
        val passed = state.attempts.filter { it.outcome == TaskOutcome.SUCCESS }.map { it.taskId }.toSet()
        val level = game.level(state)
        return HomeUiState.Ready(
            petName = saved.profile.petName,
            appearance = saved.profile.appearance,
            isDemo = state.isDemo,
            stats = state.pet,
            emotion = game.emotion(state),
            level = level,
            levelProgress = levelProgress(state.points, level),
            stage = game.stage(state),
            balance = state.balance,
            totalSavings = state.totalSavings,
            goal = game.goalProgress(state),
            periodNumber = state.currentPeriod.number,
            phase = state.currentPeriod.phase,
            needsHint = game.needsHint(state),
            nextTask = content.tasks.firstOrNull { it.id !in passed && game.isTaskAvailable(state, it) },
            // Что лежит на столе и что в ванной, решает не список имён, а эффект
            // предмета: добавят в контент новую еду — она появится на столе сама.
            food = previews(state) { it.effect.satiety > 0 },
            care = previews(state) { it.effect.hygiene > 0 },
        )
    }

    /**
     * Сколько пройдено до следующего уровня.
     *
     * Формула уровня здесь не повторяется — берётся тот же шаг очков из
     * economy.json, что и в домене. Сам уровень по-прежнему считает `Game`.
     * Отдельного `Game.levelProgress()` в домене нет, а заводить его — правка
     * в чужой зоне: понадобится ещё где-то, тогда и попросим.
     */
    private fun levelProgress(points: Int, level: Int): Float = with(content.economy) {
        if (level >= maxLevel) 1f else (points % pointsPerLevel) / pointsPerLevel.toFloat()
    }

    private fun previews(state: GameState, fits: (ShopItem) -> Boolean): List<PurchasePreview> =
        content.shop.filter(fits).mapNotNull { game.previewPurchase(state, it.id) }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { HomeViewModel(it.session, it.game, it.content) } }
        }
    }
}
