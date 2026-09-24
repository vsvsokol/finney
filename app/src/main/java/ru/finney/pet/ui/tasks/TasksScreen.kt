package ru.finney.pet.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.tasks.games.ItemPicture
import ru.finney.pet.ui.tasks.games.taskIcon
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyYellow

/** Карточка игры: задание, пройдено ли и открыто ли. */
data class TaskRow(val task: TaskDefinition, val done: Boolean, val available: Boolean)

/** Все мини-игры в порядке tasks.json. */
class TasksViewModel(session: Session, private val game: Game, private val content: GameContent) : ViewModel() {

    val rows: StateFlow<List<TaskRow>?> = session.activeGame
        .filterNotNull()
        .map { saved ->
            val state = saved.state
            val done = state.attempts.filter { it.outcome == TaskOutcome.SUCCESS }.map { it.taskId }.toSet()
            content.tasks.map { TaskRow(it, it.id in done, game.isTaskAvailable(state, it)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { TasksViewModel(it.session, it.game, it.content) } }
        }
    }
}

@Composable
fun TasksScreen(
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val list = rows
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FinneyInk) }
        return
    }

    FinneyScreen(scrollable = false) {
        OutlinedText("Мини-игры", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Пройдено ${list.count { it.done }} из ${list.size}",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            list.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { row -> GameCard(row, Modifier.weight(1f)) { onOpenTask(row.task.id) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        FinneyButton(text = "Назад", onClick = onBack)
    }
}

/** Карточка: картинка из самой игры, название, тема. Пройденное — жёлтая заливка и «✓ пройдено» (ТЗ п. 3.6). */
@Composable
private fun GameCard(row: TaskRow, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (row.done) FinneyYellow else Color.White)
            .border(4.dp, FinneyInk, shape)
            .clickable(role = Role.Button, onClickLabel = "Играть: ${row.task.title}", onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FinneyCream).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            taskIcon(row.task)?.let { ItemPicture(it, row.task.title, 72.dp) }
        }
        OutlinedText(row.task.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(row.task.theme.label(), style = MaterialTheme.typography.labelMedium, color = FinneyInk, textAlign = TextAlign.Center)
        Text(
            text = when {
                !row.available -> "откроется позже"
                row.done -> "✓ пройдено"
                else -> "новая"
            },
            style = MaterialTheme.typography.labelLarge,
            color = FinneyInk,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (row.done) FinneyGreen else FinneyCream)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
