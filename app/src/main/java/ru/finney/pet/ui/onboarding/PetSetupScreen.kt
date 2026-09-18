package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.profile.PetNameError
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetPose
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.currentPose
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.theme.FinneyTheme

// Черновик от [@lemonke68]: рабочий контракт с PetSetupViewModel. Графику питомца и вёрстку делают [@mitzzi4] и [@zYafALL].

/**
 * Подписи питомцев для игрока. В домене они названы по автору рисунка
 * (design/exports/pet) — ребёнку такое имя ничего не говорит. Здесь заглушки:
 * настоящие имена придумывает [@vsvsokol] вместе с текстами знакомства.
 */
private val CharacterLabels = mapOf(
    PetCharacter.PUSHISTIK to "Пушистик",
    PetCharacter.ROGATIK to "Рогатик",
)

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
        onCharacterChange = viewModel::setCharacter,
        onBodyColorChange = viewModel::setBodyColor,
        onEyesChange = viewModel::setEyes,
        onSave = viewModel::save,
    )
}

@Composable
private fun PetSetupContent(
    state: PetSetupUiState,
    onNameChange: (String) -> Unit,
    onCharacterChange: (PetCharacter) -> Unit,
    onBodyColorChange: (BodyColor) -> Unit,
    onEyesChange: (EyesVariant) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (state.isEditing) "Настройка питомца" else "Твой питомец",
            style = MaterialTheme.typography.headlineMedium,
        )

        Text("Кто это будет")
        CharacterPicker(
            selected = state.appearance.character,
            enabled = !state.isLoading,
            onSelect = onCharacterChange,
        )

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

/**
 * Выбор питомца: на карточках сами питомцы, а не подписи — ребёнок может ещё не
 * читать. Выбранный дышит и подпрыгивает в ответ на нажатие, остальные стоят:
 * так видно, кто выбран, даже мимо рамки.
 */
@Composable
private fun CharacterPicker(
    selected: PetCharacter,
    enabled: Boolean,
    onSelect: (PetCharacter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        PetCharacter.entries.forEach { character ->
            val isSelected = character == selected
            val animation = rememberPetAnimation()
            val pose = if (isSelected) animation.currentPose() else PetPose()

            OutlinedCard(
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            onSelect(character)
                            animation.playJoy()
                        },
                    ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    PetView(
                        character = character,
                        mood = PetMood.HAPPY,
                        pose = pose,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = CharacterLabels.getValue(character),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PetSetupContentPreview() {
    FinneyTheme {
        PetSetupContent(
            state = PetSetupUiState(isEditing = false, name = "Финни", nameError = PetNameError.TOO_LONG),
            onNameChange = {}, onCharacterChange = {}, onBodyColorChange = {},
            onEyesChange = {}, onSave = {},
        )
    }
}
