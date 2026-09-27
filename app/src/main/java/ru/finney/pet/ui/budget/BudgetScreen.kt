package ru.finney.pet.ui.budget

import androidx.compose.foundation.Image
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
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onClose = onBack,
    ) {
        OutlinedText("План", style = MaterialTheme.typography.headlineLarge)

        // Сколько можно распределить — крупно и сверху: это главное число экрана.
        // Подпись «Есть:» и подсказка «Разложи на три части…» убраны: монетка с
        // числом понятна сама, а правило «в каждую хоть немного» написано внизу,
        // когда кнопка ещё погашена.
        CoinAmount(amount = state.budget)

        // Шаг помещается в остаток — значит, добавлять ещё можно.
        val canAdd = state.remainder >= STEP

        AmountRow(
            label = "Нужное",
            hint = state.needsHint?.takeIf { it > 0 }?.let { "нужно хотя бы $it" },
            value = state.needs,
            onChange = onNeedsChange,
            canAdd = canAdd,
        )
        AmountRow(
            label = "Желаемое",
            hint = null,
            value = state.wants,
            onChange = onWantsChange,
            canAdd = canAdd,
        )
        // Без выбранной цели откладывать некуда: цель выбирается прямо здесь,
        // а не на другом экране, — иначе план было бы не собрать.
        if (state.goalLabel == null) {
            FinneyCard {
                Text("Копилка", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Text("На что копим? Выбери цель:", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
                state.goals.forEach { goal -> GoalChoice(goal, onClick = { onSelectGoal(goal.id) }) }
            }
        } else {
            AmountRow(
                label = "Копилка: ${state.goalLabel}",
                hint = null,
                value = state.savings,
                onChange = onSavingsChange,
                canAdd = canAdd,
            )
        }

        Remainder(remainder = state.remainder)

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

/**
 * Одна строка плана: подпись, подсказка и сумма с кнопками.
 *
 * [canAdd] — есть ли ещё нераспределённые деньги. Когда бюджет разобран весь,
 * «плюс» гаснет во всех строках сразу: ребёнок не составит план, который
 * домен всё равно отклонит, и объяснять отказ не придётся.
 */
@Composable
private fun AmountRow(
    label: String,
    hint: String?,
    value: Int,
    onChange: (Int) -> Unit,
    canAdd: Boolean,
) {
    FinneyCard {
        Text(label, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
        }
        AmountStepper(label = label, value = value, onChange = onChange, canAdd = canAdd, step = STEP)
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
 * Остаток. «Останется» — просто число, поэтому панель нейтральная, кремовая:
 * зелёная читалась как похвала. «Не хватает» — предупреждение: голубое и с «!»
 * (ТЗ п. 3.6 — не только цветом).
 */
@Composable
private fun Remainder(remainder: Int) {
    val over = remainder < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(if (over) FinneyBlue else FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (over) WarningBadge(size = 28.dp)
        Text(
            text = if (over) "Не хватает" else "Останется",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
            modifier = Modifier.weight(1f),
        )
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

        FinneyPanel(title = "Уровень ${state.level}") {
            FactRow("Нужное", report.facts.needs, report.plan.needs)
            FactRow("Желаемое", report.facts.wants, report.plan.wants)
            FactRow("Копилка", report.facts.savings, report.plan.savings, progress = true)
        }

        CoinAmount(amount = state.balance)

        // По плану или нет — значком и словами. Уровень ещё идёт, это не итог, поэтому
        // панель нейтральная: персиковое «План» на зелёной подложке читали как «красный
        // текст на зелёном — будто я что-то сделал не так» (ТЗ п. 2.5.9, 3.6).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusCard))
                .background(FinneyCream)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
                .padding(14.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            if (report.onTrack) CheckBadge(size = 32.dp) else WarningBadge(size = 32.dp)
            Text(
                if (report.onTrack) "Идёшь по плану" else "Пока не по плану",
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
            )
        }
    }
}

/**
 * Строка «потрачено из запланированного» и полоса под ней. Траты — полосой без
 * зелёного (потратить больше — не успех), [progress] — копилка: там полоса
 * прогресса от розового к зелёному.
 */
@Composable
private fun FactRow(label: String, fact: Int, planned: Int, progress: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$fact из $planned",
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
            )
        }
        if (progress) FillBar(fact, planned, Modifier.fillMaxWidth()) else SpendBar(fact, planned, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun PlanningContentPreview() {
    FinneyTheme {
        PlanningContent(
            state = BudgetUiState.Planning(
                periodNumber = 1, level = 1, budget = 50, needs = 30, wants = 10, savings = 10,
                needsHint = 30, goalLabel = "Велосипед", rejection = null, isSaving = false,
            ),
            onNeedsChange = {}, onWantsChange = {}, onSavingsChange = {}, onSelectGoal = {}, onConfirm = {}, onBack = {},
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
