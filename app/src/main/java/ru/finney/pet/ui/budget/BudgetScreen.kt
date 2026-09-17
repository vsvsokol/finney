package ru.finney.pet.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.ui.theme.FinneyTheme

// Черновик от [@lemonke68]: рабочий контракт с BudgetViewModel. Вёрстку по макетам делает [@zYafALL],
// тексты отказов — [@vsvsokol] (feedback.json).

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = state) {
            BudgetUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is BudgetUiState.Planning -> PlanningContent(
                state = s,
                onNeedsChange = viewModel::setNeeds,
                onWantsChange = viewModel::setWants,
                onSavingsChange = viewModel::setSavings,
                onConfirm = viewModel::confirm,
            )
            is BudgetUiState.Active -> ActiveContent(s)
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun PlanningContent(
    state: BudgetUiState.Planning,
    onNeedsChange: (Int) -> Unit,
    onWantsChange: (Int) -> Unit,
    onSavingsChange: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    Text("План на период ${state.periodNumber}", style = MaterialTheme.typography.headlineMedium)
    Text("Можно распределить: ${state.budget}")
    state.needsHint?.let { Text("На нужное нужно хотя бы $it") }
    AmountField("Нужное", state.needs, onNeedsChange)
    AmountField("Желаемое", state.wants, onWantsChange)
    AmountField(state.goalLabel?.let { "Отложить на «$it»" } ?: "Отложить", state.savings, onSavingsChange)
    Text("Остаток: ${state.remainder}")
    state.rejection?.let { Text("Не получилось: $it", color = MaterialTheme.colorScheme.error) }
    Button(onClick = onConfirm, enabled = state.canConfirm, modifier = Modifier.fillMaxWidth()) {
        Text("Подтвердить план")
    }
}

@Composable
private fun ActiveContent(state: BudgetUiState.Active) {
    val report = state.report
    Text("План и факт, период ${state.periodNumber}", style = MaterialTheme.typography.headlineMedium)
    Text("Нужное: ${report.facts.needs} из ${report.plan.needs}")
    Text("Желаемое: ${report.facts.wants} из ${report.plan.wants}")
    Text("Отложено: ${report.facts.savings} из ${report.plan.savings}")
    Text("Осталось денег: ${state.balance}")
    Text(if (report.onTrack) "Идёшь по плану" else "Пока не по плану")
}

@Composable
private fun AmountField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> onValueChange(text.filter(Char::isDigit).take(6).toIntOrNull() ?: 0) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlanningContentPreview() {
    FinneyTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanningContent(
                state = BudgetUiState.Planning(
                    periodNumber = 1, budget = 50, needs = 30, wants = 10, savings = 10,
                    needsHint = 30, goalLabel = "Велосипед", rejection = null, isSaving = false,
                ),
                onNeedsChange = {}, onWantsChange = {}, onSavingsChange = {}, onConfirm = {},
            )
        }
    }
}
