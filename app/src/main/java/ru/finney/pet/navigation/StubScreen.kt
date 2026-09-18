package ru.finney.pet.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk

/**
 * Временный экран, чтобы граф был проходим целиком. [@zYafALL] заменяет вызов в FinneyNavHost
 * на настоящий экран; когда заглушек не останется — файл удалить.
 */
@Composable
internal fun StubScreen(title: String, vararg actions: Pair<String, () -> Unit>) {
    FinneyScreen(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedText(title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Экран в разработке",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
            textAlign = TextAlign.Center,
            modifier = Modifier,
        )
        actions.forEach { (label, onClick) ->
            FinneyButton(text = label, onClick = onClick)
        }
    }
}
