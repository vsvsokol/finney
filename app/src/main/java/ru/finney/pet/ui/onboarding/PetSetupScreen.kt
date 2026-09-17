package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.profile.PetNameError
import ru.finney.pet.ui.theme.FinneyTheme

// Черновик от [@lemonke68]: рабочий контракт с PetSetupViewModel. Графику питомца и вёрстку делают [@mitzzi4] и [@zYafALL].

@Composable
fun PetSetupScreen(
    isEditing: Boolean,
    onSaved: () -> Unit,
    viewModel: PetSetupViewModel = viewModel(factory = PetSetupViewModel.factory(isEditing)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PetSetupEvent.Saved -> onSaved()
            }
        }
    }

    PetSetupContent(
        state = state,
        onNameChange = viewModel::setName,
        onBodyColorChange = viewModel::setBodyColor,
        onEyesChange = viewModel::setEyes,
        onSave = viewModel::save,
    )
}

@Composable
private fun PetSetupContent(
    state: PetSetupUiState,
    onNameChange: (String) -> Unit,
    onBodyColorChange: (BodyColor) -> Unit,
    onEyesChange: (EyesVariant) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (state.isEditing) "Настройка питомца" else "Твой питомец", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Имя питомца") },
            isError = state.nameError != null,
            supportingText = {
                when (state.nameError) {
                    PetNameError.BLANK -> Text("Придумай имя")
                    PetNameError.TOO_LONG -> Text("Не длиннее ${state.maxNameLength} букв")
                    null -> Unit
                }
            },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Цвет")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyColor.entries.forEach { color ->
                FilterChip(
                    selected = state.appearance.bodyColor == color,
                    onClick = { onBodyColorChange(color) },
                    label = { Text(color.name) },
                )
            }
        }

        Text("Глаза")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EyesVariant.entries.forEach { eyes ->
                FilterChip(
                    selected = state.appearance.eyes == eyes,
                    onClick = { onEyesChange(eyes) },
                    label = { Text(eyes.name) },
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = !state.isSaving && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isEditing) "Сохранить" else "Начать игру")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PetSetupContentPreview() {
    FinneyTheme {
        PetSetupContent(
            state = PetSetupUiState(isEditing = false, name = "Финни", nameError = PetNameError.TOO_LONG),
            onNameChange = {}, onBodyColorChange = {}, onEyesChange = {}, onSave = {},
        )
    }
}
