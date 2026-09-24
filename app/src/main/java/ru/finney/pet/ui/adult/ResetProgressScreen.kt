package ru.finney.pet.ui.adult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Session
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme

/**
 * Сброс прогресса из раздела взрослого: питомец тот же, игра — с первого периода.
 * Для демонстраций: потыкали, вернулись к началу (ТЗ п. 2.5.13).
 */
class ResetProgressViewModel(private val session: Session) : ViewModel() {

    /** Имя питомца для текста подтверждения; null — профиль ещё грузится. */
    val petName: StateFlow<String?> = session.activeGame
        .map { it?.profile?.petName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private var running = false

    fun reset() {
        if (running) return
        running = true
        viewModelScope.launch {
            session.resetProgress()
            _done.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ResetProgressViewModel(appContainer().session) }
        }
    }
}

@Composable
fun ResetProgressScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ResetProgressViewModel = viewModel(factory = ResetProgressViewModel.Factory),
) {
    val petName by viewModel.petName.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }

    val name = petName
    if (name == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FinneyInk) }
        return
    }
    ResetProgressContent(petName = name, onReset = viewModel::reset, onCancel = onCancel)
}

/** Кнопка сброса — первой, отмена — второй: на ней «назад» по привычке и остановится палец. */
@Composable
private fun ResetProgressContent(petName: String, onReset: () -> Unit, onCancel: () -> Unit) {
    FinneyScreen(verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        OutlinedText("Начать заново?", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "$petName останется с тем же именем и внешностью. Деньги, копилка, цели, покупки, " +
                "пройденные задания и уровень вернутся к началу игры.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Отменить сброс будет нельзя.",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
            textAlign = TextAlign.Center,
        )
        FinneyButton(text = "Сбросить прогресс", onClick = onReset)
        FinneyButton(text = "Отмена", onClick = onCancel)
    }
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun ResetProgressPreview() {
    FinneyTheme { ResetProgressContent(petName = "Финни", onReset = {}, onCancel = {}) }
}
