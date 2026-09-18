package ru.finney.pet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.StatBar
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

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
    FinneyScreen(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ---------- Верх: деньги, уровень, подсказка и меню ----------
        // Подсказка и раздел взрослого — кружками наверху, а не кнопками внизу:
        // высоты экрана 640 dp иначе не хватает, и питомца прижимает.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CoinAmount(amount = state.balance)
            Spacer(Modifier.weight(1f))
            LevelBadge(level = state.level, size = 56.dp)
            FinneyIconButton(
                onClick = onOpenHelp,
                contentDescription = "Подсказка",
                size = 56.dp,
            ) {
                OutlinedText("?", style = MaterialTheme.typography.headlineMedium)
            }
            FinneyIconButton(
                onClick = onOpenAdult,
                contentDescription = "Для взрослых",
                size = 56.dp,
            ) {
                OutlinedText("≡", style = MaterialTheme.typography.headlineMedium)
            }
        }

        Text(
            text = "${state.petName} · период ${state.periodNumber}",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
        )

        // ---------- Середина: питомец и шкалы ----------
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val animation = rememberPetAnimation()
            PetView(
                character = state.appearance.character,
                mood = state.emotion.toMood(),
                pose = rememberPoseProvider(animation),
                modifier = Modifier.weight(1f),
            )

            // Три шкалы рядом одинаковой высоты: так их видно как один блок
            // состояния питомца, а не как три отдельных значка.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBar(label = "сытость", value = state.stats.satiety, height = 130.dp, width = 26.dp)
                StatBar(label = "чистота", value = state.stats.hygiene, height = 130.dp, width = 26.dp)
                StatBar(label = "радость", value = state.stats.mood, height = 130.dp, width = 26.dp)
            }
        }

        // Цель и подсказка — то, ради чего копят (ТЗ п. 2.5.3).
        state.goal?.let { goal ->
            InfoStrip(text = "${goal.goal.label}: ${goal.saved} из ${goal.goal.price}")
        }
        state.needsHint?.takeIf { it > 0 }?.let {
            InfoStrip(text = "На нужное понадобится $it")
        }

        // Активное задание — отдельной кнопкой, чтобы его было видно сразу.
        state.nextTask?.let { task ->
            FinneyButton(text = "Задание: ${task.title}", onClick = { onOpenTask(task.id) })
        }

        // ---------- Низ: действия ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionButton(label = "План", onClick = onOpenBudget)
            ActionButton(label = "Магазин", onClick = onOpenShop)
            ActionButton(label = "Копилка", onClick = onOpenGoals)
            ActionButton(label = "Задания", onClick = onOpenTasks)
            ActionButton(label = "Прогресс", onClick = onOpenProgress)
        }

        if (state.canClosePeriod) {
            FinneyButton(text = "Завершить период", onClick = onClosePeriod)
        }
        // Временный вход в черновик анимаций. Удалить вместе с PetLabScreen.
        FinneyButton(text = "Анимации питомца", onClick = onOpenPetLab)
    }
}

/**
 * Кнопка нижнего ряда: кружок и подпись под ним. Подпись обязательна —
 * иконок в ресурсах пока нет, да и текст понятнее ребёнку, чем значок без слов.
 */
@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FinneyIconButton(onClick = onClick, contentDescription = label, size = 56.dp) {
            OutlinedText(
                text = label.take(1),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FinneyInk,
            textAlign = TextAlign.Center,
        )
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
            .clip(RoundedCornerShape(16.dp))
            .background(FinneySand)
            .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
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

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
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
            onOpenProgress = {}, onOpenAdult = {}, onOpenHelp = {}, onOpenPetLab = {},
            onClosePeriod = {},
        )
    }
}
