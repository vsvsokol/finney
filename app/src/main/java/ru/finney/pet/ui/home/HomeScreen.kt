package ru.finney.pet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
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
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.StatPill
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.RadiusField
import ru.finney.pet.ui.theme.StrokeThin
import ru.finney.pet.ui.theme.FinneyTheme

// Главный экран по каркасу 1:275: сверху деньги, уровень и служебные кнопки,
// посередине питомец со шкалами, снизу ряд действий. Всё, что требует ТЗ п. 2.5.3,
// видно сразу и без прокрутки: середина растягивается по остатку высоты,
// поэтому на экране 640 dp ничего не срезается.

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
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    FinneyScreen(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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

            LevelBadge(level = state.level, size = 56.dp)

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

        // Шкалы — отдельной полосой над сценой, а не поверх неё: лёжа на питомце
        // они перекрывали ему лицо, а подложки у них нет.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPill(label = "сытость", icon = FinneyIcons.Food, value = state.stats.satiety)
            StatPill(label = "чистота", icon = FinneyIcons.Bath, value = state.stats.hygiene)
            StatPill(label = "радость", icon = FinneyIcons.Star, value = state.stats.mood)
        }

        // ---------- Середина: сцена питомца ----------
        // Питомец занимает всю свободную высоту и стоит по центру. Он здесь главный,
        // а не иллюстрация при показаниях приборов: шкалы и подпись уведены к краям
        // сцены, чтобы не спорить с ним за внимание.
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val animation = rememberPetAnimation()
            // Питомец квадратный и рисуется от «земли», поэтому на всю ширину
            // он вылезает за низ сцены и налезает на подпись. Ограничиваем по
            // высоте: PetView сам держит пропорции внутри (ui/pet/ — зона @vsvsokol,
            // трогаем только то, что отдаём ему снаружи).
            // Нажатие на самого питомца открывает черновик анимаций — отдельная
            // кнопка внизу под это больше не нужна. Подсветку убираем: у питомца
            // нет прямоугольной формы, и ripple лёг бы квадратом вокруг него.
            PetView(
                character = state.appearance.character,
                mood = state.emotion.toMood(),
                pose = rememberPoseProvider(animation),
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Анимации питомца",
                        onClick = onOpenPetLab,
                    ),
            )
        }

        // Имя и период — строкой под сценой. Раньше подпись лежала внутри сцены
        // по нижнему краю и налезала питомцу на ноги.
        Text(
            text = "${state.petName} · период ${state.periodNumber}",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
        )

        // Цель и подсказка — то, ради чего копят (ТЗ п. 2.5.3).
        state.goal?.let { goal ->
            InfoStrip(text = "${goal.goal.label}: ${goal.saved} из ${goal.goal.price}")
        }
        state.needsHint?.takeIf { it > 0 }?.let {
            InfoStrip(text = "На нужное понадобится $it")
        }

        // ---------- Низ: действия ----------
        // Кружки крупные и без подписей: ряд стал полосой действий, как в играх
        // про питомцев, а не пятью пунктами меню. Название ушло в TalkBack.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionButton(label = "Зал", icon = FinneyIcons.Lamp, onClick = onOpenGoals)
            ActionButton(label = "Кухня", icon = FinneyIcons.Food, onClick = onOpenShop)
            ActionButton(label = "Ванная", icon = FinneyIcons.Bath, onClick = onOpenProgress)
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
 * Кнопка нижнего ряда: крупный кружок со значком, без подписи.
 *
 * Подпись убрана намеренно: пять кружков с текстом под каждым читались как меню
 * приложения. [label] никуда не делся — он уходит в TalkBack, так что действие
 * по-прежнему называется словами, а не только рисунком.
 *
 * Значки рисуются кодом ([FinneyIcon]): в эталоне они плоские и одного цвета
 * с обводкой, а стоявшие тут раньше эмодзи были многоцветными и выбивались.
 */
@Composable
private fun ActionButton(label: String, icon: FinneyIcons, onClick: () -> Unit) {
    FinneyIconButton(onClick = onClick, contentDescription = label, size = 64.dp) {
        FinneyIcon(icon, size = 32.dp)
    }
}

/** Узкая полоса с фактом: цель, подсказка. */
@Composable
private fun InfoStrip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = FinneyInk,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusField))
            .background(FinneySand)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(RadiusField))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
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
                stage = 1,
                balance = 50,
                totalSavings = 0,
                goal = null,
                periodNumber = 1,
                phase = PeriodPhase.PLANNING,
                needsHint = 40,
                nextTask = null,
            ),
            onOpenBudget = {}, onOpenShop = {}, onOpenGoals = {}, onOpenTasks = {}, onOpenTask = {},
            onOpenProgress = {}, onOpenAdult = {}, onOpenHelp = {}, onOpenPetLab = {}, onOpenOnboarding = {},
            onClosePeriod = {},
        )
    }
}
