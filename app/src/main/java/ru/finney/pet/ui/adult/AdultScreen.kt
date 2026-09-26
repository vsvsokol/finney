package ru.finney.pet.ui.adult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.ui.components.FeedbackDialog
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.FinneyTextField
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk

private val StageNames = mapOf(1 to "малыш", 2 to "подросток", 3 to "взрослый")

/** Чему учит приложение — компетенции базового уровня из Единой рамки (ТЗ раздел 1). */
private val AppGoals = listOf(
    "Понимать, что расходы не должны превышать доходов",
    "Отличать нужное от желаемого",
    "Планировать покупки в пределах бюджета",
    "Ставить цель и регулярно откладывать",
    "Оценивать свои решения и их последствия",
    "Пользоваться приложением вместе со взрослым",
)

@Composable
fun AdultScreen(
    onBack: () -> Unit,
    onEditPet: () -> Unit,
    onResetProgress: () -> Unit,
    onProfileDeleted: (hasProfile: Boolean) -> Unit,
    viewModel: AdultViewModel = viewModel(factory = AdultViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AdultEvent.ProfileDeleted -> onProfileDeleted(event.hasProfile)
            }
        }
    }

    when (val s = state) {
        AdultUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }
        is AdultUiState.Locked -> GateContent(gate = s.gate, onAnswer = viewModel::answer, onBack = onBack)
        is AdultUiState.Ready -> AdultContent(
            state = s,
            onBack = onBack,
            onEditPet = onEditPet,
            onResetProgress = onResetProgress,
            onAddBonus = viewModel::addBonus,
            onDelete = viewModel::deleteProfile,
            onDismissRejection = viewModel::dismissRejection,
        )
    }
}

@Composable
private fun GateContent(gate: AdultGate, onAnswer: (String) -> Unit, onBack: () -> Unit) {
    // Поле сбрасывается вместе с примером: старый ответ к новому не подходит.
    var answer by rememberSaveable(gate) { mutableStateOf("") }
    FinneyScreen(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = { FinneyButton(text = "Назад", onClick = onBack) },
    ) {
        OutlinedText("Для взрослых", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Этот раздел для родителей. Реши пример, чтобы войти.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )
        OutlinedText("${gate.a} × ${gate.b} = ?", style = MaterialTheme.typography.headlineMedium)
        FinneyTextField(
            value = answer,
            onValueChange = { value -> answer = value.filter(Char::isDigit).take(4) },
            label = "Ответ",
            isError = gate.wrongAnswer,
            supportingText = if (gate.wrongAnswer) "Ответ не подошёл — вот новый пример" else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FinneyButton(text = "Войти", onClick = { onAnswer(answer) }, enabled = answer.isNotEmpty())
    }
}

@Composable
private fun AdultContent(
    state: AdultUiState.Ready,
    onBack: () -> Unit,
    onEditPet: () -> Unit,
    onResetProgress: () -> Unit,
    onAddBonus: () -> Unit,
    onDelete: () -> Unit,
    onDismissRejection: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = { FinneyButton(text = "Назад", onClick = onBack) },
    ) {
        OutlinedText("Для взрослых", style = MaterialTheme.typography.headlineLarge)
        if (state.isDemo) Body("Открыт тестовый профиль демо-режима: все мини-игры доступны сразу.")

        FinneyPanel(title = "Чему учит") {
            AppGoals.forEach { Body("— $it") }
            Body("Деньги в игре условные: платежей, рекламы и сбора данных нет.")
        }

        FinneyPanel(title = "Пройденные темы") {
            state.topics.forEach { Line(it.title, "${it.passed} из ${it.total}") }
        }

        FinneyPanel(title = "Прогресс") {
            Line("Питомец", state.petName)
            Line("Уровень", "${state.level} из 9")
            Line("Стадия", StageNames[state.stage] ?: state.stage.toString())
            Line("Периодов пройдено", state.closedPeriods.toString())
            Line("В копилке", state.totalSavings.toString())
            Line("Целей достигнуто", state.goalsCompleted.toString())
            Body("Уровень и очки только растут: ошибки ребёнка их не отнимают.")
        }

        FinneyPanel(title = "Бонус") {
            Body(
                "Можно поощрить ребёнка игровыми монетками, например за помощь дома. " +
                    "В этом периоде осталось ${state.bonusLeft}.",
            )
            FinneyButton(
                text = "Добавить ${state.bonusStep}",
                onClick = onAddBonus,
                enabled = state.bonusLeft >= state.bonusStep,
            )
        }

        FinneyPanel(title = "Профиль") {
            FinneyButton(text = "Имя и внешность", onClick = onEditPet)
            FinneyButton(text = "Начать игру заново", onClick = onResetProgress)
            FinneyButton(text = "Удалить профиль", onClick = { confirmDelete = true })
        }
    }

    FeedbackDialog(feedback = null, rejection = state.rejection, onDismiss = onDismissRejection)

    if (confirmDelete) {
        Dialog(onDismissRequest = { confirmDelete = false }) {
            FinneyPanel(title = "Удалить профиль?") {
                Body(
                    "${state.petName}, деньги, копилка, задания и весь прогресс будут удалены с телефона. " +
                        "Отменить удаление будет нельзя.",
                )
                FinneyButton(text = "Удалить", onClick = { confirmDelete = false; onDelete() })
                FinneyButton(text = "Отмена", onClick = { confirmDelete = false })
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
}
