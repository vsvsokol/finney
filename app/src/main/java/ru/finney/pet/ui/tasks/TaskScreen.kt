package ru.finney.pet.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.tasks.games.GameScene
import ru.finney.pet.ui.tasks.games.PetSays
import ru.finney.pet.ui.tasks.games.ResultBody
import ru.finney.pet.ui.tasks.games.SceneBody
import ru.finney.pet.ui.tasks.games.ScenePanel
import ru.finney.pet.ui.tasks.games.TaskGame
import ru.finney.pet.ui.tasks.games.backdropFor
import ru.finney.pet.ui.theme.FinneyInk

/** Подпись темы задания — те же три темы, что в ТЗ п. 2.5.8. */
fun TaskTheme.label(): String = when (this) {
    TaskTheme.PLANNING -> "Планирование бюджета"
    TaskTheme.SAVINGS -> "Сбережения"
    TaskTheme.SHOPPING -> "Платежи и покупки"
}

@Composable
fun TaskScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: TaskViewModel = viewModel(key = taskId, factory = TaskViewModel.factory(taskId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        TaskUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        TaskUiState.NotFound -> FinneyScreen {
            OutlinedText("Такой игры нет", style = MaterialTheme.typography.headlineMedium)
            FinneyButton(text = "Назад", onClick = onBack)
        }

        is TaskUiState.Ready -> when (s.phase) {
            TaskPhase.INTRO -> TaskIntro(s, onStart = viewModel::start, onBack = onBack)
            // Крестик и системное «назад» посреди игры закрывают её: до итога ничего не засчитано.
            TaskPhase.PLAY -> key(s.attempt) {
                TaskGame(
                    task = s.task,
                    character = s.character,
                    balance = s.balance,
                    inputError = s.inputError,
                    onInputSeen = viewModel::inputSeen,
                    onClose = onBack,
                    onSubmit = viewModel::submit,
                )
            }
            TaskPhase.RESULT -> TaskResultScene(s, onReplay = viewModel::replay, onDone = onBack)
        }
    }
}

/** Вступление в той же сцене, что игра: питомец рассказывает, что делать. */
@Composable
private fun TaskIntro(state: TaskUiState.Ready, onStart: () -> Unit, onBack: () -> Unit) {
    GameScene(backdrop = backdropFor(state.task), onClose = onBack, money = state.balance) {
        // Питомец стоит внизу, на полу сцены, а «Играть» прижата к краю —
        // раньше всё собиралось наверху, и под кнопкой оставалось полэкрана пустоты.
        SceneBody(
            horizontalAlignment = Alignment.CenterHorizontally,
            bottom = { if (state.available) FinneyButton(text = "Играть", onClick = onStart) },
        ) {
            OutlinedText(state.task.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            ScenePanel(title = "Что делать", modifier = Modifier.fillMaxWidth()) {
                Text(state.task.intro, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
                Text(state.task.theme.label(), style = MaterialTheme.typography.labelLarge, color = FinneyInk.copy(alpha = 0.75f))
            }
            Spacer(Modifier.weight(1f))
            PetSays(
                state.character,
                if (state.completed) {
                    "Уже получалось! Сыграем ещё — просто так?"
                } else {
                    "Получится — дам ${state.reward.success} монет. А за первую попытку — ${state.reward.fail}."
                },
                petSize = 160.dp,
            )
            if (!state.available) {
                ScenePanel(title = null, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.lockedUntilLevel != null) {
                            "Эта игра откроется на уровне ${state.lockedUntilLevel}. Уровень растёт за план, копилку и задания."
                        } else {
                            "Эта игра откроется в следующих периодах."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = FinneyInk,
                    )
                }
            }
        }
    }
}

/**
 * Итог в сцене игры: что получилось по пунктам, объяснение при любом исходе и
 * награда (ТЗ п. 2.5.8, 2.5.9). Без стыда: «почти», а не «проиграл».
 */
@Composable
private fun TaskResultScene(state: TaskUiState.Ready, onReplay: () -> Unit, onDone: () -> Unit) {
    val result = state.result ?: return
    val success = result.outcome == TaskOutcome.SUCCESS
    GameScene(backdrop = backdropFor(state.task, finished = true), onClose = onDone, money = state.balance) {
        // Разбор бывает длиннее экрана — прокручивается он, а «Ещё раз» и «Дальше»
        // всегда видны: раньше они уезжали вниз, и ребёнок не знал, как выйти.
        SceneBody(
            bottom = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinneyButton(text = "Ещё раз", onClick = onReplay, modifier = Modifier.weight(1f))
                    FinneyButton(text = "Дальше", onClick = onDone, modifier = Modifier.weight(1f))
                }
            },
        ) {
            ScenePanel(title = if (success) "Готово!" else "Почти!", modifier = Modifier.fillMaxWidth()) {
                ResultBody(state.task, result.details, result.input)
                Text(
                    if (success) state.task.explainOk else state.task.explainFail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    if (result.reward > 0) {
                        Text("награда", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                        Coin(size = 26.dp)
                        OutlinedText("+${result.reward}", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text("награда за эту игру уже получена", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PetSays(state.character, if (success) "Получилось! Ура!" else "Ничего страшного — попробуем ещё раз?", petSize = 120.dp)
        }
    }
}
