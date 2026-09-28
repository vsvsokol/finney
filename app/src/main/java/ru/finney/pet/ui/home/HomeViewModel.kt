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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.LevelCheck
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
import ru.finney.pet.domain.pet.Emotion
import ru.finney.pet.ui.components.ActionFeedback
import ru.finney.pet.ui.components.changesBetween

/** Условий у уровня три: нужное, план, копилка. */
const val LEVEL_CONDITIONS = 3

/**
 * Одно действие, которое главный экран подсвечивает прямо сейчас (плейтест 28.09:
 * «ничего не понятно, что делать»). Порядок — как проходится уровень: план, еда и мытьё,
 * игра уровня, сон, завершение. Выбирает [nextStepFor].
 */
enum class NextStep {
    /** Плана нет — значок уровня, внутри «Составить план». */
    PLAN,
    FEED,
    WASH,
    LEVEL_GAME,
    SLEEP,
    /** Спит, и сна уже хватает для уровня — «Разбудить». Выбирает экран: сон идёт по часам. */
    WAKE,
    /** Условия выполнены — значок уровня, внутри «Завершить уровень». */
    FINISH,
    /** Отложить в копилку — плашка копилки. */
    SAVE,
}

/**
 * Следующий шаг по чек-листу ядра и шкалам. null — подсказывать нечего: питомец спит
 * или всё, что можно сделать руками, сделано, а условий не хватает (траты не по плану).
 *
 * Нужное — по порогу из конфига ядра, тому же, по которому [LevelCheck.needsCovered]:
 * ниже порога еда и мытьё сразу — первой зовём самую низкую шкалу.
 *
 * Сон — последним перед завершением: пока питомец спит, остальное закрыто, и уложи
 * его первым — ребёнок сидел бы перед капсулой, не сделав ничего другого (плейтест).
 */
internal fun nextStepFor(check: LevelCheck, stats: PetStats, needsThreshold: Int, asleep: Boolean): NextStep? {
    if (asleep) return null
    if (!check.planConfirmed) return NextStep.PLAN
    listOf(NextStep.FEED to stats.satiety, NextStep.WASH to stats.hygiene)
        .filter { it.second < needsThreshold }
        .minByOrNull { it.second }
        ?.let { return it.first }
    if (check.levelTaskId != null && check.gameRequired && !check.gamePassed) return NextStep.LEVEL_GAME
    if (stats.energy < needsThreshold) return NextStep.SLEEP
    if (check.willPass) return NextStep.FINISH
    if (!check.savingsAdded) return NextStep.SAVE
    return null
}

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
        /** Условия текущего уровня на эту минуту: чек-лист и дуга вокруг значка. */
        val check: LevelCheck,
        val stage: Int,
        val balance: Int,
        val totalSavings: Int,
        /** null — цель не выбрана. */
        val goal: GoalProgress?,
        val periodNumber: Int,
        val phase: PeriodPhase,
        /** Сколько стоит закрыть нужное при текущих шкалах. */
        val needsHint: Int?,
        /** Название игры уровня: её id — в [check]. null — игры в этом периоде нет. */
        val levelGame: String?,
        /** Чем покормить: всё из магазина, что поднимает сытость. */
        val food: List<PurchasePreview>,
        /** Чем помыть: всё, что поднимает чистоту. */
        val care: List<PurchasePreview>,
        /** Что надето сейчас — id аксессуара из магазина. */
        val worn: String? = null,
        /** Питомец спит; null — не спит. */
        val sleep: SleepInfo? = null,
        /** Купленные игрушки — id из магазина. Лежат на полу в зале. */
        val toys: List<String> = emptyList(),
        /** Сколько настроения игрушки ещё дадут в этой сессии игры; 0 — питомец наигрался. */
        val playLeft: Int = 0,
        /** Прибавка настроения за одно потряхивание игрушкой. */
        val moodPerShake: Int = 0,
        /** Настроение ниже порога грусти: питомец вздыхает, шкала зовёт на неё нажать. */
        val moodLow: Boolean = false,
        /** Что подсветить сейчас; null — ничего. */
        val nextStep: NextStep? = null,
    ) : HomeUiState {
        /** Уровень завершается только после подтверждения плана. */
        val canClosePeriod: Boolean get() = phase == PeriodPhase.ACTIVE

        /** Дуга вокруг значка уровня: сколько условий уровня уже выполнено, 0..1. Игра уровня — ещё одно. */
        val levelProgress: Float get() {
            val hasGame = check.levelTaskId != null
            val done = check.met + if (hasGame && check.gamePassed) 1 else 0
            return done / (LEVEL_CONDITIONS + if (hasGame) 1 else 0).toFloat()
        }
    }
}

/**
 * Сон идёт по часам: [energyFrom] — с чем уснул, к [endsAt] сон дорастёт до 100.
 * Экран сам двигает кольцо сна по времени и будит питомца в [endsAt].
 */
data class SleepInfo(
    val since: Long,
    val endsAt: Long,
    val energyFrom: Int,
    /** С этой минуты сна хватает для уровня: будить можно, условие «выспался» выполнено. */
    val enoughAt: Long = since,
) {
    fun energyAt(now: Long): Int {
        val slept = ((now - since).toFloat() / (endsAt - since)).coerceIn(0f, 1f)
        return energyFrom + ((100 - energyFrom) * slept).toInt()
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

    /** Покупка ухода прошла: что изменилось и что дальше (ТЗ п. 2.5.9). */
    data class Purchased(val feedback: ActionFeedback) : HomeEvent

    /** Питомец проснулся — сам или разбудили: сколько выспался (ТЗ п. 2.5.9). */
    data class Woke(val feedback: ActionFeedback) : HomeEvent
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

    init {
        // Сохранение до игры уровня: текущему периоду она выдаётся при первом входе.
        viewModelScope.launch {
            val saved = session.activeGame.filterNotNull().first()
            if (saved.state.currentPeriod.levelTaskId == null) session.execute { assignLevelGame(it) }
        }
    }

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
            val before = session.activeGame.first()?.state
            when (val result = session.execute { buy(it, itemId) }) {
                is GameResult.Ok -> {
                    val item = content.item(itemId)
                    if (before != null && item != null) {
                        _events.send(
                            HomeEvent.Purchased(
                                ActionFeedback(
                                    title = "Купили: ${item.label}",
                                    lines = changesBetween(before, result.state),
                                    why = "Еда и мытьё — это нужное.",
                                    next = "Кольца у кнопок покажут, что ещё нужно.",
                                    itemId = item.id,
                                ),
                            ),
                        )
                    }
                }
                is GameResult.Rejected -> _events.send(HomeEvent.Rejected(result.reason))
            }
        }
    }

    /** Уложить спать в капсулу. Бесплатно: сон не покупают. */
    fun sleep() {
        viewModelScope.launch {
            (session.execute { sleep(it) } as? GameResult.Rejected)?.let {
                _events.send(HomeEvent.Rejected(it.reason))
            }
        }
    }

    /**
     * Разбудить — или отметить, что проснулся сам: срок вышел, и хранилище уже
     * разбудило его перед командой (тогда `wake` отвечает NotAsleep). Итог в обоих
     * случаях считается от состояния до пробуждения.
     */
    fun wake() {
        viewModelScope.launch {
            val before = session.activeGame.first()?.state?.takeIf { it.sleepingSince != null } ?: return@launch
            val after = when (val result = session.execute { wake(it) }) {
                is GameResult.Ok -> result.state
                is GameResult.Rejected -> (game.wake(before) as? GameResult.Ok)?.state ?: return@launch
            }
            val full = after.pet.energy >= 100
            _events.send(
                HomeEvent.Woke(
                    ActionFeedback(
                        // Одной строкой: на главном видна только она и значки шкал.
                        title = if (full) "Выспался!" else "Проснулся раньше — сон не полный",
                        lines = changesBetween(before, after),
                        why = if (full) "Сон — это нужное. И он бесплатный." else "Сон набирается постепенно: чем дольше спит, тем больше.",
                        next = "К следующему уровню сон снова убудет — следи за кольцом у кнопки зала.",
                    ),
                ),
            )
        }
    }

    /**
     * Поиграли с игрушкой: [shakes] потряхиваний рядом с питомцем с прошлого раза.
     * Экран копит их и присылает пачкой — сохранять каждое движение пальца незачем.
     */
    fun play(toyId: String, shakes: Int) {
        if (shakes <= 0) return
        viewModelScope.launch { session.execute { play(it, toyId, shakes) } }
    }

    private fun toUiState(saved: SavedGame): HomeUiState.Ready {
        val state = saved.state
        val level = game.level(state)
        val check = game.levelCheck(state)
        return HomeUiState.Ready(
            petName = saved.profile.petName,
            appearance = saved.profile.appearance,
            isDemo = state.isDemo,
            stats = state.pet,
            emotion = game.emotion(state),
            level = level,
            check = check,
            stage = game.stage(state),
            balance = state.balance,
            totalSavings = state.totalSavings,
            goal = game.goalProgress(state),
            periodNumber = state.currentPeriod.number,
            phase = state.currentPeriod.phase,
            needsHint = game.needsHint(state),
            levelGame = state.currentPeriod.levelTaskId?.let { content.task(it)?.title },
            // Что лежит на столе и что в ванной, решает не список имён, а эффект
            // предмета: добавят в контент новую еду — она появится на столе сама.
            food = previews(state) { it.effect.satiety > 0 },
            care = previews(state) { it.effect.hygiene > 0 },
            worn = state.wornItemId,
            sleep = state.sleepingSince?.let { since ->
                val endsAt = game.sleepEndsAt(state)!!
                val threshold = content.economy.pet.needsThreshold
                val energy = state.pet.energy
                SleepInfo(
                    since = since,
                    endsAt = endsAt,
                    energyFrom = energy,
                    // Сон растёт ровно, как в [SleepInfo.energyAt]: до порога — та же доля пути.
                    // Секунда сверху — ядро округляет сон вниз, и разбуженный ровно в срок
                    // получил бы на единицу меньше порога.
                    enoughAt = if (energy >= threshold) {
                        since
                    } else {
                        since + (endsAt - since) * (threshold - energy) / (100 - energy) + 1_000
                    },
                )
            },
            toys = game.toys(state).map { it.id },
            playLeft = game.playMoodLeft(state),
            moodPerShake = content.economy.play.moodPerShake,
            moodLow = state.pet.mood < content.economy.pet.emotionLow,
            nextStep = nextStepFor(
                check = check,
                stats = state.pet,
                needsThreshold = content.economy.pet.needsThreshold,
                asleep = state.sleepingSince != null,
            ),
        )
    }

    private fun previews(state: GameState, fits: (ShopItem) -> Boolean): List<PurchasePreview> =
        content.shop.filter(fits).mapNotNull { game.previewPurchase(state, it.id) }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { HomeViewModel(it.session, it.game, it.content) } }
        }
    }
}
