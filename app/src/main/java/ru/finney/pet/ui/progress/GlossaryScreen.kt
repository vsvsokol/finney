package ru.finney.pet.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.finney.pet.appContainer
import ru.finney.pet.domain.model.GlossaryTerm
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk

/** Термины из glossary.json — учебный контент, не код (ТЗ п. 2.5.11, 3.2). */
class GlossaryViewModel(val terms: List<GlossaryTerm>) : ViewModel() {
    companion object {
        val Factory = viewModelFactory {
            initializer { GlossaryViewModel(appContainer().content.glossary) }
        }
    }
}

@Composable
fun GlossaryScreen(
    onBack: () -> Unit,
    viewModel: GlossaryViewModel = viewModel(factory = GlossaryViewModel.Factory),
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        bottom = { FinneyButton(text = "Назад", onClick = onBack) },
    ) {
        OutlinedText("Справочник", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Короткие объяснения слов, которые встречаются в игре.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )
        viewModel.terms.forEach { term ->
            FinneyCard {
                Text(term.term, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Text(term.text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            }
        }
    }
}
