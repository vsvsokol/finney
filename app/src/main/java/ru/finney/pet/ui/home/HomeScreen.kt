package ru.finney.pet.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.pet.Emotion
import ru.finney.pet.ui.theme.FinneyTheme

// Черновик от [@lemonke68]: показывает, как экран берёт состояние и события из ViewModel.
// Вёрстку по макетам [@zYafALL] делает прямо здесь, контракт — HomeUiState и лямбды навигации.

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
            CircularProgressIndicator()
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
    onClosePeriod: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.petName, style = MaterialTheme.typography.headlineMedium)
        Text("Период ${state.periodNumber} · уровень ${state.level} · стадия ${state.stage}")
        Text("Настроение: ${state.emotion}")
        Text("Сытость ${state.stats.satiety} · чистота ${state.stats.hygiene} · настроение ${state.stats.mood}")
        Text("Баланс: ${state.balance} · в копилке: ${state.totalSavings}")
        Text(state.goal?.let { "Цель: ${it.goal.label} — ${it.saved} из ${it.goal.price}" } ?: "Цель не выбрана")
        state.needsHint?.takeIf { it > 0 }?.let { Text("На нужное понадобится: $it") }

        val action = Modifier.fillMaxWidth()
        state.nextTask?.let { task ->
            Button(onClick = { onOpenTask(task.id) }, modifier = action) { Text("Задание: ${task.title}") }
        }
        OutlinedButton(onClick = onOpenBudget, modifier = action) { Text("План") }
        OutlinedButton(onClick = onOpenShop, modifier = action) { Text("Магазин") }
        OutlinedButton(onClick = onOpenGoals, modifier = action) { Text("Копилка") }
        OutlinedButton(onClick = onOpenTasks, modifier = action) { Text("Задания") }
        OutlinedButton(onClick = onOpenProgress, modifier = action) { Text("Прогресс") }
        OutlinedButton(onClick = onOpenHelp, modifier = action) { Text("Подсказка") }
        OutlinedButton(onClick = onOpenAdult, modifier = action) { Text("Для взрослых") }
        // Временный вход в черновик анимаций. Удалить вместе с PetLabScreen.
        OutlinedButton(onClick = onOpenPetLab, modifier = action) { Text("Анимации питомца") }
        Button(onClick = onClosePeriod, enabled = state.canClosePeriod, modifier = action) {
            Text("Завершить период")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    FinneyTheme {
        HomeContent(
            state = HomeUiState.Ready(
                petName = "Финни",
                appearance = PetAppearance(BodyColor.A, EyesVariant.ROUND),
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
            onOpenProgress = {}, onOpenAdult = {}, onOpenHelp = {}, onOpenPetLab = {},
            onClosePeriod = {},
        )
    }
}
