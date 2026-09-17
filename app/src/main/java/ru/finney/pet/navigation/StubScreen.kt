package ru.finney.pet.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Временный экран, чтобы граф был проходим целиком. [@zYafALL] заменяет вызов в FinneyNavHost
 * на настоящий экран; когда заглушек не останется — файл удалить.
 */
@Composable
internal fun StubScreen(title: String, vararg actions: Pair<String, () -> Unit>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Экран в разработке", style = MaterialTheme.typography.bodyLarge)
        actions.forEach { (label, onClick) ->
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}
