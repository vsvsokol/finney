package ru.finney.pet.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.finney.pet.ui.theme.FinneyTheme

// Заглушка, чтобы у графа навигации был стартовый экран. Верстает [@zYafALL] по макетам.
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Финни")
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FinneyTheme { HomeScreen() }
}
