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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.game.PlanReport
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Экран плана (ТЗ п. 2.5.5). Суммы набираются кнопками «плюс» и «минус», а не
// с клавиатуры: цифровое поле для 7-летнего — барьер, а шаг в 5 финок заодно
// не даёт составить план из копеек.

/** Шаг изменения суммы. Совпадает с шагом родительского бонуса из economy.json. */
private const val STEP = 5

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedText("План", style = MaterialTheme.typography.headlineLarge)

        Text(
            text = "Период ${state.periodNumber}. Реши, на что потратить деньги.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
            textAlign = TextAlign.Center,
        )

        // Сколько можно распределить — крупно и сверху: это главное число экрана.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Есть:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            CoinAmount(amount = state.budget)
        }

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
        // Без выбранной цели откладывать некуда: план с копилкой домен не примет,
        // поэтому «+» здесь недоступен, а подпись объясняет почему.
        AmountRow(
            label = state.goalLabel?.let { "Копилка: $it" } ?: "Копилка",
            hint = if (state.goalLabel == null) "сначала выбери цель" else null,
            value = state.savings,
            onChange = onSavingsChange,
            canAdd = canAdd && state.goalLabel != null,
        )

        Remainder(remainder = state.remainder)

        state.rejection?.let { RejectionNote(it) }

        FinneyButton(
            text = "Подтвердить план",
            onClick = onConfirm,
            enabled = state.canConfirm,
        )
        FinneyButton(text = "Назад", onClick = onBack)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FinneySand)
            .border(2.dp, FinneyInk, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FinneyIconButton(
                onClick = { onChange((value - STEP).coerceAtLeast(0)) },
                contentDescription = "$label: убавить",
                size = 56.dp,
                enabled = value > 0,
            ) {
                OutlinedText("−", style = MaterialTheme.typography.headlineMedium)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CoinAmount(amount = value)
            }

            FinneyIconButton(
                onClick = { onChange(value + STEP) },
                contentDescription = "$label: добавить",
                size = 56.dp,
                enabled = canAdd,
            ) {
                OutlinedText("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

/** Остаток. Перерасход показан и словом, и знаком — не только цветом (ТЗ п. 3.6). */
@Composable
private fun Remainder(remainder: Int) {
    val over = remainder < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (over) FinneyPink else FinneyGreen)
            .border(3.dp, FinneyInk, RoundedCornerShape(20.dp))
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

/** Отказ домена, переведённый на детский язык. Тексты потом заменит feedback.json [@vsvsokol]. */
@Composable
private fun RejectionNote(rejection: Rejection) {
    val text = when (rejection) {
        is Rejection.PlanExceedsBudget ->
            "Ты разделил ${rejection.planned}, а есть только ${rejection.budget}. Убавь что-нибудь."
        is Rejection.InsufficientFunds -> "Не хватает ${rejection.shortage} финок."
        Rejection.InvalidAmount -> "Так не получится: суммы не могут быть меньше нуля."
        Rejection.PlanAlreadyConfirmed -> "План на этот период уже готов."
        Rejection.NoActiveGoal -> "Выбери цель — тогда будет куда откладывать."
        else -> "Так пока нельзя."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = FinneyInk,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinneyPink)
            .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
            .padding(12.dp),
    )
}

@Composable
private fun ActiveContent(state: BudgetUiState.Active, onBack: () -> Unit) {
    val report = state.report
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedText("План и факт", style = MaterialTheme.typography.headlineLarge)

        FinneyPanel(title = "Период ${state.periodNumber}") {
            FactRow("Нужное", report.facts.needs, report.plan.needs)
            FactRow("Желаемое", report.facts.wants, report.plan.wants)
            FactRow("Копилка", report.facts.savings, report.plan.savings)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Осталось:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            CoinAmount(amount = state.balance)
        }

        // «По плану» подкреплено словами, значок здесь не нужен: текст и есть признак.
        Text(
            text = if (report.onTrack) "Ты идёшь по плану!" else "Пока не по плану",
            style = MaterialTheme.typography.titleLarge,
            color = FinneyInk,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (report.onTrack) FinneyGreen else FinneyYellow)
                .border(3.dp, FinneyInk, RoundedCornerShape(20.dp))
                .padding(14.dp),
            textAlign = TextAlign.Center,
        )

        FinneyButton(text = "Назад", onClick = onBack)
    }
}

/** Строка «потрачено из запланированного». */
@Composable
private fun FactRow(label: String, fact: Int, planned: Int) {
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
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun PlanningContentPreview() {
    FinneyTheme {
        PlanningContent(
            state = BudgetUiState.Planning(
                periodNumber = 1, budget = 50, needs = 30, wants = 10, savings = 10,
                needsHint = 30, goalLabel = "Велосипед", rejection = null, isSaving = false,
            ),
            onNeedsChange = {}, onWantsChange = {}, onSavingsChange = {}, onConfirm = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun ActiveContentPreview() {
    FinneyTheme {
        ActiveContent(
            state = BudgetUiState.Active(
                periodNumber = 1,
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
