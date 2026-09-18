package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.profile.PetNameError
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetPose
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.currentPose
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

/**
 * Подписи питомцев для игрока. В домене они названы по автору рисунка
 * (design/exports/pet) — ребёнку такое имя ничего не говорит. Здесь заглушки:
 * настоящие имена придумывает [@vsvsokol] вместе с текстами знакомства.
 */
private val CharacterLabels = mapOf(
    PetCharacter.PUSHISTIK to "Пушистик",
    PetCharacter.ROGATIK to "Рогатик",
)

/** Цвета тела для плашек выбора. Пока графики нет, вариант показывается кружком. */
private val BodyColorSwatches = mapOf(
    BodyColor.A to FinneyPeach,
    BodyColor.B to FinneyBlue,
    BodyColor.C to FinneyGreen,
)

/** Названия вариантов глаз: ребёнок выбирает по слову, а не по букве enum. */
private val EyesLabels = mapOf(
    EyesVariant.ROUND to "круглые",
    EyesVariant.OVAL to "овальные",
    EyesVariant.SLY to "хитрые",
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
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OutlinedText(
            text = if (state.isEditing) "Настройка" else "Твой питомец",
            style = MaterialTheme.typography.headlineLarge,
        )

        SectionTitle("Кто это будет")
        CharacterPicker(
            selected = state.appearance.character,
            enabled = !state.isLoading,
            onSelect = onCharacterChange,
        )

        SectionTitle("Как его зовут")
        NameField(
            name = state.name,
            error = state.nameError,
            maxLength = state.maxNameLength,
            enabled = !state.isLoading,
            onNameChange = onNameChange,
        )

        SectionTitle("Цвет")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BodyColor.entries.forEach { color ->
                ColorSwatch(
                    color = BodyColorSwatches.getValue(color),
                    label = color.name,
                    selected = state.appearance.bodyColor == color,
                    enabled = !state.isLoading,
                    onSelect = { onBodyColorChange(color) },
                )
            }
        }

        SectionTitle("Глаза")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            EyesVariant.entries.forEach { eyes ->
                ChoiceChip(
                    label = EyesLabels.getValue(eyes),
                    selected = state.appearance.eyes == eyes,
                    enabled = !state.isLoading,
                    onSelect = { onEyesChange(eyes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FinneyButton(
            text = if (state.isEditing) "Сохранить" else "Начать игру",
            onClick = onSave,
            enabled = !state.isSaving && !state.isLoading,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = FinneyInk,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NameField(
    name: String,
    error: PetNameError?,
    maxLength: Int,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Например, Финни", style = MaterialTheme.typography.bodyLarge) },
        isError = error != null,
        supportingText = {
            // Ошибка — не только красной рамкой: текстом сказано, что именно не так (ТЗ п. 3.6).
            when (error) {
                PetNameError.BLANK -> Text("Придумай имя", style = MaterialTheme.typography.bodyMedium)
                PetNameError.TOO_LONG -> Text(
                    "Не длиннее $maxLength букв",
                    style = MaterialTheme.typography.bodyMedium,
                )
                null -> Unit
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FinneySand,
            unfocusedContainerColor = FinneySand,
            disabledContainerColor = FinneySand,
            errorContainerColor = FinneySand,
            focusedIndicatorColor = FinneyInk,
            unfocusedIndicatorColor = FinneyInk,
            errorIndicatorColor = FinneyPink,
            focusedTextColor = FinneyInk,
            unfocusedTextColor = FinneyInk,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
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
            val label = CharacterLabels.getValue(character)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    // Выбранная карточка отличается и заливкой, и толщиной рамки,
                    // а не одним цветом (ТЗ п. 3.6).
                    .background(if (isSelected) FinneyYellow else FinneySand)
                    .border(
                        width = if (isSelected) 4.dp else 2.dp,
                        color = FinneyInk,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            onSelect(character)
                            animation.playJoy()
                        },
                    )
                    .padding(8.dp)
                    .clearAndSetSemantics { contentDescription = label },
            ) {
                PetView(
                    character = character,
                    mood = PetMood.HAPPY,
                    pose = pose,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = FinneyInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Кружок цвета. Выбранный — с толстой рамкой и подписью под ним, чтобы не полагаться на цвет. */
@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            // 64 dp — с запасом к минимуму 48 dp из ТЗ п. 3.6.
            .size(64.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 5.dp else 2.dp, FinneyInk, CircleShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .clearAndSetSemantics { contentDescription = "Цвет $label" },
        contentAlignment = Alignment.Center,
    ) {
        // Галочка — второй, не цветовой признак выбора.
        if (selected) {
            OutlinedText("✓", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Плашка выбора из нескольких вариантов. */
@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) FinneyYellow else FinneySand)
            .border(
                width = if (selected) 4.dp else 2.dp,
                color = FinneyInk,
                shape = RoundedCornerShape(percent = 50),
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinneyInk,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun PetSetupContentPreview() {
    FinneyTheme {
        PetSetupContent(
            state = PetSetupUiState(isEditing = false, name = "Финни"),
            onNameChange = {}, onCharacterChange = {}, onBodyColorChange = {},
            onEyesChange = {}, onSave = {},
        )
    }
}
