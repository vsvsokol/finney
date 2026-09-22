package ru.finney.pet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyStrokeRatio
import ru.finney.pet.ui.theme.FinneyYellow
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import ru.finney.pet.ui.theme.FinneyCream
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.FinneyNeedButton
import ru.finney.pet.ui.components.HappinessBar
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.room.CareBlock
import ru.finney.pet.ui.room.CareOption
import ru.finney.pet.ui.room.CarePanel
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
    onOpenProgress: () -> Unit,
    onOpenAdult: () -> Unit,
    onOpenHelp: () -> Unit,
    onPeriodClosed: (periodNumber: Int) -> Unit,
    onOpenPetLab: () -> Unit,
    onOpenOnboarding: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.PeriodClosed -> onPeriodClosed(event.periodNumber)
                is HomeEvent.Rejected -> Unit // кнопка и так неактивна до подтверждения плана
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
            onOpenProgress = onOpenProgress,
            onOpenAdult = onOpenAdult,
            onOpenHelp = onOpenHelp,
            onOpenPetLab = onOpenPetLab,
            onOpenOnboarding = onOpenOnboarding,
            onClosePeriod = viewModel::closePeriod,
            onBuy = viewModel::buy,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onOpenBudget: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenAdult: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPetLab: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onClosePeriod: () -> Unit,
    onBuy: (itemId: String) -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

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

    fun openCare(target: CareTarget) {
        care = target
        picked = null
    }

    // Кнопка внизу сначала показывает комнату, и только повторное нажатие
    // открывает выбор: по макету её дело — «появляется стол», «появляется ванна».
    // Ребёнок сперва видит, куда попал, и лишь потом тратит деньги.
    fun goTo(next: RoomSpot, target: CareTarget) {
        if (spot == next) openCare(target) else spot = next
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
                    // Зал пока пустой: свет и сон — отдельная работа после сдачи.
                    RoomSpot.LIVING -> Unit
                }
            },
        ) {
            val animation = rememberPetAnimation()
            // Нажатие — питомец подпрыгивает: это игра, а не меню. Черновик
            // анимаций, который раньше открывался здесь же, ушёл на долгое
            // нажатие и только в отладочной сборке — ребёнку он не нужен,
            // а команде по-прежнему под рукой.
            //
            // Подсветку убираем: у питомца нет прямоугольной формы, и ripple
            // лёг бы квадратом вокруг.
            PetView(
                character = state.appearance.character,
                mood = state.emotion.toMood(),
                pose = rememberPoseProvider(animation),
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Погладить питомца",
                        onLongClick = if (isDebuggable) onOpenPetLab else null,
                        onLongClickLabel = if (isDebuggable) "Черновик анимаций" else null,
                        onClick = animation::playJoy,
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
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                MoneyButton(balance = state.balance, onClick = onOpenShop)
            }

            LevelBadge(level = state.level, size = 56.dp, progress = state.levelProgress)

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FinneyIconButton(
                    onClick = onOpenHelp,
                    contentDescription = "Подсказка",
                    size = 56.dp,
                ) {
                    FinneyIcon(FinneyIcons.Help, size = 26.dp)
                }
                // «Бургер» — всё, что не про уход за питомцем: знакомство с игрой,
                // активное задание, план, копилка, прогресс и раздел взрослого.
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
                        task = state.nextTask,
                        onOpenTask = onOpenTask,
                        onOpenOnboarding = onOpenOnboarding,
                        onOpenBudget = onOpenBudget,
                        onOpenTasks = onOpenTasks,
                        onOpenProgress = onOpenProgress,
                        onOpenAdult = onOpenAdult,
                    )
                }
            }
        }

        // Копилка и активное задание — то, чего макет на главном не предусмотрел,
        // а ТЗ п. 2.5.3 требует видеть сразу. Обе плашки умещаются в одну строку
        // и лежат в кремовой полосе над комнатой: строкой ниже начинается абажур
        // лампы, и второй ряд его бы срезал. Сытость, чистота и настроение сюда
        // не попадают — они переехали в кольца кнопок и в шкалу слева.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoChip(
                icon = FinneyIcons.Piggy,
                text = state.goal
                    ?.let { "${it.goal.label}: ${it.saved} из ${it.goal.price}" }
                    ?: "Копилка: ${state.totalSavings}",
                action = "Копилка и цель",
                onClick = onOpenGoals,
                modifier = Modifier.weight(1f),
            )

            // Слово «задание» в плашку не влезает — его держит значок звезды
            // и подпись для TalkBack.
            state.nextTask?.let { task ->
                InfoChip(
                    icon = FinneyIcons.Star,
                    text = task.title,
                    action = "Задание: ${task.title}",
                    onClick = { onOpenTask(task.id) },
                    modifier = Modifier.weight(1f),
                )
            }
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
            HappinessBar(
                value = state.stats.mood,
                // Кружок-лицо выступает под капсулой на ширину шкалы — вычитаем.
                height = maxHeight * 0.55f - barWidth,
                width = barWidth,
                modifier = Modifier.offset(x = -HappinessEdgeShift),
            )
        }

        // ---------- Низ: комнаты ----------
        // Три кнопки из макета. Каждая переключает комнату, а кольцо вокруг
        // показывает потребность, которую в этой комнате закрывают, — так они
        // нарисованы в ките («опускание/поднятие шкал потребностей»).
        //
        // У зала кольца нет: отдельной шкалы сна в домене не существует,
        // а рисовать пустое кольцо ради симметрии — врать про данные.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FinneyNeedButton(
                icon = FinneyIcons.Lamp,
                label = "Зал и сон",
                onClick = { spot = RoomSpot.LIVING },
                selected = spot == RoomSpot.LIVING,
            )
            FinneyNeedButton(
                icon = FinneyIcons.Food,
                label = "Покормить",
                onClick = { goTo(RoomSpot.KITCHEN, CareTarget.FOOD) },
                value = state.stats.satiety,
                selected = spot == RoomSpot.KITCHEN,
            )
            FinneyNeedButton(
                icon = FinneyIcons.Bath,
                label = "Помыть",
                onClick = { goTo(RoomSpot.BATH, CareTarget.BATH) },
                value = state.stats.hygiene,
                selected = spot == RoomSpot.BATH,
            )
        }

        }

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
                            reason = "Сначала распредели деньги в плане расходов — " +
                                "после этого можно покупать",
                            actionLabel = "К плану расходов",
                            onAction = onOpenBudget,
                        )
                    },
                    options = previews.map {
                        CareOption(it.item, it, isSelected = it.item.id == picked)
                    },
                    onPick = { picked = it },
                    onConfirm = { itemId ->
                        onBuy(itemId)
                        care = null
                        picked = null
                    },
                    onDismiss = { care = null; picked = null },
                )
            }
        }
    }
}

/**
 * Меню «бургера»: всё, что не про уход за питомцем.
 *
 * Активное задание стоит первым пунктом и подписано своим названием — раньше
 * оно занимало на экране целую кнопку, а нужно оно не в каждый момент.
 */
@Composable
private fun HomeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    task: TaskDefinition?,
    onOpenTask: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenAdult: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(FinneyCream),
    ) {
        // Каждый пункт сначала закрывает меню: иначе после возврата с экрана
        // оно осталось бы раскрытым поверх главного.
        task?.let { active ->
            HomeMenuItem("Задание: ${active.title}") {
                onDismiss()
                onOpenTask(active.id)
            }
        }
        HomeMenuItem("Знакомство с игрой") { onDismiss(); onOpenOnboarding() }
        HomeMenuItem("План расходов") { onDismiss(); onOpenBudget() }
        HomeMenuItem("Все задания") { onDismiss(); onOpenTasks() }
        HomeMenuItem("Прогресс") { onDismiss(); onOpenProgress() }
        HomeMenuItem("Для взрослых") { onDismiss(); onOpenAdult() }
    }
}

/** Пункт меню «бургера». */
@Composable
private fun HomeMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk) },
        onClick = onClick,
    )
}

/**
 * Деньги и вход в магазин одной кнопкой.
 *
 * Баланс и магазин объединены намеренно: ребёнок смотрит на сумму как раз
 * тогда, когда собирается что-то купить, и отдельный кружок «Магазин» внизу
 * после этого лишний. Сумма берётся из состояния и на экране не пересчитывается.
 *
 * Форма — «стадион» с обводкой и нижней полосой, как у остальных кнопок кита,
 * но по содержимому: внутри сумма и монета, а не надпись.
 */
@Composable
private fun MoneyButton(balance: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .defaultMinSize(minWidth = 112.dp, minHeight = 56.dp)
            .clearAndSetSemantics { contentDescription = "$balance финок, открыть магазин" },
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .moneyButtonFill(pressed)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedText(
                    balance.toString(),
                    style = MaterialTheme.typography.titleLarge,
                )
                Coin(size = 26.dp)
            }
        }
    }
}

/** Заливка кнопки денег: те же цвета и полоса, что у [FinneyButton]. */
private fun Modifier.moneyButtonFill(pressed: Boolean): Modifier = drawBehind {
    val face = if (pressed) FinneyPeach else FinneyYellow
    val band = if (pressed) FinneyPink else FinneyPeach
    drawRect(face)
    val bandHeight = size.height * 0.25f
    drawRect(
        color = band,
        topLeft = Offset(0f, size.height - bandHeight),
        size = Size(size.width, bandHeight),
    )
    val outline = size.height * FinneyStrokeRatio
    drawRoundRect(
        color = FinneyInk,
        topLeft = Offset(outline / 2f, outline / 2f),
        size = Size(size.width - outline, size.height - outline),
        cornerRadius = CornerRadius(size.height / 2f),
        style = Stroke(width = outline),
    )
}

/**
 * Плашка-кнопка с фактом: копилка, активное задание.
 *
 * Значок слева обязателен: по ТЗ п. 3.6 одним цветом ничего не сообщают,
 * и плашки должны отличаться друг от друга не только словами. Высота — от
 * 48 dp, потому что по плашке нажимают.
 *
 * [action] — что произойдёт при нажатии; уходит в TalkBack вместо [text],
 * который сам по себе действия не называет.
 */
@Composable
private fun InfoChip(
    icon: FinneyIcons,
    text: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusField))
            .background(FinneySand)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(RadiusField))
            .clickable(onClickLabel = action, onClick = onClick)
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
    // Голод отдельного лица не имеет: голодный питомец выглядит грустным.
    Emotion.HUNGRY, Emotion.SAD -> PetMood.SAD
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
                stats = PetStats(30, 40, 50),
                emotion = Emotion.CALM,
                level = 1,
                levelProgress = 0.4f,
                stage = 1,
                balance = 50,
                totalSavings = 0,
                goal = null,
                periodNumber = 1,
                phase = PeriodPhase.PLANNING,
                needsHint = 40,
                nextTask = null,
                food = emptyList(),
                care = emptyList(),
            ),
            onOpenBudget = {}, onOpenShop = {}, onOpenGoals = {}, onOpenTasks = {}, onOpenTask = {},
            onOpenProgress = {}, onOpenAdult = {}, onOpenHelp = {}, onOpenPetLab = {}, onOpenOnboarding = {},
            onClosePeriod = {}, onBuy = {},
        )
    }
}
