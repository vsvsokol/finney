package ru.finney.pet.ui.budget

import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import ru.finney.pet.domain.model.Category
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyQuietButton
import ru.finney.pet.ui.components.PlanDonut
import ru.finney.pet.ui.components.SavingsIcon
import ru.finney.pet.ui.components.StableText
import ru.finney.pet.ui.components.categoryIcon
import ru.finney.pet.ui.room.itemArt
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeBold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.game.PlanReport
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.ui.components.AMOUNT_STEP
import ru.finney.pet.ui.components.AmountStepper
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.WarningBadge
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FeedbackSound
import ru.finney.pet.ui.components.FillBar
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.SpendBar
import ru.finney.pet.ui.pet.accessoryArt
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.RadiusCard
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx

// Экран плана (ТЗ п. 2.5.5). Суммы набираются кнопками «плюс» и «минус» (AmountStepper),
// и в каждое из трёх направлений нужно положить хоть что-то.

private const val STEP = AMOUNT_STEP

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // План подтверждён — часть дохода сразу ушла в копилку: монета падает в неё.
    val sounds = LocalSounds.current
    var plannedSavings by remember { mutableStateOf(0) }
    LaunchedEffect(state) {
        when (val s = state) {
            is BudgetUiState.Planning -> plannedSavings = s.savings
            is BudgetUiState.Active -> {
                if (plannedSavings > 0) sounds.play(Sfx.PiggyIn)
                plannedSavings = 0
            }
            BudgetUiState.Loading -> Unit
        }
    }

    when (val s = state) {
        BudgetUiState.Loading -> FinneyScreen {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FinneyInk)
            }
        }
        is BudgetUiState.Planning -> PlanningContent(
            state = s,
            onNeedsChange = viewModel::setNeeds,
            onWantsChange = viewModel::setWants,
            onSavingsChange = viewModel::setSavings,
            onSelectGoal = viewModel::selectGoal,
            onRepeatLast = viewModel::repeatLastPlan,
            onConfirm = viewModel::confirm,
            onBack = onBack,
        )
        is BudgetUiState.Active -> ActiveContent(state = s, onBack = onBack)
    }
}

@Composable
private fun PlanningContent(
    state: BudgetUiState.Planning,
    onNeedsChange: (Int) -> Unit,
    onWantsChange: (Int) -> Unit,
    onSavingsChange: (Int) -> Unit,
    onSelectGoal: (String) -> Unit,
    onRepeatLast: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onClose = onBack,
        // Круг — в шапке, а не в прокрутке: на него смотрят, пока жмут «+» в части
        // внизу, и он должен быть виден всегда (плейтест).
        top = { PlanHeader(state) },
    ) {
        // Пока ничего не разложено — одна строка, что делать. Дальше её место
        // занимают сами части: круг уже показывает, что происходит.
        if (state.planned == 0) {
            Text(
                "Жми «+» в каждой части — монетки лягут в круг.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
        }

        // Каждый уровень план собирается заново, и на плейтесте это было скучно:
        // одной кнопкой — как в прошлый раз, а дальше подправить «−/+».
        if (state.canRepeatLast) {
            FinneyQuietButton(
                text = "Как в прошлый раз",
                onClick = onRepeatLast,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Шаг помещается в остаток — значит, добавлять ещё можно.
        val canAdd = state.remainder >= STEP

        AmountRow(
            icon = categoryIcon(Category.NEEDS),
            label = "Нужное",
            pictures = state.needsItems.mapNotNull(::itemArt),
            value = state.needs,
            onChange = onNeedsChange,
            canAdd = canAdd,
            budget = state.budget,
            minimum = state.needsHint?.takeIf { it > 0 },
        )
        AmountRow(
            icon = categoryIcon(Category.WANTS),
            label = "Желаемое",
            pictures = state.wantsItems.mapNotNull(::itemArt),
            value = state.wants,
            onChange = onWantsChange,
            canAdd = canAdd,
            budget = state.budget,
        )
        // Без выбранной цели откладывать некуда: цель выбирается прямо здесь,
        // а не на другом экране, — иначе план было бы не собрать.
        if (state.goalLabel == null) {
            FinneyCard {
                CategoryTitle(SavingsIcon, "Копилка")
                Text("На что копим? Выбери цель:", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
                state.goals.forEach { goal -> GoalChoice(goal, onClick = { onSelectGoal(goal.id) }) }
            }
        } else {
            AmountRow(
                icon = SavingsIcon,
                label = "Копилка",
                pictures = listOfNotNull(state.goalReward?.let(::itemArt)),
                value = state.savings,
                onChange = onSavingsChange,
                canAdd = canAdd,
                budget = state.budget,
                subtitle = "Цель: ${state.goalLabel}",
            )
        }

        FeedbackSound(feedback = null, rejection = state.rejection)
        state.rejection?.let { RejectionNote(it) }

        // Почему кнопка погашена — по пунктам и значком, а не одним серым цветом.
        // Чего не хватает, решает состояние из ViewModel, экран только показывает.
        if (state.missing.isNotEmpty()) MissingList(state.missing)

        FinneyButton(
            text = "Подтвердить план",
            onClick = onConfirm,
            enabled = state.canConfirm,
        )
    }
}

/** Сумма в середине круга: монетка над числом — рядом в дырку круга они не влезают. */
@Composable
private fun BudgetCenter(budget: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Есть $budget финок" },
    ) {
        Coin(size = 26.dp)
        OutlinedText(budget.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

/** Заголовок части: значок части (тот же, что на круге) и слово. */
@Composable
private fun CategoryTitle(
    icon: FinneyIcons,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        FinneyIcon(icon, size = 30.dp)
        Text(label, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
    }
}

/**
 * Одна часть плана: значок и слово, ниже сумма с кнопками, а над суммой — рисунки
 * того, что на неё покупают. Плейтест: «наглядно показать, что тут речь о еде, мыле»,
 * поэтому у «Нужного» яблоко, суп и мыло, а у копилки — вещь цели.
 *
 * [minimum] — сколько нужно хотя бы: отметкой на шкале ([MinimumBar]), а не строкой
 * текста, которая «плохо сидела» рядом с заголовком.
 *
 * [canAdd] — есть ли ещё нераспределённые деньги. Когда бюджет разобран весь,
 * «плюс» гаснет во всех строках сразу: ребёнок не составит план, который
 * домен всё равно отклонит, и объяснять отказ не придётся.
 */
@Composable
private fun AmountRow(
    icon: FinneyIcons,
    label: String,
    pictures: List<Int>,
    value: Int,
    onChange: (Int) -> Unit,
    canAdd: Boolean,
    budget: Int,
    minimum: Int? = null,
    subtitle: String? = null,
) {
    FinneyCard {
        CategoryTitle(icon, label)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = FinneyInk) }
        // Рисунки — над суммой, между «−» и «+»: что покупают на эти монеты, стоит
        // прямо на них, а не у края карточки. Диктору они не нужны: слово он уже прочёл.
        AmountStepper(label = label, value = value, onChange = onChange, canAdd = canAdd, step = STEP) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                pictures.forEach { art ->
                    Image(painter = painterResource(art), contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
        }
        minimum?.let { MinimumBar(value = value, minimum = it, budget = budget) }
    }
}

/**
 * Шкала части с отметкой «хотя бы»: заливка — сколько положено из всего бюджета,
 * черта — минимум, под ней число. Дотянул до черты — в конце «✓», не дотянул — «!»:
 * не только цветом (ТЗ п. 3.6).
 */
@Composable
private fun MinimumBar(value: Int, minimum: Int, budget: Int) {
    val total = budget.coerceAtLeast(1)
    val shown by animateFloatAsState((value.toFloat() / total).coerceIn(0f, 1f), label = "part")
    val mark = (minimum.toFloat() / total).coerceIn(0f, 1f)
    val enough = value >= minimum
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = if (enough) "Хватает: нужно хотя бы $minimum" else "Нужно хотя бы $minimum, положено $value"
            },
    ) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val barHeight = 16.dp
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(FinneyCream)
                    .border(StrokeRegular, FinneyInk, RoundedCornerShape(50)),
            ) {
                drawRect(FinneyYellow, size = size.copy(width = size.width * shown))
            }
            // Черта минимума выходит за полосу сверху и снизу — её видно и поверх заливки.
            val x = maxWidth * mark
            Box(
                Modifier
                    .offset(x = x - StrokeBold / 2, y = (-4).dp)
                    .size(width = StrokeBold, height = barHeight + 8.dp)
                    .background(FinneyInk, RoundedCornerShape(50)),
            )
            // Подпись под чертой, но не за краем шкалы: у самого края она сдвигается внутрь.
            val labelWidth = 110.dp
            Text(
                text = "хотя бы $minimum",
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = barHeight + 6.dp)
                    .offset(x = (x - labelWidth / 2).coerceIn(0.dp, (maxWidth - labelWidth).coerceAtLeast(0.dp)))
                    .width(labelWidth),
            )
        }
        if (enough) CheckBadge(size = 26.dp) else WarningBadge(size = 26.dp)
    }
}

/**
 * Цель на выбор: рисунок награды, название и цена. Плейтест: текстовые кнопки
 * «Ковбойская шляпа · 50» не показывали, что это за вещь. Рисунок — по `reward` цели,
 * тем же путём, что в гардеробе и на экране целей; у цели без рисунка — только слова.
 */
@Composable
private fun GoalChoice(goal: Goal, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(RadiusCard))
            .background(FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .clickable(role = Role.Button, onClickLabel = "Выбрать цель", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${goal.label}, цена ${goal.price}" }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        goal.reward?.let(::accessoryArt)?.let {
            Image(painter = painterResource(it.res), contentDescription = null, modifier = Modifier.size(56.dp))
        }
        Text(goal.label, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
        CoinAmount(amount = goal.price, coinSize = 26.dp)
    }
}

/** Чего не хватает до «Подтвердить» — по пункту на строку, у каждого значок. */
@Composable
private fun MissingList(items: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Чтобы подтвердить:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WarningBadge(size = 24.dp)
                Text(item, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Шапка плана: круг слева, справа — заголовок и сколько ещё не разложено.
 * Сколько можно распределить — в середине круга: это главное число экрана.
 * Круг меняется с каждым «−/+» — видно, какая часть от всего уходит куда
 * (плейтест: «диаграмму прикольно сделать»).
 */
@Composable
private fun PlanHeader(state: BudgetUiState.Planning) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        PlanDonut(
            budget = state.budget,
            needs = state.needs,
            wants = state.wants,
            savings = state.savings,
            diameter = 148.dp,
        ) { BudgetCenter(state.budget) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedText("План", style = MaterialTheme.typography.headlineLarge)
            RemainderNote(state.remainder)
        }
    }
}

/**
 * Остаток — пустая дуга круга, словами. «Не разложено» — просто число, без цвета:
 * зелёное читалось как похвала. «Не хватает» — предупреждение: голубое и с «!»
 * (ТЗ п. 3.6 — не только цветом).
 */
@Composable
private fun RemainderNote(remainder: Int) {
    val over = remainder < 0
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusCard))
            .background(if (over) FinneyBlue else FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (over) WarningBadge(size = 24.dp)
            Text(
                text = when {
                    over -> "Не хватает"
                    remainder == 0 -> "Всё разложено"
                    else -> "Не разложено"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
            )
        }
        // Число — всегда, и ноль тоже: ТЗ п. 2.5.5 требует показывать остаток, а не только слова.
        CoinAmount(amount = if (over) -remainder else remainder)
    }
}

/** Отказ домена, переведённый на детский язык. Тексты потом заменит feedback.json @vsvsokol. */
@Composable
private fun RejectionNote(rejection: Rejection) {
    val text = when (rejection) {
        is Rejection.PlanExceedsBudget ->
            "Ты разделил ${rejection.planned}, а есть только ${rejection.budget}. Убавь что-нибудь."
        is Rejection.InsufficientFunds -> "Не хватает ${rejection.shortage} финок."
        Rejection.InvalidAmount -> "Так не получится: суммы не могут быть меньше нуля."
        Rejection.PlanAlreadyConfirmed -> "План на этот уровень уже готов."
        Rejection.NoActiveGoal -> "Выбери цель — тогда будет куда откладывать."
        Rejection.PlanMissingDirection -> "Положи хоть немного в каждую часть: нужное, желаемое и копилку."
        else -> "Так пока нельзя."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = FinneyInk,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(FinneyPink)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .padding(12.dp),
    )
}

@Composable
private fun ActiveContent(state: BudgetUiState.Active, onBack: () -> Unit) {
    val report = state.report
    // «Дальше» внизу, а не только «✕»: сразу после подтверждения ребёнок попадает
    // сюда, и на плейтесте не понял, что крестик и есть путь дальше, в комнату.
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onClose = onBack,
        bottom = { FinneyButton(text = "Дальше", onClick = onBack) },
    ) {
        OutlinedText("План и факт", style = MaterialTheme.typography.headlineLarge)

        // Сначала вывод, потом подробности: плейтест спрашивал, «что я должен понять»
        // из строк с числами, — вот это одной фразой.
        VerdictNote(state.verdict)

        FinneyPanel(title = "Уровень ${state.level}") {
            PlanFact(categoryIcon(Category.NEEDS), "Нужное", report.plan.needs, report.facts.needs, done = "Потратил")
            PlanFact(categoryIcon(Category.WANTS), "Желаемое", report.plan.wants, report.facts.wants, done = "Потратил")
            PlanFact(SavingsIcon, "Копилка", report.plan.savings, report.facts.savings, done = "Отложил", saving = true)
        }

        // Одна монетка с числом внизу была непонятно чем — подписываем словами.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            Text("Сейчас у тебя", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            CoinAmount(amount = state.balance)
        }
    }
}

/**
 * Вывод «План и факт» одной фразой со значком. Уровень ещё идёт, это не итог,
 * поэтому панель нейтральная: персиковое «План» на зелёной подложке читали как
 * «красный текст на зелёном — будто я что-то сделал не так» (ТЗ п. 2.5.9, 3.6).
 */
@Composable
private fun VerdictNote(verdict: PlanVerdict) {
    val (title, line) = when (verdict) {
        PlanVerdict.OnTrack -> "Идёшь по плану" to "Тратишь не больше, чем задумал."
        is PlanVerdict.Overspent -> "Больше плана на ${verdict.amount}" to "Купил больше, чем задумал. Смотри, где полоса длиннее."
        is PlanVerdict.SavingsShort -> "В копилку ещё ${verdict.amount}" to "Отложи их до конца уровня — так по плану."
        PlanVerdict.OffTrack -> "Пока не по плану" to "Сравни полосы: что задумал и что вышло."
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .padding(14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        if (verdict == PlanVerdict.OnTrack) CheckBadge(size = 40.dp) else WarningBadge(size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
        }
    }
}

/**
 * Часть плана двумя полосами: «Собирался» — сколько задумал, [done] — сколько вышло.
 * Шкала у пары общая, поэтому длины сравниваются на глаз, без «10 из 10».
 * Трата сверх плана — розовая полоса и «!» ([SpendBar]); у копилки [saving] —
 * там больше плана не ошибка, и полоса обычная.
 */
@Composable
private fun PlanFact(icon: FinneyIcons, label: String, planned: Int, fact: Int, done: String, saving: Boolean = false) {
    val scale = maxOf(planned, fact).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CategoryTitle(icon, label)
        BarLine("Собирался", planned) { ScaleBar(planned, scale, FinneyPeach) }
        BarLine(done, fact) {
            if (saving || fact <= planned) ScaleBar(fact, scale, FinneyYellow) else SpendBar(fact, planned)
        }
    }
}

/** Строка полосы: слово слева, полоса, число справа. Слова и числа — колонками одной ширины. */
@Composable
private fun BarLine(word: String, amount: Int, bar: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$word $amount" },
    ) {
        StableText(word, widest = "Собирался", style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f)) { bar() }
        StableText(amount.toString(), widest = "000", style = MaterialTheme.typography.titleMedium)
    }
}

/** Полоса [value] из [scale] одним цветом — без «✓» и «!»: это мерка, а не цель. */
@Composable
private fun ScaleBar(value: Int, scale: Int, color: Color) {
    val shown by animateFloatAsState((value.toFloat() / scale).coerceIn(0f, 1f), label = "bar")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(50)),
    ) {
        drawRect(color, size = size.copy(width = size.width * shown))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun PlanningContentPreview() {
    FinneyTheme {
        PlanningContent(
            state = BudgetUiState.Planning(
                periodNumber = 1, level = 1, budget = 50, needs = 30, wants = 10, savings = 10,
                needsHint = 30, goalLabel = "Ковбойская шляпа", rejection = null, isSaving = false,
                needsItems = listOf("food_apple", "food_bowl", "care_soap"),
                wantsItems = listOf("treat_candy", "toy_ball", "toy_book"),
                goalReward = "hat_cowboy",
                lastPlan = Plan(budget = 50, needs = 25, wants = 15, savings = 10),
            ),
            onNeedsChange = {}, onWantsChange = {}, onSavingsChange = {}, onSelectGoal = {}, onRepeatLast = {}, onConfirm = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun ActiveContentPreview() {
    FinneyTheme {
        ActiveContent(
            state = BudgetUiState.Active(
                periodNumber = 1,
                level = 1,
                report = PlanReport(
                    plan = Plan(budget = 50, needs = 30, wants = 10, savings = 10),
                    facts = PeriodFacts(needs = 25, wants = 10, savings = 10, unplannedIncome = 0),
                    overspend = 0,
                    onTrack = true,
                ),
                balance = 5,
            ),
            onBack = {},
        )
    }
}
