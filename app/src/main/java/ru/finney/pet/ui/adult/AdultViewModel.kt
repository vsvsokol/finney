package ru.finney.pet.ui.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.ui.progress.label
import kotlin.random.Random

/**
 * Барьер для взрослого — пример на умножение двузначного на однозначное (ТЗ п. 2.5.12:
 * «простой барьер, например решение арифметического примера»). Ребёнку 7–11 лет такой
 * пример в уме решить трудно, взрослому — нет. Каждый неверный ответ даёт новый пример.
 */
data class AdultGate(val a: Int, val b: Int, val wrongAnswer: Boolean = false) {
    val answer: Int get() = a * b

    companion object {
        fun random(random: Random = Random.Default, wrongAnswer: Boolean = false) =
            AdultGate(a = random.nextInt(12, 20), b = random.nextInt(6, 10), wrongAnswer = wrongAnswer)
    }
}

/** Тема Единой рамки и сколько её мини-игр пройдено. */
data class TopicProgress(val title: String, val passed: Int, val total: Int)

sealed interface AdultUiState {
    data object Loading : AdultUiState

    /** Раздел закрыт барьером. */
    data class Locked(val gate: AdultGate) : AdultUiState

    data class Ready(
        val petName: String,
        val isDemo: Boolean,
        val level: Int,
        val stage: Int,
        val closedPeriods: Int,
        val totalSavings: Int,
        val goalsCompleted: Int,
        val topics: List<TopicProgress>,
        /** Сколько бонуса ещё можно дать в этом периоде. */
        val bonusLeft: Int,
        val bonusStep: Int,
        val rejection: Rejection?,
    ) : AdultUiState
}

sealed interface AdultEvent {
    /** Профиль удалён. [hasProfile] — остался другой профиль, иначе — первый запуск. */
    data class ProfileDeleted(val hasProfile: Boolean) : AdultEvent
}

/**
 * Раздел для взрослого — ТЗ п. 2.5.12 и 3.5: цели приложения, пройденные темы и общий
 * прогресс без оценок ребёнка; бонус от взрослого; сброс и удаление локальных данных.
 */
class AdultViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
    random: Random = Random.Default,
) : ViewModel() {

    private val rnd = random
    private val gate = MutableStateFlow<AdultGate?>(AdultGate.random(rnd))
    private val rejection = MutableStateFlow<Rejection?>(null)

    private val _events = Channel<AdultEvent>(Channel.BUFFERED)
    val events: Flow<AdultEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<AdultUiState> =
        combine(session.activeGame.filterNotNull(), gate, rejection) { saved, gate, rejection ->
            if (gate != null) AdultUiState.Locked(gate) else toUiState(saved, rejection)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdultUiState.Loading)

    /** Проверить ответ на пример. Неверный — новый пример и подсказка, что ответ не подошёл. */
    fun answer(text: String) {
        val current = gate.value ?: return
        gate.value = if (text.trim().toIntOrNull() == current.answer) {
            null
        } else {
            AdultGate.random(rnd, wrongAnswer = true)
        }
    }

    fun addBonus() {
        viewModelScope.launch {
            val step = content.economy.parentBonus.step
            rejection.value = (session.execute { addParentBonus(it, step) } as? GameResult.Rejected)?.reason
        }
    }

    fun dismissRejection() = rejection.update { null }

    fun deleteProfile() {
        viewModelScope.launch {
            session.deleteActiveProfile()
            _events.send(AdultEvent.ProfileDeleted(hasProfile = session.restore()))
        }
    }

    private fun toUiState(saved: SavedGame, rejection: Rejection?): AdultUiState.Ready {
        val state = saved.state
        val passedSeries = state.attempts
            .filter { it.outcome == TaskOutcome.SUCCESS }
            .mapNotNull { content.task(it.taskId)?.seriesId }
            .toSet()
        val bonus = content.economy.parentBonus
        val given = state.ledger
            .filter { it.type == EntryType.PARENT_BONUS && it.periodNumber == state.currentPeriod.number }
            .sumOf { it.balanceDelta }
        return AdultUiState.Ready(
            petName = saved.profile.petName,
            isDemo = state.isDemo,
            level = game.level(state),
            stage = game.stage(state),
            closedPeriods = state.periods.count { it.result != null },
            totalSavings = state.totalSavings,
            goalsCompleted = state.ledger.count { it.type == EntryType.GOAL_COMPLETE },
            topics = TaskTheme.entries.map { theme ->
                val series = content.tasks.filter { it.theme == theme }.map { it.seriesId }.toSet()
                TopicProgress(theme.label(), passed = series.count { it in passedSeries }, total = series.size)
            },
            bonusLeft = (bonus.maxPerPeriod - given).coerceAtLeast(0),
            bonusStep = bonus.step,
            rejection = rejection,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { AdultViewModel(it.session, it.game, it.content) } }
        }
    }
}
