package ru.finney.pet.ui.home

import ru.finney.pet.R
import androidx.annotation.DrawableRes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import ru.finney.pet.ui.components.buttonFill
import ru.finney.pet.ui.theme.FinneyInkFaded
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.RadiusCard
import ru.finney.pet.ui.theme.RadiusCheckbox
import ru.finney.pet.ui.theme.RadiusPanel
import ru.finney.pet.ui.theme.StrokeBold
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.motion.motionEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.LoopWhile
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyCream
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.pet.Emotion
import androidx.compose.ui.layout.layout
import androidx.compose.animation.core.snap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import ru.finney.pet.domain.model.Category
import ru.finney.pet.ui.components.CategoryBanner
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.WalletButton
import ru.finney.pet.ui.components.FeedbackDialog
import ru.finney.pet.ui.components.ActionFeedbackCard
import ru.finney.pet.ui.components.ActionFeedback
import ru.finney.pet.domain.game.Rejection
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateMapOf
import ru.finney.pet.domain.game.LevelCheck
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FeedbackSound
import ru.finney.pet.ui.components.FillRing
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyQuietButton
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.FinneyNeedButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.HappinessBar
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.grownPet
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.debug.DebugPanel
import androidx.compose.animation.core.animateFloatAsState
import ru.finney.pet.ui.components.OutlinedText
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import ru.finney.pet.ui.pet.skin
import ru.finney.pet.ui.room.FeedingGame
import ru.finney.pet.ui.room.WashingGame
import ru.finney.pet.ui.room.CareBlock
import ru.finney.pet.ui.room.CareOption
import ru.finney.pet.ui.room.CarePanel
import ru.finney.pet.ui.room.DOOR_MS
import ru.finney.pet.ui.room.WALK_MS
import ru.finney.pet.ui.room.RoomScene
import ru.finney.pet.ui.room.RoomSpot
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.RadiusField
import ru.finney.pet.ui.theme.StrokeThin
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.motion.LocalAnimations
import ru.finney.pet.ui.motion.PortalReveal
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

// Главный экран по макету main_screen_layout: сверху деньги, уровень и «бургер»,
// слева по центру шкала настроения, внизу три кнопки комнат. Больше в макете
// на главном ничего нет — ни имени, ни номера периода, ни подсказки про нужное,
// ни отдельных полос сытости и чистоты: эти две переехали в кольца кнопок.
//
// Две строки макет не предусматривает, а ТЗ п. 2.5.3 требует: копилка
// и активное задание. Они стоят узкими плашками в кремовой полосе над комнатой,
// где пустая стена, и ничего собой не закрывают. Расхождение с макетом
// намеренное и записано в docs/requirements-matrix.md.
//
// Фоном лежит комната (ui/room). Предмет в ней ровно один, и его меняют те же
// нижние кнопки: комната одна, а стол, ванна и пустой зал — её состояния.
// Уход происходит там же, где живёт питомец, — ребёнок не уходит в меню, чтобы
// покормить. Интерфейс плавает поверх комнаты отдельным пластом: у комнаты
// обводка чёрная, у кита FinneyInk, и сливаться им не положено.

/**
 * На сколько шкала настроения заходит в поле экрана: от края остаётся 6 dp
 * вместо общих 16. В макете она стоит почти вплотную к краю.
 */
private val HappinessEdgeShift: Dp = 10.dp

@Composable
fun HomeScreen(
    onOpenBudget: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (taskId: String) -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onPeriodClosed: (periodNumber: Int) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Итог покупки ухода висит несколько секунд и сам уходит: игра не прерывается окном,
    // а ребёнок успевает увидеть, что стало с деньгами и шкалой (ТЗ п. 2.5.9).
    var purchased by remember { mutableStateOf<ActionFeedback?>(null) }
    var rejected by remember { mutableStateOf<Rejection?>(null) }

    LaunchedEffect(purchased) {
        if (purchased != null) {
            delay(PurchaseFeedbackMillis)
            purchased = null
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.PeriodClosed -> onPeriodClosed(event.periodNumber)
                is HomeEvent.Rejected -> rejected = event.reason
                is HomeEvent.Purchased -> purchased = event.feedback
                // Итог сна — уже проснувшемуся, той же карточкой, что и покупка.
                is HomeEvent.Woke -> purchased = event.feedback
            }
        }
    }

    when (val s = state) {
        HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }
        is HomeUiState.Ready -> HomeContent(
            state = s,
            onOpenBudget = onOpenBudget,
            onOpenShop = onOpenShop,
            onOpenGoals = onOpenGoals,
            onOpenTasks = onOpenTasks,
            onOpenTask = onOpenTask,
            onOpenWardrobe = onOpenWardrobe,
            onOpenProgress = onOpenProgress,
            onOpenSettings = onOpenSettings,
            onOpenHelp = onOpenHelp,
            onClosePeriod = viewModel::closePeriod,
            onBuy = viewModel::buy,
            onSleep = viewModel::sleep,
            onWake = viewModel::wake,
            onSleepCard = viewModel::answerSleepCard,
            onPlay = viewModel::play,
        )
    }

    FeedbackSound(feedback = purchased, rejection = null)
    purchased?.let { feedback ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 96.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            ActionFeedbackCard(
                feedback = feedback,
                compact = true,
                modifier = Modifier.clickable(onClickLabel = "Скрыть") { purchased = null },
            )
        }
    }
    FeedbackDialog(
        feedback = null,
        rejection = rejected,
        onDismiss = { rejected = null },
        actionLabel = if (rejected is Rejection.PlanNotConfirmed) "К плану расходов" else null,
        onAction = onOpenBudget,
    )
    // Пришли из портала — зал раскрывается из круга в центре, поверх всего.
    PortalReveal(ready = state is HomeUiState.Ready)
}

private const val PurchaseFeedbackMillis = 5_000L

/** Сколько пути пальца с игрушкой рядом с питомцем — одно потряхивание. */
private val ShakeTravel = 40.dp

/** Раз в сколько потряхивания уходят в игру, пока игрушку не отпустили. */
private const val PlayFlushMillis = 800L

/** Не чаще раза в столько питомец смеётся вслух и подпрыгивает от игры. */
private const val HappySoundGapMillis = 1_500L

/** Самый частый звук встряски игрушки. */
private const val ToySoundGapMillis = 140L

/** Паузы между вздохами грустного питомца. */
private val SighPauseMs = 4_000L..8_000L

/** Шкала настроения при нехватке радости покачивается с такой паузой. */
private const val BarWobblePauseMillis = 3_000L

/** Настроения, на которые питомец вздыхает. */
private val SadEmotions = setOf(Emotion.HUNGRY, Emotion.DIRTY, Emotion.TIRED, Emotion.SAD)

/**
 * Сон — перерыв в игре: ребёнок не сидит в приложении, пока питомец спит.
 * Открыты только верхний ряд (деньги, уровень, меню) и «Зал и сон»; всё
 * остальное полупрозрачное и не нажимается — кухня, ванная, плашки копилки
 * и игры уровня, «Завершить уровень» в панели уровня.
 */
private const val NightDim = 0.45f

/**
 * Почему питомцу так — ТЗ п. 2.5.10: краткое объяснение причины эмоции и что сделать.
 * Спокойное состояние не комментируем: всё в порядке, лишний текст ни к чему.
 */
private fun emotionReason(name: String, emotion: Emotion, hasToys: Boolean): String? = when (emotion) {
    Emotion.HUNGRY -> "$name голоден: сытость низкая. Покорми на кухне"
    Emotion.DIRTY -> "$name испачкался. Помой в ванной"
    Emotion.TIRED -> "$name устал и хочет спать. Уложи его в капсулу в зале"
    Emotion.SAD -> if (hasToys) {
        "$name грустит: мало радости. Поиграй с ним игрушкой — возьми её с пола"
    } else {
        "$name грустит: мало радости. Загляни в магазин за «хочется»"
    }
    Emotion.HAPPY -> "$name доволен: о нём хорошо заботятся"
    Emotion.CALM -> null
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onOpenBudget: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (taskId: String) -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onClosePeriod: () -> Unit,
    onBuy: (itemId: String) -> Unit,
    onSleep: () -> Unit = {},
    onWake: () -> Unit = {},
    onSleepCard: (correct: Boolean) -> Unit = {},
    onPlay: (toyId: String, shakes: Int) -> Unit = { _, _ -> },
) {
    // Питомец спит — это ночь: он в капсуле, свет выключен, игра на паузе
    // до утра. Глаза закрываются, когда он уже внутри, и открываются,
    // как только проснулся. Открыли приложение, а он спит, — глаза закрыты сразу.
    val sleep = state.sleep
    val sleeping = sleep != null
    var eyesClosed by remember { mutableStateOf(sleeping) }
    LaunchedEffect(sleeping) {
        if (!sleeping) {
            eyesClosed = false
        } else if (!eyesClosed) {
            delay((WALK_MS + DOOR_MS).toLong())
            eyesClosed = true
        }
    }

    // Часы сна: раз в секунду — кольцо сна растёт, минуты до пробуждения тают.
    val now by produceState(System.currentTimeMillis(), sleep) {
        while (sleep != null) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    // Срок вышел — просыпается сам, даже если экран открыт весь час.
    LaunchedEffect(sleep?.endsAt) {
        val ends = sleep?.endsAt ?: return@LaunchedEffect
        delay((ends - System.currentTimeMillis()).coerceAtLeast(0))
        onWake()
    }
    val energyNow = sleep?.energyAt(now) ?: state.stats.energy
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    // Во сне — сопение и приглушённая музыка. Дверь капсулы щёлкает, когда
    // питомец до неё дошёл; открыли приложение, а он уже спит, — без щелчка.
    val sounds = LocalSounds.current
    LoopWhile(Sfx.SleepLoop, active = eyesClosed)
    DisposableEffect(sleeping) {
        sounds.duckMusic(sleeping)
        onDispose { sounds.duckMusic(false) }
    }
    var wasSleeping by remember { mutableStateOf(sleeping) }
    LaunchedEffect(sleeping) {
        if (sleeping != wasSleeping) {
            if (sleeping) delay(WALK_MS.toLong())
            sounds.play(Sfx.CapsuleDoor)
            wasSleeping = sleeping
        }
    }

    // Питомцу плохо — вздыхает, когда ребёнок это видит: на входе и при смене настроения.
    LaunchedEffect(state.emotion, sleeping) {
        if (!sleeping && state.emotion in SadEmotions) sounds.play(Sfx.PetSad)
    }

    // Отладочная сборка — та, что ставится как ru.finney.pet.debug. Флаг читается
    // из манифеста, а не из BuildConfig: генерация BuildConfig в модуле выключена,
    // а включать её — правка в build.gradle.kts, не в этой зоне.
    val context = LocalContext.current
    val isDebuggable = remember(context) {
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    // Что стоит в комнате. Живёт на экране, а не в игре: это взгляд ребёнка на
    // комнату, а не событие в правилах.
    var spot by rememberSaveable { mutableStateOf(RoomSpot.LIVING) }

    // Какая панель ухода открыта и что в ней выбрано. Выбор живёт на экране,
    // а не в игре: пока не нажали «Купить», ничего не произошло.
    var care by rememberSaveable { mutableStateOf<CareTarget?>(null) }

    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    // Панель отладки: долгое нажатие на уровень, только в отладочной сборке.
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    // Отладка: облик питомца поверх уровня, чтобы посмотреть все стадии роста.
    var previewStage by rememberSaveable { mutableStateOf<Int?>(null) }

    // Панель уровня: чек-лист условий и «Завершить уровень». Открывается только
    // значком уровня, и она же — подтверждение: шаг необратимый, случайное
    // нажатие не должно подводить итоги за ребёнка.
    var levelOpen by rememberSaveable { mutableStateOf(false) }

    // Игра ухода: что выбрали в панели и теперь бросают в рот или трут о питомца.
    // Покупка — в конце игры, см. ui/room/CareGame.kt.
    var playing by rememberSaveable { mutableStateOf<String?>(null) }

    // Выключены — питомец не прыгает и не хихикает: цепочка шагов при нулевой
    // скорости мелькает по кадру на шаг, это хуже, чем стоять спокойно.
    val animations = LocalAnimations.current

    // Анимация живёт здесь, а не внутри комнаты: игры ухода открывают питомцу рот
    // и заставляют его хихикать, а сами лежат поверх комнаты.
    val animation = rememberPetAnimation()

    // Где питомец на экране — игры целятся в него и в его рот.
    var petLayout by remember { mutableStateOf(Rect.Zero) }

    // Игра с игрушкой: ребёнок водит игрушкой рядом с питомцем. Каждые ShakeTravel
    // пути пальца рядом с ним — одно потряхивание. Потряхивания копятся и уходят
    // в игру пачкой раз в PlayFlushMillis и когда игрушку отпустили.
    val hearts = rememberHeartBurst()
    val shakeTravelPx = with(LocalDensity.current) { ShakeTravel.toPx() }
    var travel by remember { mutableFloatStateOf(0f) }
    var pendingShakes by remember { mutableIntStateOf(0) }
    var heldToy by remember { mutableStateOf<String?>(null) }
    var lastHappySound by remember { mutableLongStateOf(0L) }
    val currentState by rememberUpdatedState(state)

    // Место PetView — с поправкой на стадию роста: облик меньше рамки и стоит у её низа.
    fun petBounds(): Rect = currentState.let { petLayout.grownPet(it.appearance.character.skin, previewStage ?: it.stage) }
    fun flushPlay() {
        val toy = heldToy ?: return
        if (pendingShakes > 0) onPlay(toy, pendingShakes)
        pendingShakes = 0
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(PlayFlushMillis)
            flushPlay()
        }
    }
    // Звук игрушки: «поп» — взял, своя встряска на ходу, «тук» — отпустил. Встряска
    // звучит где угодно, не только у питомца: игрушка живая сама по себе. Не чаще
    // раза в ToySoundGapMillis и с разбросом тона — серия не звучит пулемётом.
    var soundTravel by remember { mutableFloatStateOf(0f) }
    var lastToySound by remember { mutableLongStateOf(0L) }
    fun onToyDrag(toyId: String, centre: Offset, delta: Offset) {
        if (heldToy == null) sounds.play(Sfx.ToyPickup)
        heldToy = toyId
        soundTravel += delta.getDistance()
        if (soundTravel >= shakeTravelPx) {
            soundTravel = 0f
            val now = System.currentTimeMillis()
            if (now - lastToySound > ToySoundGapMillis) {
                lastToySound = now
                sounds.play(toyShakeSound(toyId), 0.9f + Random.nextFloat() * 0.25f)
            }
        }
        val pet = petBounds()
        if (pet == Rect.Zero) return
        // Тянется к игрушке, где бы она ни была: следит за ней.
        animation.lookAt((centre.x - pet.center.x) / pet.width)
        if (!pet.inflate(pet.width * 0.1f).contains(centre)) return
        travel += delta.getDistance()
        while (travel >= shakeTravelPx) {
            travel -= shakeTravelPx
            val s = currentState
            val gain = pendingShakes * s.moodPerShake
            val joyful = s.playLeft - gain > 0 && s.stats.mood + gain < 100
            pendingShakes++
            hearts.emit(Offset(pet.center.x, pet.top + pet.height * 0.2f), filled = joyful)
            if (joyful) {
                if (animations) animation.playGiggle()
                val now = System.currentTimeMillis()
                if (now - lastHappySound > HappySoundGapMillis) {
                    lastHappySound = now
                    sounds.play(Sfx.PetHappy)
                    if (animations) animation.playJoy()
                }
            }
        }
    }
    fun onToyDrop(toyId: String) {
        heldToy = toyId
        flushPlay()
        heldToy = null
        travel = 0f
        soundTravel = 0f
        sounds.play(Sfx.ToyDrop)
        animation.lookAt(0f)
    }

    // Мало радости — питомец время от времени вздыхает и оседает: грусть видна
    // и без слов. Шкала при этом покачивается, зовёт на неё нажать.
    LaunchedEffect(state.moodLow, sleeping, animations) {
        if (!state.moodLow || sleeping || !animations) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(SighPauseMs.first, SighPauseMs.last))
            animation.playSigh()
        }
    }

    // Сон-загадка: открытая карточка, выбранное слово и сколько карточек уже было за этот
    // сон. Новый сон — счёт заново: ключ — минута, когда уснул.
    var dreamCard by remember(sleep?.since) { mutableStateOf<SleepCard?>(null) }
    var dreamPicked by remember(sleep?.since) { mutableStateOf<String?>(null) }
    var dreamsAsked by rememberSaveable(sleep?.since) { mutableIntStateOf(0) }
    var lastDream by remember { mutableStateOf<String?>(null) }

    // Подсказка к шкале радости: что её поднимает. Открывается нажатием на шкалу и
    // висит, пока не закроют: сама она пропадала, пока ребёнок её читал.
    var moodHint by remember { mutableStateOf(false) }

    // Подсказка следующего шага: где стоят кнопки-цели и вспышка «уровень готов».
    val hintTargets = remember { HintTargets() }
    val burst = remember { Animatable(0f) }
    // Условия выполнены — дуга у значка закрылась. Вспышка — только на переходе:
    // что уже видели, помнит rememberSaveable, и вернувшись с игры уровня, ребёнок
    // видит вспышку, а при каждом входе на экран она не повторяется.
    val ready = state.check.planConfirmed && state.check.willPass && !sleeping
    var seenReady by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(ready) {
        val before = seenReady
        seenReady = ready
        if (before != false || !ready) return@LaunchedEffect
        sounds.play(Sfx.Correct)
        if (!animations) return@LaunchedEffect
        burst.snapTo(0f)
        burst.animateTo(1f, tween(durationMillis = 1_000))
        burst.snapTo(0f)
    }

    // Во время игры плашки и кнопки комнат гаснут: на их месте подсказка и
    // «Не сейчас». Гаснут, а не убираются — иначе шкала настроения, которая
    // тянется по свободной высоте, прыгала бы. Деньги остаются: ребёнок видит,
    // как монеты уходят, когда еда попала в рот.
    val hudAlpha by animateFloatAsState(if (playing == null) 1f else 0f, label = "hud")

    fun openCare(target: CareTarget) {
        care = target
        picked = null
    }

    // Кнопка внизу сначала показывает комнату, и только повторное нажатие
    // открывает выбор: по макету её дело — «появляется стол», «появляется ванна».
    // Ребёнок сперва видит, куда попал, и лишь потом тратит деньги.
    fun goTo(next: RoomSpot, target: CareTarget) {
        if (sleeping) return
        if (spot == next) openCare(target) else spot = next
    }

    // Спят только в зале. Вернулись в приложение, а комната запомнилась другая, —
    // питомец всё равно в капсуле, и на экране должна быть она.
    LaunchedEffect(sleeping) {
        if (sleeping) spot = RoomSpot.LIVING
    }


    // Касания по экрану — для подсказки шага: она гаснет от любого касания и
    // возвращается, только если ребёнок замешкался (NextStepHint.kt, hintDelayMillis).
    // Считаются одни нажатия, без движения пальца: каждое — одна рекомпозиция, не на кадр.
    var touches by remember { mutableIntStateOf(0) }

    // Игрушка показывает, что с ней делать (ToyShowcase.kt): рука — пока с игрушками ни разу
    // не играли, подскок — когда питомцу не радостно и ребёнок долго ничего не делает.
    // Рука уходит от любого касания; до следующего запуска она не вернётся.
    val showcase = remember { ToyShowcase() }
    val toyBounds = remember { mutableStateMapOf<String, Rect>() }
    var toyShown by rememberSaveable { mutableStateOf(false) }
    val roomCalm = spot == RoomSpot.LIVING && !sleeping && playing == null && care == null &&
        heldToy == null && !levelOpen && dreamCard == null
    val showToy = !state.toyPlayed && !toyShown && state.toys.isNotEmpty() && roomCalm
    LaunchedEffect(showToy, animations) {
        if (!showToy) return@LaunchedEffect
        try {
            delay(ToyShowDelayMillis)
            val id = currentState.toys.firstOrNull { it in toyBounds } ?: return@LaunchedEffect
            if (!animations) {
                showcase.point(id)
                awaitCancellation()
            }
            repeat(ToyShowRounds) { round ->
                val from = toyBounds[id] ?: return@LaunchedEffect
                val pet = petBounds()
                if (pet == Rect.Zero) return@LaunchedEffect
                // К тому боку питомца, с которого лежит игрушка: рука не пересекает его.
                val side = if (from.center.x >= pet.center.x) 1f else -1f
                val to = Offset(pet.center.x + side * pet.width * 0.3f, pet.center.y + pet.height * 0.1f)
                showcase.show(id, from, to) {
                    // Звук — только в первом круге: дальше показ идёт молча, не надоедает.
                    if (round == 0) sounds.play(toyShakeSound(id), 0.95f + Random.nextFloat() * 0.2f)
                    hearts.emit(Offset(pet.center.x, pet.top + pet.height * 0.2f), filled = true)
                }
                delay(600)
            }
            toyShown = true
        } finally {
            showcase.reset()
        }
    }
    LaunchedEffect(touches) {
        if (showcase.hand) toyShown = true
    }
    val hopToys = animations && roomCalm && !state.moodHappy && state.toys.isNotEmpty() && !showToy
    LaunchedEffect(touches, hopToys) {
        if (!hopToys) return@LaunchedEffect
        try {
            delay(ToyIdleMillis)
            repeat(ToyHopsPerIdle) { i ->
                val toys = currentState.toys.filter { it in toyBounds }
                if (toys.isEmpty()) return@LaunchedEffect
                val id = toys[i % toys.size]
                showcase.hop(id, toyBounds.getValue(id).height, sounds, toyShakeSound(id))
                delay(ToyHopGapMillis)
            }
        } finally {
            showcase.reset()
        }
    }

    // FinneyScreen тут не подходит: он заливает фон кремовым и сам растит колонку,
    // а под интерфейсом должна быть видна комната. Свой корень — ровно поэтому,
    // сам FinneyScreen не трогаем, на нём держатся пять других экранов.
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Первым проходом, до детей, и ничего не поглощая: кнопки, питомец и игрушки
            // получают касание как раньше, корень только замечает, что оно было.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) touches++
                    }
                }
            },
    ) {

        RoomScene(
            spot = spot,
            modifier = Modifier.fillMaxSize(),
            onTapItem = {
                when (spot) {
                    RoomSpot.KITCHEN -> openCare(CareTarget.FOOD)
                    RoomSpot.BATH -> openCare(CareTarget.BATH)
                    RoomSpot.LIVING -> if (sleeping) onWake() else onSleep()
                }
            },
            asleep = sleeping,
            // Сам гуляет по залу, пока с ним ничего не делают — и не играют игрушкой.
            wander = playing == null && care == null && heldToy == null,
            // Без анимаций прыжок мелькнул бы по кадру на шаг — не прыгаем вовсе.
            onHop = { if (animations) animation.playJoy() },
            toys = state.toys,
            toysEnabled = !sleeping && playing == null && care == null,
            onToyDrag = ::onToyDrag,
            onToyDrop = ::onToyDrop,
            toyNudge = showcase::offsetFor,
            onToyPlaced = { id, bounds -> if (toyBounds[id] != bounds) toyBounds[id] = bounds },
        ) {
            // Нажатие — питомец подпрыгивает: это игра, а не меню.
            //
            // Подсветку убираем: у питомца нет прямоугольной формы, и ripple
            // лёг бы квадратом вокруг.
            PetView(
                character = state.appearance.character,
                bodyColor = state.appearance.bodyColor,
                accessories = state.worn,
                stage = previewStage ?: state.stage,
                mood = if (eyesClosed) PetMood.SLEEP else state.emotion.toMood(),
                pose = rememberPoseProvider(animation),
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { petLayout = it.boundsInRoot() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Погладить питомца",
                        onClick = {
                            if (!sleeping) {
                                sounds.play(Sfx.PetHappy)
                                if (animations) animation.playJoy()
                            }
                        },
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

        // ---------- Верх: деньги-магазин, уровень, меню ----------
        // Три слота в ряд: слева деньги, по центру уровень, справа «бургер».
        // Кнопка с деньгами и есть вход в магазин — баланс показывает, сколько
        // можно потратить, и нажатие ведёт туда, где тратят. Отдельный кружок
        // магазина в нижнем ряду после этого не нужен.
        //
        // Крайние слоты одинаковой ширины (weight 1), поэтому уровень стоит
        // ровно по середине экрана, а не съезжает от длины суммы.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Верхний ряд во сне не гаснет: деньги, уровень и меню открыты
            // и ночью. Всё остальное на экране во сне закрыто (см. NightDim).
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                WalletButton(balance = state.balance, onClick = onOpenShop)
            }

            // Уровень — главный показатель роста, поэтому в полтора раза крупнее кнопок по краям.
            // Центры всех трёх на одной линии: ряд выравнивается по вертикали.
            Box(contentAlignment = Alignment.Center) {
                // Нажатие — что нужно для уровня. Дуга вокруг — сколько условий уже выполнено.
                LevelBadge(
                    level = state.level,
                    size = LevelBadgeSize,
                    progress = state.levelProgress,
                    modifier = Modifier.hintTarget(hintTargets, NextStep.PLAN, NextStep.FINISH).combinedClickable(
                        onClickLabel = "Что нужно для уровня",
                        onLongClickLabel = if (isDebuggable) "Отладка" else null,
                        onLongClick = if (isDebuggable) ({ debugOpen = true }) else null,
                        onClick = { levelOpen = true },
                    ),
                )
                // Тестовый профиль видно сразу: эксперт знает, почему все игры открыты.
                // Метка лежит на нижнем краю значка, а не под ним — ряд не вырастает.
                if (state.isDemo) {
                    Text(
                        text = "демо",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinneyInk,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(FinneySand)
                            .border(StrokeThin, FinneyInk, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Отдельной кнопки «?» нет: подсказка — первый пункт меню («Как играть»).
                // Она по-прежнему доступна в любой момент (ТЗ п. 2.5.1), а верхний ряд
                // остаётся симметричным: монета — уровень — меню.
                // «Бургер» — всё, что не про уход за питомцем и не на экране:
                // игры, план, гардероб, подсказка и раздел взрослого.
                // Низ экрана из-за этого остался про комнаты, а не про меню.
                Box {
                    FinneyIconButton(
                        onClick = { menuOpen = true },
                        contentDescription = "Меню",
                        size = 56.dp,
                    ) {
                        FinneyIcon(FinneyIcons.Menu, size = 26.dp)
                    }

                    HomeMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        onOpenHelp = onOpenHelp,
                        onOpenBudget = onOpenBudget,
                        onOpenWardrobe = onOpenWardrobe,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        }

        // Копилка и мини-игры — то, чего макет на главном не предусмотрел,
        // а ТЗ п. 2.5.3 требует видеть сразу. Обе плашки умещаются в одну строку
        // и лежат в кремовой полосе над комнатой: строкой ниже начинается абажур
        // лампы, и второй ряд его бы срезал. Сытость, чистота и настроение сюда
        // не попадают — они переехали в кольца кнопок и в шкалу слева.
        Row(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = hudAlpha },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // У цели копилка — кольцом вокруг значка: «10 из 50» видно и не читая.
            InfoChip(
                icon = FinneyIcons.Piggy,
                text = state.goal
                    ?.let { "${it.goal.label}: ${it.saved} из ${it.goal.price}" }
                    ?: "Копилка: ${state.totalSavings}",
                action = "Копилка и цель",
                onClick = onOpenGoals,
                enabled = !sleeping,
                progress = state.goal?.let { it.saved to it.goal.price },
                modifier = Modifier.weight(1f).hintTarget(hintTargets, NextStep.SAVE),
            )

            // Игра уровня — обязательное условие, поэтому вход в неё всегда на виду.
            // Список всех игр ребёнку не нужен: на каждом уровне своя (он остался в отладке).
            val levelTaskId = state.check.levelTaskId
            if (levelTaskId != null && state.levelGame != null) {
                // Пройденная — не тонкой галочкой в конце строки (плейтест 28.09, п. 46),
                // а крупным «✓» вместо звезды, зелёной заливкой и словом «пройдено».
                // Зелёный здесь — итог действия ребёнка, и рядом значок с подписью.
                val passed = state.check.gamePassed
                InfoChip(
                    icon = FinneyIcons.Star,
                    text = if (passed) "${state.levelGame}\nпройдено" else "Игра: ${state.levelGame}",
                    action = "Игра уровня",
                    onClick = { onOpenTask(levelTaskId) },
                    enabled = !sleeping,
                    done = passed,
                    modifier = Modifier.weight(1f).hintTarget(hintTargets, NextStep.LEVEL_GAME),
                )
            }
        }

        emotionReason(state.petName, state.emotion, hasToys = state.toys.isNotEmpty())?.takeIf { !sleeping }?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = hudAlpha }
                    .clip(RoundedCornerShape(16.dp))
                    .background(FinneyCream.copy(alpha = 0.9f))
                    .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // ---------- Середина: сама комната ----------
        // Дырка в колонке: сквозь неё видно питомца и обстановку, которые рисует
        // RoomScene под интерфейсом. Нажатия проходят насквозь — пустой Box
        // их не ловит.
        //
        // Единственное, что здесь лежит, — шкала настроения у левого края:
        // в макете на этом месте подписано «тут только шкала настроения»,
        // и нарисована она там узкой и длинной.
        //
        // Высота — доля свободного места, а не число: на 360 dp и на 412 dp
        // середина экрана разная, и шкала должна тянуться вместе с ней. К низу
        // не опускается: на 0.55 кружок-лицо заканчивается выше борта ванны.
        //
        // К краю шкала прижата сдвигом, а не своими отступами колонки: поля
        // экрана в 16 dp нужны кнопкам и плашкам, а шкале они только мешали —
        // она отъезжала к питомцу.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopStart,
        ) {
            val barWidth = 36.dp
            val barHeight = maxHeight * 0.55f - barWidth
            // Мало радости — шкала покачивается: на неё стоит нажать.
            val wobble = remember { Animatable(0f) }
            // Без анимаций покачивание было бы дрожью в пять кадров — шкала стоит, и зовёт
            // на неё лицо-кружок внизу.
            LaunchedEffect(state.moodLow, sleeping, animations) {
                if (!state.moodLow || sleeping || !animations) {
                    wobble.snapTo(0f)
                    return@LaunchedEffect
                }
                while (true) {
                    delay(BarWobblePauseMillis)
                    for (target in floatArrayOf(1f, -1f, 0.6f, -0.4f, 0f)) wobble.animateTo(target, tween(90))
                }
            }
            HappinessBar(
                value = state.stats.mood,
                // Кружок-лицо выступает под капсулой на ширину шкалы — вычитаем.
                height = barHeight,
                width = barWidth,
                modifier = Modifier
                    .offset(x = -HappinessEdgeShift)
                    .graphicsLayer {
                        rotationZ = wobble.value * 4f
                        alpha = if (sleeping) NightDim else hudAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Что поднимает настроение",
                        onClick = { if (!sleeping) moodHint = !moodHint },
                    ),
            )
            if (moodHint) {
                // Касание мимо подсказки её закрывает. Слой — только на средней части
                // экрана и только пока подсказка открыта; сама шкала лежит под ним
                // и закрывает её так же.
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Закрыть подсказку",
                            onClick = { moodHint = false },
                        ),
                )
                MoodHint(
                    onClose = { moodHint = false },
                    // Отступом, а не сдвигом: сдвинутая плашка не знает, что справа кончился экран.
                    modifier = Modifier.padding(start = barWidth + 4.dp, top = barHeight * 0.35f),
                )
            }

            // Во сне на полу — сколько ещё спать и «Разбудить». Днём здесь пусто:
            // «Завершить уровень» живёт только в панели уровня (значок сверху),
            // чтобы ребёнок подводил итог, видя условия, а не мимоходом.
            if (sleep != null) {
                SleepPanel(
                    petName = state.petName,
                    minutesLeft = ((sleep.endsAt - now + 59_999) / 60_000).toInt(),
                    secondsLeft = ((sleep.endsAt - now + 999) / 1_000).toInt(),
                    enoughIn = (sleep.enoughAt - now).coerceAtLeast(0),
                    onWake = onWake,
                    wakeModifier = Modifier.hintTarget(hintTargets, NextStep.WAKE),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // ---------- Низ: комнаты ----------
        // Три кнопки из макета. Каждая переключает комнату, а кольцо вокруг
        // показывает потребность, которую в этой комнате закрывают, — так они
        // нарисованы в ките («опускание/поднятие шкал потребностей»). В зале
        // закрывают сон.
        Row(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = hudAlpha },
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FinneyNeedButton(
                icon = FinneyIcons.Lamp,
                label = "Зал и сон",
                // Как у кухни и ванной: первое нажатие — комната, второе — уложить
                // спать. Гардероб — в меню.
                onClick = {
                    when {
                        sleeping -> onWake()
                        spot == RoomSpot.LIVING -> onSleep()
                        else -> spot = RoomSpot.LIVING
                    }
                },
                value = energyNow,
                selected = spot == RoomSpot.LIVING,
                modifier = Modifier.hintTarget(hintTargets, NextStep.SLEEP),
            )
            // Спящего не кормят и не моют: кнопки полупрозрачные и не нажимаются.
            FinneyNeedButton(
                icon = FinneyIcons.Food,
                label = "Покормить",
                onClick = { goTo(RoomSpot.KITCHEN, CareTarget.FOOD) },
                value = state.stats.satiety,
                selected = spot == RoomSpot.KITCHEN,
                enabled = !sleeping,
                modifier = Modifier.hintTarget(hintTargets, NextStep.FEED).graphicsLayer { alpha = if (sleeping) NightDim else 1f },
            )
            FinneyNeedButton(
                icon = FinneyIcons.Bath,
                label = "Помыть",
                onClick = { goTo(RoomSpot.BATH, CareTarget.BATH) },
                value = state.stats.hygiene,
                selected = spot == RoomSpot.BATH,
                enabled = !sleeping,
                modifier = Modifier.hintTarget(hintTargets, NextStep.WASH).graphicsLayer { alpha = if (sleeping) NightDim else 1f },
            )
        }

        }

        // Облачко-мысль над спящим: сон-загадка по справочнику (SleepCards.kt).
        // Ставится от головы питомца вправо-вверх и не выходит за край экрана.
        if (eyesClosed && sleeping && petLayout != Rect.Zero && dreamCard == null && !levelOpen) {
            DreamCloud(
                done = dreamsAsked >= state.sleepCardsPerSleep,
                animate = animations,
                onClick = {
                    sounds.play(Sfx.Tap)
                    dreamCard = sleepCard(state.glossary, Random, avoid = lastDream)
                    dreamPicked = null
                },
                modifier = Modifier.layout { measurable, constraints ->
                    val pet = petBounds()
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val margin = 8.dp.roundToPx()
                    val x = (pet.center.x + pet.width * 0.08f).toInt()
                        .coerceIn(margin, (constraints.maxWidth - placeable.width - margin).coerceAtLeast(margin))
                    val y = (pet.top + pet.height * 0.18f - placeable.height).toInt().coerceAtLeast(margin)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, y) }
                },
            )
        }

        // Одна подсказка за раз и только когда ничего не открыто поверх: во время
        // игры ухода и в панелях палец занят другим. Первый уровень — обучение:
        // экран темнеет вокруг цели, и рядом пузырь с фразой.
        //
        // Спящего будить подсказываем здесь, а не в ViewModel: сон идёт по часам,
        // а состояние игры за это время не меняется. Минуту, когда сна хватает,
        // считает ViewModel (SleepInfo.enoughAt), экран только сверяет часы.
        val step = if (sleep != null) NextStep.WAKE.takeIf { now >= sleep.enoughAt } else state.nextStep
        val hint = step.takeIf { playing == null && care == null && !levelOpen && !debugOpen }
        val tutorial = state.level == 1
        // Подсказка проявляется, когда ребёнок замешкался, и гаснет от касания. Какой
        // был последний шаг, помним отдельно: на угасании кольцо должно остаться на
        // цели, а не пропасть кадром, когда шаг сменился на null.
        var shownStep by remember { mutableStateOf<NextStep?>(null) }
        if (hint != null) shownStep = hint
        val wait = hint?.let { hintDelayMillis(it, state.level) }
        var idle by remember { mutableStateOf(false) }
        LaunchedEffect(touches, hint, wait) {
            idle = false
            if (wait == null) return@LaunchedEffect
            delay(wait)
            idle = true
        }
        val visible = hint != null && (wait == 0L || idle)
        val visibility by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = if (animations) tween(durationMillis = 300) else snap(),
            label = "hintVisibility",
        )
        NextStepOverlay(
            // Пока рука показывает игрушку, подсказка шага молчит: двух рук сразу не бывает.
            step = if (visibility > 0f && !showcase.hand) shownStep else null,
            targets = hintTargets,
            bubble = shownStep?.takeIf { tutorial }?.bubble(),
            spotlight = tutorial,
            animate = animations,
            burst = { burst.value },
            hand = hintWithHand(state.level),
            visibility = { visibility },
        )

        ToyShowcaseLayer(showcase, toyBounds, caption = "Возьми игрушку и потряси — ${state.petName} обрадуется")

        HeartBurstLayer(hearts)

        // Панель ухода поверх всего: пока выбирают, комната остаётся видна,
        // и понятно, к чему относится выбор.
        //
        // Под панелью — полупрозрачная подложка. Она и приглушает комнату,
        // чтобы заголовок панели не наезжал на плашки, и ловит нажатия: без неё
        // сквозь панель нажимались кнопки комнат, а нажатие мимо панели ничего
        // не делало. Теперь мимо — это «закрыть».
        care?.let { target ->
            val previews = when (target) {
                CareTarget.FOOD -> state.food
                CareTarget.BATH -> state.care
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FinneyInk.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Закрыть",
                        onClick = { care = null; picked = null },
                    )
                    .systemBarsPadding()
                    .padding(16.dp),
                // Панель прижата к низу, а не по центру: её заголовок висит
                // над рамкой и по центру попадал ровно на плашки копилки
                // и задания. Заодно сверху остаётся видна комната — понятно,
                // кого кормят.
                contentAlignment = Alignment.BottomCenter,
            ) {
                CarePanel(
                    // Нажатия по самой панели гасятся здесь: иначе попадание
                    // между карточками уходило бы на подложку и закрывало панель.
                    // Не clickable: это не кнопка, и TalkBack не должен звать её
                    // нажимаемой.
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
                    title = target.title,
                    // До подтверждения плана домен покупку не пропустит
                    // (Rejection.PlanNotConfirmed). Раньше кнопка «Купить»
                    // при этом молча закрывала панель и ничего не делала.
                    block = if (state.phase == PeriodPhase.ACTIVE) {
                        null
                    } else {
                        CareBlock(
                            reason = "Сначала составь план — потом покупки",
                            actionLabel = "К плану расходов",
                            onAction = onOpenBudget,
                        )
                    },
                    options = previews.map {
                        CareOption(it.item, it, isSelected = it.item.id == picked)
                    },
                    onPick = { picked = it },
                    confirmLabel = when (target) {
                        CareTarget.FOOD -> "Купить и покормить"
                        CareTarget.BATH -> "Купить и помыть"
                    },
                    onConfirm = { itemId ->
                        playing = itemId
                        care = null
                        picked = null
                    },
                    onDismiss = { care = null; picked = null },
                    // Окно покупки — тот же магазин в миниатюре (плейтест 28.09: еду покупали
                    // двумя путями, и они выглядели как два разных магазина): те же плитки,
                    // та же полоса «нужное» с остатком плана и кошелёк — комната под окном
                    // приглушена, и сумма в углу главного читается плохо.
                    tiles = true,
                    header = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "У тебя ${state.balance} финок" },
                        ) {
                            Text("У тебя", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                            CoinAmount(amount = state.balance)
                        }
                        CategoryBanner(Category.NEEDS, "Без этого Финни плохо", state.needsLeft)
                    },
                )
            }
        }

        // Карточка сна поверх всего, как панель ухода: подложка приглушает комнату
        // и закрывает карточку касанием мимо.
        val card = dreamCard
        if (card != null && sleeping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FinneyInk.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Закрыть",
                        onClick = { dreamCard = null },
                    )
                    .systemBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SleepCardPanel(
                    petName = state.petName,
                    card = card,
                    picked = dreamPicked,
                    cardsLeft = state.sleepCardsPerSleep - dreamsAsked,
                    sleepGain = state.sleepPerCard,
                    onPick = { word ->
                        dreamPicked = word
                        dreamsAsked++
                        lastDream = card.termId
                        onSleepCard(word == card.answer)
                        if (word == card.answer) {
                            sounds.play(Sfx.Correct)
                            // Верно — над спящим всплывают сердечки, как когда его гладят.
                            val pet = petBounds()
                            val head = Offset(pet.center.x, pet.top + pet.height * 0.2f)
                            repeat(3) { i -> hearts.emit(head + Offset((i - 1) * 40f, 0f), filled = true) }
                        }
                    },
                    onNext = {
                        dreamCard = sleepCard(state.glossary, Random, avoid = lastDream)
                        dreamPicked = null
                    },
                    onClose = { dreamCard = null },
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }

        playing?.let { itemId ->
            val mouth = state.appearance.character.skin.mouthCenter
            val pet = petBounds()
            when (spot) {
                RoomSpot.KITCHEN -> FeedingGame(
                    itemId = itemId,
                    pet = pet,
                    mouth = Offset(
                        pet.left + pet.width * mouth.pivotFractionX,
                        pet.top + pet.height * mouth.pivotFractionY,
                    ),
                    onMouthOpen = animation::openMouth,
                    onEaten = {
                        onBuy(itemId)
                        if (animations) animation.playEat()
                        playing = null
                    },
                    onCancel = { playing = null },
                )
                RoomSpot.BATH -> WashingGame(
                    itemId = itemId,
                    pet = pet,
                    onScrub = { if (animations) animation.playGiggle() },
                    onClean = {
                        onBuy(itemId)
                        if (animations) animation.playJoy()
                        playing = null
                    },
                    onCancel = { playing = null },
                )
                // Игры идут поверх кнопок комнат, из кухни или ванной не уйти.
                RoomSpot.LIVING -> Unit
            }
        }

        if (levelOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FinneyInk.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Закрыть",
                        onClick = { levelOpen = false },
                    )
                    .systemBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                LevelPanel(
                    level = state.level,
                    check = state.check,
                    levelGame = state.levelGame,
                    asleep = sleeping,
                    onFinish = {
                        levelOpen = false
                        onClosePeriod()
                    },
                    onOpenBudget = { levelOpen = false; onOpenBudget() },
                    onOpenProgress = { levelOpen = false; onOpenProgress() },
                    onDismiss = { levelOpen = false },
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }

        if (debugOpen && isDebuggable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FinneyInk.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { debugOpen = false },
                    )
                    .systemBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                DebugPanel(
                    onDismiss = { debugOpen = false },
                    onOpenTasks = { debugOpen = false; onOpenTasks() },
                    previewStage = previewStage,
                    onPreviewStage = { previewStage = it },
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }
    }
}


/**
 * Ночь: сколько ещё спать и «Разбудить». Лежит на полу под капсулой, над
 * кнопками комнат — там, куда смотрит ребёнок, когда уложил питомца.
 */
@Composable
private fun SleepPanel(
    petName: String,
    minutesLeft: Int,
    secondsLeft: Int,
    enoughIn: Long,
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    wakeModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FinneyCream.copy(alpha = 0.9f))
            .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            // Сокращения вместо слов: «минут/минуты/минуту» без склонений.
            text = if (secondsLeft < 60) "$petName спит · ещё $secondsLeft с" else "$petName спит · ещё $minutesLeft мин",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
        )
        // Ждать полного сна не обязательно: для уровня хватает порога, и панель говорит,
        // когда он будет. Иначе ребёнок сидел перед капсулой весь час (плейтест).
        Text(
            text = if (enoughIn <= 0) {
                "Сна уже хватает для уровня — можно будить"
            } else if (enoughIn < 60_000) {
                "Для уровня хватит через ${(enoughIn + 999) / 1_000} с"
            } else {
                "Для уровня хватит через ${(enoughIn + 59_999) / 60_000} мин"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )
        FinneyButton(text = "Разбудить", onClick = onWake, fillWidth = false, modifier = wakeModifier)
    }
}

/**
 * Панель уровня: что нужно, чтобы его пройти, и «Завершить уровень».
 *
 * Открывается только нажатием на значок уровня, поэтому ребёнок всегда видит
 * условия до того, как подвести итог. До подтверждения плана вместо
 * «Завершить уровень» здесь «Составить план» — следующий шаг, а не отказ.
 * Вернуть уровень нельзя, и панель прямо говорит, что будет: пройден или
 * начнётся заново с новыми деньгами. Прогресс при этом не падает никогда.
 *
 * Вид — по макету экрана условий уровня (28.09): номер уровня плашкой-«стадионом»
 * над панелью, игра уровня строкой-плашкой, три условия плитками с отметкой под
 * каждой, второстепенные кнопки — спокойные зелёные.
 */
@Composable
private fun LevelPanel(
    level: Int,
    check: LevelCheck,
    levelGame: String?,
    asleep: Boolean,
    onFinish: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenProgress: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LevelTitle(level)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusPanel))
                .background(FinneyCream)
                .border(StrokeBold, FinneyInk, RoundedCornerShape(RadiusPanel))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!check.planConfirmed) {
                Text(
                    "Уровень начинается с плана: реши, сколько на нужное, сколько на «хочется» и сколько отложить.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
                FinneyButton(text = "Составить план", onClick = onOpenBudget)
            } else {
                if (levelGame != null && check.levelTaskId != null) {
                    LevelGameRow(levelGame, check.gamePassed, required = check.gameRequired)
                }
                // Обязательна ли игра — здесь, а не в её плашке: там крупному
                // названию и так тесно.
                val withGame = levelGame != null && check.levelTaskId != null && check.gameRequired
                Text(
                    if (withGame) {
                        "Пройди игру и выполни ${check.toPass} из $LEVEL_CONDITIONS:"
                    } else {
                        "Выполни ${check.toPass} из $LEVEL_CONDITIONS:"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = FinneyInk,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ConditionTile(
                        label = "Уход",
                        spoken = "Сыт, чист и выспался",
                        done = check.needsCovered,
                        modifier = Modifier.weight(1f),
                    ) { LevelIcon(R.drawable.ic_level_care) }
                    // «По плану» проверяют по числам на другом экране: плитка ведёт
                    // туда, в «План и факт». Иначе этот экран ребёнок видел один
                    // раз — сразу после подтверждения плана.
                    ConditionTile(
                        label = "План",
                        spoken = "Траты по плану",
                        done = check.planMatched,
                        onClick = onOpenBudget,
                        modifier = Modifier.weight(1f),
                    ) { Coin(size = 56.dp) }
                    ConditionTile(
                        label = "Копилка",
                        spoken = "Отложено в копилку",
                        done = check.savingsAdded,
                        modifier = Modifier.weight(1f),
                    ) { LevelIcon(R.drawable.ic_level_piggy) }
                }
                Text(
                    if (check.willPass) {
                        "Готово! Уровень будет пройден, и придут новые деньги."
                    } else {
                        "Если завершить сейчас, уровень начнётся заново — с новыми деньгами."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                    textAlign = TextAlign.Center,
                )
                // Во сне уровень не завершают: условия видно, а итог — утром.
                if (asleep) {
                    Text(
                        "Питомец спит. Завершить уровень можно, когда он проснётся.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FinneyInk,
                        textAlign = TextAlign.Center,
                    )
                }
                FinneyButton(
                    text = "Завершить уровень",
                    onClick = onFinish,
                    enabled = !asleep,
                    textStyle = LevelMainButtonText,
                    modifier = Modifier.graphicsLayer { alpha = if (asleep) NightDim else 1f },
                )
            }
            // Уже основной: рядом с главным действием второстепенные не должны
            // спорить с ним ни цветом, ни шириной.
            FinneyButton(
                text = "Итоги и история",
                onClick = onOpenProgress,
                fillWidth = false,
                calm = true,
                textStyle = LevelSideButtonText,
                modifier = Modifier.fillMaxWidth(SideButtonWidth),
            )
            // «Ещё поиграю» — просто закрыть панель, звать к нему не нужно:
            // спокойная кнопка, как «Назад» в знакомстве.
            FinneyQuietButton(
                text = "Ещё поиграю",
                onClick = onDismiss,
                sound = Sfx.Back,
                modifier = Modifier.fillMaxWidth(SideButtonWidth),
            )
        }
    }
}

/** Ширина второстепенных кнопок панели уровня — доля ширины панели, как в макете. */
private const val SideButtonWidth = 0.82f

// Кегли панели уровня. В макете надписи здесь заметно крупнее, чем на обычных
// кнопках: это главный выбор уровня, и его должно быть видно издалека.
// lineHeight задан вместе с кеглем: у Glina высокие круглые буквы, и без запаса
// строки им срезает верх (см. Type.kt).

/** «Уровень N» над панелью. */
private val LevelTitleText: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 52.sp)

/** «Завершить уровень». */
private val LevelMainButtonText: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 40.sp)

/** «Итоги и история», «Ещё поиграю». */
private val LevelSideButtonText: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontSize = 27.sp, lineHeight = 36.sp)

/** Название игры уровня и подписи плиток. */
private val LevelItemText: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontSize = 23.sp, lineHeight = 31.sp)

/** Номер уровня плашкой-«стадионом» над панелью: та же заливка, что у кнопки, но не нажимается. */
@Composable
private fun LevelTitle(level: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(percent = 50))
            .buttonFill(pressed = false, round = false)
            .semantics { heading() }
            .padding(horizontal = 36.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedText("Уровень $level", style = LevelTitleText)
    }
}

/**
 * Игра уровня строкой-плашкой: геймпад, название и отметка. Отметка обязательна —
 * без неё не видно, пройдена ли игра (ТЗ п. 3.6), хотя в макете её нет.
 */
@Composable
private fun LevelGameRow(game: String, passed: Boolean, required: Boolean) {
    val note = if (required) "обязательно" else "по желанию"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(percent = 50))
            .buttonFill(pressed = false, round = false, glare = false)
            .clearAndSetSemantics {
                contentDescription = "Игра «$game», $note: ${if (passed) "пройдена" else "пока нет"}"
            }
            .padding(start = 20.dp, end = 14.dp, top = 6.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_level_game),
            contentDescription = null,
            modifier = Modifier.size(width = 47.dp, height = 40.dp),
        )
        // Без слова «Игра»: о нём говорит геймпад, а с ним название крупным кеглем
        // переносилось и вылезало на нижнюю полосу плашки.
        Text(
            "«$game»",
            // Длинное название («Лимонадная лавка») встаёт в две строки: интервал
            // плотнее, чтобы вторая строка не заходила на нижнюю полосу плашки.
            style = LevelItemText.copy(lineHeight = 26.sp),
            color = FinneyInk,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        CheckSquare(passed)
    }
}

/**
 * Условие уровня плиткой: крупный значок, подпись и отметка под ней.
 *
 * Выполненное — зелёная плитка, но главный признак — галочка в квадрате
 * под ней: по ТЗ п. 3.6 цвет не может быть единственным. Подпись — наше
 * добавление к макету: без неё смайлик и монету ребёнок не свяжет с условием.
 */
@Composable
private fun ConditionTile(
    label: String,
    spoken: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    val tap = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = "Посмотреть", onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusCard))
            .then(tap)
            .clearAndSetSemantics { contentDescription = "$spoken: ${if (done) "да" else "пока нет"}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(RadiusCard))
                .background(if (done) FinneyGreen else FinneyCream)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Text(label, style = LevelItemText, color = FinneyInk, maxLines = 1)
        CheckSquare(done)
    }
}

/** Отметка условия: квадрат с галочкой или пустой — отличаются формой, а не цветом. */
@Composable
private fun CheckSquare(done: Boolean) {
    val shape = RoundedCornerShape(RadiusCheckbox)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(if (done) FinneyYellow else FinneyCream)
            .border(StrokeRegular, FinneyInk, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Canvas(Modifier.size(24.dp)) {
                val w = size.minDimension
                drawPath(
                    Path().apply {
                        moveTo(w * 0.08f, w * 0.52f)
                        lineTo(w * 0.38f, w * 0.82f)
                        lineTo(w * 0.92f, w * 0.20f)
                    },
                    color = FinneyInk,
                    style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/**
 * Значок условия в плитке — рисунок из design/exports/ui/icons (tools/render_icons.py).
 * Подпись для TalkBack не нужна: плитка читается целиком своей фразой.
 */
@Composable
private fun LevelIcon(@DrawableRes id: Int) {
    Image(
        painterResource(id),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(60.dp),
    )
}

/**
 * Меню «бургера»: всё, что не про уход за питомцем и чего нет на экране.
 *
 * Порядок — от игры к служебному: планировать, наряжать, потом подсказка
 * и настройки (звук, музыка, раздел взрослого). Прогресс и итоги — в панели уровня
 * (нажатие на значок), игра уровня — в плашке над комнатой: дублей в меню нет.
 */
@Composable
private fun HomeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(FinneyCream),
    ) {
        // Каждый пункт сначала закрывает меню: иначе после возврата с экрана
        // оно осталось бы раскрытым поверх главного.
        HomeMenuItem("План расходов") { onDismiss(); onOpenBudget() }
        HomeMenuItem("Гардероб") { onDismiss(); onOpenWardrobe() }
        // Знакомство в режиме подсказки: в конце «Понятно» и назад, без «Создать питомца».
        HomeMenuItem("Как играть") { onDismiss(); onOpenHelp() }
        HomeMenuItem("Настройки") { onDismiss(); onOpenSettings() }
    }
}

/** Пункт меню «бургера». */
@Composable
private fun HomeMenuItem(text: String, onClick: () -> Unit) {
    val sounds = LocalSounds.current
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk) },
        onClick = {
            sounds.play(Sfx.Tap)
            onClick()
        },
    )
}

/**
 * Уровень в полтора раза крупнее кнопок по краям верхнего ряда — вместе с кольцом
 * прогресса. Кольцо рисуется снаружи круга и добавляет 26 % диаметра (дорожка и обводка
 * с двух сторон), поэтому круг 66 dp даёт значок 84 dp = 1,5 × 56. Вдвое крупнее
 * (112 dp) уровень перетягивал на себя весь ряд.
 */
private val LevelBadgeSize = 66.dp

/**
 * Плашка-кнопка с фактом: копилка, активное задание.
 *
 * Значок слева обязателен: по ТЗ п. 3.6 одним цветом ничего не сообщают,
 * и плашки должны отличаться друг от друга не только словами. Высота — от
 * 48 dp, потому что по плашке нажимают.
 *
 * [action] — что произойдёт при нажатии; уходит в TalkBack вместо [text],
 * который сам по себе действия не называет.
 *
 * [enabled] = false — плашка полупрозрачная и не нажимается (во сне).
 *
 * [progress] — «сколько из скольки» (накоплено и цена цели): значок встаёт
 * в кольцо, которое заполняется от розового к зелёному. null — просто значок.
 */
@Composable
private fun InfoChip(
    icon: FinneyIcons,
    text: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    progress: Pair<Int, Int>? = null,
    done: Boolean = false,
) {
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else NightDim }
            .clip(RoundedCornerShape(RadiusField))
            .background(if (done) FinneyGreen else FinneySand)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(RadiusField))
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (done) {
            // Тот же размер, что кольцо копилки рядом: полоса над комнатой не растёт.
            CheckBadge(size = 32.dp)
        } else if (progress != null) {
            // 32 dp — ровно высота плашки без полей: полоса над комнатой не растёт.
            FillRing(progress.first, progress.second, diameter = 32.dp) { FinneyIcon(icon, size = 17.dp) }
        } else {
            FinneyIcon(icon, size = 22.dp)
        }
        if (done) {
            // «Пройдена!» — первой строкой и целиком: длинное название иначе съедало
            // обе строки, и слово обрезалось. Название — ниже, сколько влезет.
            Column {
                Text("Пройдена!", style = MaterialTheme.typography.bodyMedium, color = FinneyInk, maxLines = 1)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = FinneyInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Эмоция домена → выражение лица питомца. Отдельная функция, потому что
 * состояний лица четыре, а эмоций больше: грусть и голод выглядят одинаково.
 */
private fun Emotion.toMood(): PetMood = when (this) {
    Emotion.HAPPY, Emotion.CALM -> PetMood.HAPPY
    // Голод и усталость отдельного лица не имеют: такой питомец выглядит грустным.
    // Лицо сна — закрытые глаза — только для настоящего сна в капсуле.
    Emotion.HUNGRY, Emotion.TIRED, Emotion.SAD -> PetMood.SAD
    Emotion.DIRTY -> PetMood.DIRTY
}

/** Все шаги подсказки — по превью на каждый. */
private class NextStepProvider : PreviewParameterProvider<NextStep> {
    override val values = NextStep.entries.asSequence()
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD, widthDp = 360, heightDp = 780)
@Composable
private fun HomeNextStepPreview(@PreviewParameter(NextStepProvider::class) step: NextStep) {
    HomePreviewContent(step)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun HomeContentPreview() {
    HomePreviewContent(step = null)
}

@Composable
private fun HomePreviewContent(step: NextStep?) {
    FinneyTheme {
        HomeContent(
            state = HomeUiState.Ready(
                petName = "Финни",
                appearance = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND),
                isDemo = false,
                stats = PetStats(30, 40, 50, 20),
                emotion = Emotion.CALM,
                level = 1,
                check = LevelCheck(
                    planConfirmed = true,
                    needsCovered = true,
                    planMatched = false,
                    savingsAdded = false,
                    toPass = 2,
                    levelTaskId = "change_1",
                    gamePassed = false,
                ),
                stage = 1,
                balance = 50,
                totalSavings = 0,
                goal = null,
                periodNumber = 1,
                phase = PeriodPhase.PLANNING,
                needsHint = 40,
                levelGame = "Касса Финни",
                food = emptyList(),
                care = emptyList(),
                nextStep = step,
            ),
            onOpenBudget = {}, onOpenShop = {}, onOpenGoals = {}, onOpenTasks = {}, onOpenTask = {}, onOpenWardrobe = {},
            onOpenProgress = {}, onOpenSettings = {}, onOpenHelp = {},
            onClosePeriod = {}, onBuy = {},
        )
    }
}
