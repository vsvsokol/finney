package ru.finney.pet.ui.home

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
import ru.finney.pet.ui.components.FeedbackDialog
import ru.finney.pet.ui.components.ActionFeedbackCard
import ru.finney.pet.ui.components.ActionFeedback
import ru.finney.pet.domain.game.Rejection
import kotlinx.coroutines.delay
import ru.finney.pet.domain.game.LevelCheck
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FeedbackSound
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.FinneyNeedButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.HappinessBar
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
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
}

private const val PurchaseFeedbackMillis = 5_000L

/** Сколько пути пальца с игрушкой рядом с питомцем — одно потряхивание. */
private val ShakeTravel = 40.dp

/** Раз в сколько потряхивания уходят в игру, пока игрушку не отпустили. */
private const val PlayFlushMillis = 800L

/** Не чаще раза в столько питомец смеётся вслух и подпрыгивает от игры. */
private const val HappySoundGapMillis = 1_500L

/** Паузы между вздохами грустного питомца. */
private val SighPauseMs = 4_000L..8_000L

/** Шкала настроения при нехватке радости покачивается с такой паузой. */
private const val BarWobblePauseMillis = 3_000L

/** Подсказка к шкале настроения закрывается сама через столько. */
private const val MoodHintMillis = 5_000L

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

    // Панель уровня: чек-лист условий и «Завершить уровень». Открывается только
    // значком уровня, и она же — подтверждение: шаг необратимый, случайное
    // нажатие не должно подводить итоги за ребёнка.
    var levelOpen by rememberSaveable { mutableStateOf(false) }

    // Игра ухода: что выбрали в панели и теперь бросают в рот или трут о питомца.
    // Покупка — в конце игры, см. ui/room/CareGame.kt.
    var playing by rememberSaveable { mutableStateOf<String?>(null) }

    // Анимация живёт здесь, а не внутри комнаты: игры ухода открывают питомцу рот
    // и заставляют его хихикать, а сами лежат поверх комнаты.
    val animation = rememberPetAnimation()

    // Где питомец на экране — игры целятся в него и в его рот.
    var petBounds by remember { mutableStateOf(Rect.Zero) }

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
    fun onToyDrag(toyId: String, centre: Offset, delta: Offset) {
        heldToy = toyId
        val pet = petBounds
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
                animation.playGiggle()
                val now = System.currentTimeMillis()
                if (now - lastHappySound > HappySoundGapMillis) {
                    lastHappySound = now
                    sounds.play(Sfx.PetHappy)
                    animation.playJoy()
                }
            }
        }
    }
    fun onToyDrop(toyId: String) {
        heldToy = toyId
        flushPlay()
        heldToy = null
        travel = 0f
        animation.lookAt(0f)
    }

    // Мало радости — питомец время от времени вздыхает и оседает: грусть видна
    // и без слов. Шкала при этом покачивается, зовёт на неё нажать.
    LaunchedEffect(state.moodLow, sleeping) {
        if (!state.moodLow || sleeping) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(SighPauseMs.first, SighPauseMs.last))
            animation.playSigh()
        }
    }

    // Подсказка к шкале настроения: что её поднимает. Открывается нажатием на шкалу.
    var moodHint by remember { mutableStateOf(false) }
    LaunchedEffect(moodHint) {
        if (moodHint) {
            delay(MoodHintMillis)
            moodHint = false
        }
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


    // FinneyScreen тут не подходит: он заливает фон кремовым и сам растит колонку,
    // а под интерфейсом должна быть видна комната. Свой корень — ровно поэтому,
    // сам FinneyScreen не трогаем, на нём держатся пять других экранов.
    Box(modifier = Modifier.fillMaxSize()) {

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
            onHop = animation::playJoy,
            toys = state.toys,
            toysEnabled = !sleeping && playing == null && care == null,
            onToyDrag = ::onToyDrag,
            onToyDrop = ::onToyDrop,
        ) {
            // Нажатие — питомец подпрыгивает: это игра, а не меню.
            //
            // Подсветку убираем: у питомца нет прямоугольной формы, и ripple
            // лёг бы квадратом вокруг.
            PetView(
                character = state.appearance.character,
                bodyColor = state.appearance.bodyColor,
                accessory = state.worn,
                mood = if (eyesClosed) PetMood.SLEEP else state.emotion.toMood(),
                pose = rememberPoseProvider(animation),
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { petBounds = it.boundsInRoot() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Погладить питомца",
                        onClick = {
                            if (!sleeping) {
                                sounds.play(Sfx.PetHappy)
                                animation.playJoy()
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
                MoneyButton(balance = state.balance, onClick = onOpenShop)
            }

            // Уровень — главный показатель роста, поэтому в полтора раза крупнее кнопок по краям.
            // Центры всех трёх на одной линии: ряд выравнивается по вертикали.
            Box(contentAlignment = Alignment.Center) {
                // Нажатие — что нужно для уровня. Дуга вокруг — сколько условий уже выполнено.
                LevelBadge(
                    level = state.level,
                    size = LevelBadgeSize,
                    progress = state.levelProgress,
                    modifier = Modifier.combinedClickable(
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
            InfoChip(
                icon = FinneyIcons.Piggy,
                text = state.goal
                    ?.let { "${it.goal.label}: ${it.saved} из ${it.goal.price}" }
                    ?: "Копилка: ${state.totalSavings}",
                action = "Копилка и цель",
                onClick = onOpenGoals,
                enabled = !sleeping,
                modifier = Modifier.weight(1f),
            )

            // Игра уровня — обязательное условие, поэтому вход в неё всегда на виду.
            // Список всех игр ребёнку не нужен: на каждом уровне своя (он остался в отладке).
            val levelTaskId = state.check.levelTaskId
            if (levelTaskId != null && state.levelGame != null) {
                InfoChip(
                    icon = FinneyIcons.Star,
                    text = if (state.check.gamePassed) "${state.levelGame} ✓" else "Игра: ${state.levelGame}",
                    action = "Игра уровня",
                    onClick = { onOpenTask(levelTaskId) },
                    enabled = !sleeping,
                    modifier = Modifier.weight(1f),
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
            LaunchedEffect(state.moodLow, sleeping) {
                if (!state.moodLow || sleeping) {
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
                MoodHint(
                    modifier = Modifier
                        .offset(x = barWidth + 4.dp, y = barHeight * 0.55f)
                        .clickable(onClickLabel = "Скрыть") { moodHint = false },
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
                    onWake = onWake,
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
            )
            // Спящего не кормят и не моют: кнопки полупрозрачные и не нажимаются.
            FinneyNeedButton(
                icon = FinneyIcons.Food,
                label = "Покормить",
                onClick = { goTo(RoomSpot.KITCHEN, CareTarget.FOOD) },
                value = state.stats.satiety,
                selected = spot == RoomSpot.KITCHEN,
                enabled = !sleeping,
                modifier = Modifier.graphicsLayer { alpha = if (sleeping) NightDim else 1f },
            )
            FinneyNeedButton(
                icon = FinneyIcons.Bath,
                label = "Помыть",
                onClick = { goTo(RoomSpot.BATH, CareTarget.BATH) },
                value = state.stats.hygiene,
                selected = spot == RoomSpot.BATH,
                enabled = !sleeping,
                modifier = Modifier.graphicsLayer { alpha = if (sleeping) NightDim else 1f },
            )
        }

        }

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
                )
            }
        }

        playing?.let { itemId ->
            val mouth = state.appearance.character.skin.mouthCenter
            when (spot) {
                RoomSpot.KITCHEN -> FeedingGame(
                    itemId = itemId,
                    pet = petBounds,
                    mouth = Offset(
                        petBounds.left + petBounds.width * mouth.pivotFractionX,
                        petBounds.top + petBounds.height * mouth.pivotFractionY,
                    ),
                    onMouthOpen = animation::openMouth,
                    onEaten = {
                        onBuy(itemId)
                        animation.playEat()
                        playing = null
                    },
                    onCancel = { playing = null },
                )
                RoomSpot.BATH -> WashingGame(
                    itemId = itemId,
                    pet = petBounds,
                    onScrub = animation::playGiggle,
                    onClean = {
                        onBuy(itemId)
                        animation.playJoy()
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
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
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
        FinneyButton(text = "Разбудить", onClick = onWake, fillWidth = false)
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
    FinneyPanel(title = "Уровень $level", modifier = modifier) {
        if (!check.planConfirmed) {
            Text(
                "Уровень начинается с плана: реши, сколько на нужное, сколько на «хочется» и сколько отложить.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            FinneyButton(text = "Составить план", onClick = onOpenBudget)
        } else {
            if (levelGame != null && check.levelTaskId != null) {
                Text(
                    if (check.gameRequired) "Обязательно пройди игру уровня:" else "Игра уровня — по желанию (демо):",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
                CheckRow("«$levelGame»", check.gamePassed)
            }
            Text(
                if (check.levelTaskId != null && check.gameRequired) {
                    "И выполни ${check.toPass} из $LEVEL_CONDITIONS:"
                } else {
                    "Чтобы пройти уровень, выполни ${check.toPass} из $LEVEL_CONDITIONS:"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            CheckRow("Питомец сыт, чист и выспался", check.needsCovered)
            CheckRow("Траты по плану", check.planMatched)
            CheckRow("Отложено в копилку", check.savingsAdded)
            Text(
                if (check.willPass) {
                    "Готово! Уровень будет пройден, и придут новые деньги."
                } else {
                    "Если завершить сейчас, уровень начнётся заново — с новыми деньгами."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            // Во сне уровень не завершают: условия видно, а итог — утром.
            if (asleep) {
                Text(
                    "Питомец спит. Завершить уровень можно, когда он проснётся.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
            }
            FinneyButton(
                text = "Завершить уровень",
                onClick = onFinish,
                enabled = !asleep,
                modifier = Modifier.graphicsLayer { alpha = if (asleep) NightDim else 1f },
            )
        }
        FinneyButton(text = "Итоги и история", onClick = onOpenProgress)
        FinneyButton(text = "Ещё поиграю", onClick = onDismiss)
    }
}

/** Условие уровня: галочка или прочерк словом — цвет тут ничего не решает (ТЗ п. 3.6). */
@Composable
private fun CheckRow(label: String, done: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label: ${if (done) "да" else "пока нет"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (done) "✓" else "—",
            style = MaterialTheme.typography.titleLarge,
            color = FinneyInk,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    }
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
 * Деньги и вход в магазин одной кнопкой.
 *
 * Баланс и магазин объединены намеренно: ребёнок смотрит на сумму как раз
 * тогда, когда собирается что-то купить, и отдельный кружок «Магазин» внизу
 * после этого лишний. Сумма берётся из состояния и на экране не пересчитывается.
 *
 * Сама кнопка — монета-финка, круглая и того же размера, что «бургер» справа:
 * верхний ряд читается как три круга вокруг уровня. Сумма — плашкой в правом
 * нижнем углу монеты, как счётчик на значке: бежевая подложка, синяя обводка
 * и синий текст, как у плашек копилки и задания.
 */
@Composable
private fun MoneyButton(balance: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "coinPress")

    // «Поп», когда монет стало больше: монета вздувается и пружинит обратно,
    // над ней всплывает «+N». Какая сумма уже показана, помнит rememberSaveable:
    // он переживает уход на другой экран, и вернувшись с мини-игры или итогов
    // периода, ребёнок видит прибавку. При первом открытии и при тратах — без попа.
    var shown by rememberSaveable { mutableStateOf<Int?>(null) }
    val pop = remember { Animatable(1f) }
    val rise = remember { Animatable(0f) }
    var gain by remember { mutableIntStateOf(0) }
    LaunchedEffect(balance) {
        val before = shown
        shown = balance
        if (before == null || balance <= before) return@LaunchedEffect
        gain = balance - before
        launch {
            pop.animateTo(1.35f, tween(durationMillis = 110))
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        rise.snapTo(0f)
        rise.animateTo(1f, tween(durationMillis = 1100))
        gain = 0
    }
    val scale = pressScale * pop.value

    // Один контейнер размером с монету: плашка привязана к его правому нижнему
    // углу и выходит за край смещением, поэтому ряд не раздвигается от длины суммы.
    //
    // Плашка и «+N» меряются без ограничения по ширине (wrapContentSize с unbounded):
    // контейнер всего 56 dp, и раньше сумма от четырёх цифр в него не влезала —
    // maxLines = 1 молча срезал хвост числа. Длинная сумма растёт влево, по монете.
    // Предки кнопки не должны гасить её через graphicsLayer: alpha меньше 1 рисует
    // слой вне экрана и обрезает всё, что вышло за контейнер, — так сумму и
    // срезало во сне, когда верхний ряд приглушался.
    Box(
        modifier = Modifier
            .size(MoneyCoinSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clearAndSetSemantics {
                contentDescription = "$balance финок, открыть магазин"
                role = Role.Button
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            Coin(size = MoneyCoinSize)
        }
        Text(
            text = balance.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 8.dp)
                .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                .clip(RoundedCornerShape(percent = 50))
                .background(FinneySand)
                .border(StrokeThin, FinneyInk, RoundedCornerShape(percent = 50))
                .clickable(onClick = onClick)
                .padding(horizontal = 7.dp),
        )
        if (gain > 0) {
            OutlinedText(
                text = "+$gain",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-4).dp)
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        translationY = -rise.value * 28.dp.toPx()
                        // Первую половину пути видна целиком, потом тает.
                        alpha = (2f - rise.value * 2f).coerceIn(0f, 1f)
                    }
                    .clearAndSetSemantics { contentDescription = "Получено $gain финок" },
            )
        }
    }
}

/** Монета того же размера, что «бургер» справа. */
private val MoneyCoinSize = 56.dp

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
 */
@Composable
private fun InfoChip(
    icon: FinneyIcons,
    text: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else NightDim }
            .clip(RoundedCornerShape(RadiusField))
            .background(FinneySand)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(RadiusField))
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FinneyIcon(icon, size = 22.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = FinneyInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun HomeContentPreview() {
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
            ),
            onOpenBudget = {}, onOpenShop = {}, onOpenGoals = {}, onOpenTasks = {}, onOpenTask = {}, onOpenWardrobe = {},
            onOpenProgress = {}, onOpenSettings = {}, onOpenHelp = {},
            onClosePeriod = {}, onBuy = {},
        )
    }
}
