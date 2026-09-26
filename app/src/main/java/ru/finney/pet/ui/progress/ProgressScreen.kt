package ru.finney.pet.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk

private val StageNames = mapOf(1 to "малыш", 2 to "подросток", 3 to "взрослый")

@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenPeriodResult: (periodNumber: Int) -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        ProgressUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        is ProgressUiState.Ready -> FinneyScreen(
            scrollable = true,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            bottom = { FinneyButton(text = "Назад", onClick = onBack) },
        ) {
            OutlinedText("Прогресс", style = MaterialTheme.typography.headlineLarge)

            FinneyPanel(title = "Финни растёт") {
                Line("Уровень", "${s.level} из 9")
                Line("Стадия", StageNames[s.stage] ?: s.stage.toString())
                Line("Очки развития", s.points.toString())
                Line("Целей достигнуто", s.goalsCompleted.toString())
            }

            FinneyPanel(title = "Цель") {
                val goal = s.goal
                if (goal == null) {
                    Body("Цель пока не выбрана — выбери её в копилке.")
                } else {
                    Line(goal.goal.label, "${goal.saved} из ${goal.goal.price}")
                    Line("Осталось собрать", goal.remaining.toString())
                }
            }

            s.lastClosedPeriod?.let { number ->
                FinneyButton(text = "Итоги периода $number", onClick = { onOpenPeriodResult(number) })
            }

            FinneyPanel(title = "Мини-игры") {
                if (s.tasks.isEmpty()) {
                    Body("Здесь появятся мини-игры, которые ты попробуешь.")
                }
                s.tasks.forEach { task ->
                    FinneyCard(accent = if (task.passed) FinneyGreen else null) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                        Body("Тема: ${task.theme}")
                        // Исход словом, не только цветом карточки (ТЗ п. 3.6).
                        Body(
                            if (task.passed) {
                                "Пройдено, попыток: ${task.attempts}"
                            } else {
                                "Пока не вышло, попыток: ${task.attempts}. Можно попробовать ещё"
                            },
                        )
                    }
                }
            }

            FinneyPanel(title = "История") {
                s.history.forEach { period ->
                    Text("Период ${period.number}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                    period.rows.forEach { HistoryLine(it) }
                }
            }

            FinneyButton(text = "Справочник", onClick = onOpenGlossary)
        }
    }
}

@Composable
private fun HistoryLine(row: HistoryRow) {
    // Движение по копилке показываем отдельно: деньги с баланса ушли, но не потрачены.
    val amount = when {
        row.balanceDelta != 0 -> signed(row.balanceDelta)
        row.savingsDelta != 0 -> "копилка ${signed(row.savingsDelta)}"
        else -> "0"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "${row.label}: $amount" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(row.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(amount, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

private fun signed(value: Int) = if (value > 0) "+$value" else value.toString()

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
