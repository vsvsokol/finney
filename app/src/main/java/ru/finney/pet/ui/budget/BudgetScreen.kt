package ru.finney.pet.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.game.PlanReport
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.ui.components.AMOUNT_STEP
import ru.finney.pet.ui.components.AmountStepper
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FeedbackSound
import ru.finney.pet.ui.components.FillBar
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.SpendBar
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.RadiusCard
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.theme.FinneyYellow
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
                Text("На что копим? Выбери цель:", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                state.goals.forEach { goal ->
                    FinneyButton(text = "${goal.label} · ${goal.price}", onClick = { onSelectGoal(goal.id) })
                }
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

        // Почему кнопка погашена — словами, а не догадкой.
        if (!state.allDirections && state.remainder >= 0) {
            Text(
                text = "Положи хоть немного в каждую часть: нужное, желаемое и копилку.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
                textAlign = TextAlign.Center,
            )
        }

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

/** Остаток. Перерасход показан и словом, и знаком — не только цветом (ТЗ п. 3.6). */
@Composable
private fun Remainder(remainder: Int) {
    val over = remainder < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(if (over) FinneyPink else FinneyGreen)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onClose = onBack,
    ) {
        OutlinedText("План и факт", style = MaterialTheme.typography.headlineLarge)

        FinneyPanel(title = "Уровень ${state.level}") {
            FactRow("Нужное", report.facts.needs, report.plan.needs)
            FactRow("Желаемое", report.facts.wants, report.plan.wants)
            FactRow("Копилка", report.facts.savings, report.plan.savings, progress = true)
        }

        CoinAmount(amount = state.balance)

        // По плану или нет — знаком ✓ / ↺ и коротким словом: одного знака мало,
        // ребёнок должен понять, про что он (ТЗ п. 2.5.9, 3.6).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusCard))
                .background(if (report.onTrack) FinneyGreen else FinneyYellow)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCard))
                .padding(14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (report.onTrack) "Ты идёшь по плану" else "Пока не по плану"
                },
        ) {
            OutlinedText(if (report.onTrack) "✓" else "↺", style = MaterialTheme.typography.headlineMedium)
            OutlinedText("План", style = MaterialTheme.typography.titleLarge)
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
