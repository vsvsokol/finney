package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import ru.finney.pet.ui.components.FinneyTextField
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetPose
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.RadiusCard
import ru.finney.pet.ui.theme.StrokeBold
import ru.finney.pet.ui.theme.StrokeThin
import ru.finney.pet.ui.theme.FinneyYellow

/**
 * Подписи питомцев для игрока. В домене они названы по автору рисунка
 * (design/exports/pet) — ребёнку такое имя ничего не говорит. Здесь заглушки:
 * настоящие имена придумывает @vsvsokol вместе с текстами знакомства.
 */
private val CharacterLabels = mapOf(
    PetCharacter.PUSHISTIK to "Пушистик",
    PetCharacter.ROGATIK to "Рогатик",
    PetCharacter.ZVEZDOCHKA to "Звёздочка",
    PetCharacter.BANTIK to "Бантик",
    PetCharacter.LUCHIK to "Лучик",
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
    // Ошибка — и словом, и цветом рамки: цвет не единственный признак (ТЗ п. 3.6).
    val message = when (error) {
        PetNameError.BLANK -> "Впиши имя"
        PetNameError.TOO_LONG -> "Не длиннее $maxLength букв"
        null -> null
    }
    FinneyTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = "Например, Финни",
        isError = error != null,
        supportingText = message,
        enabled = enabled,
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
    // LazyRow, а не Row: питомцев стало пять, и в ряд по ширине экрана они не
    // помещаются — карточка ужималась до полусотни точек. Ширина фиксированная,
    // список прокручивается вбок.
    //
    // Ленивый он не ради памяти, а ради кадров: каждая карточка крутит свою
    // анимацию (см. комментарий к CharacterCard про 5 fps), и в LazyRow
    // одновременно живут только видимые — две-три вместо пяти.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(PetCharacter.entries) { character ->
            CharacterCard(
                character = character,
                isSelected = character == selected,
                enabled = enabled,
                onSelect = { onSelect(character) },
                modifier = Modifier.width(132.dp),
            )
        }
    }
}

/**
 * Карточка одного питомца.
 *
 * Вынесена из [CharacterPicker] не для красоты: `currentPose()` читает кадры анимации,
 * и та функция, в теле которой стоит чтение, пересобирается каждый кадр. Пока вызов
 * был в `CharacterPicker`, каждый кадр пересобирал обе карточки разом — вместе с
 * `PetView`, а тот на каждой пересборке заново достаёт слои питомца из ресурсов.
 * На экране выходило 5 кадров в секунду. Здесь пересборка ограничена одной карточкой.
 */
@Composable
private fun CharacterCard(
    character: PetCharacter,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animation = rememberPetAnimation()
    val label = CharacterLabels.getValue(character)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(RadiusCard))
            // Выбранная карточка отличается и заливкой, и толщиной рамки,
            // а не одним цветом (ТЗ п. 3.6).
            .background(if (isSelected) FinneyYellow else FinneySand)
            .border(
                width = if (isSelected) StrokeBold else StrokeThin,
                color = FinneyInk,
                shape = RoundedCornerShape(RadiusCard),
            )
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = {
                    onSelect()
                    animation.playJoy()
                },
            )
            .padding(8.dp)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        // Живым остаётся только выбранный: неподвижному поза не нужна,
        // и лишний источник кадров ему незачем.
        val pose = rememberPoseProvider(animation)
        PetView(
            character = character,
            mood = PetMood.HAPPY,
            pose = if (isSelected) pose else StillPose,
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

/** Поза невыбранного питомца: стоит ровно. Одна на всех, чтобы не плодить лямбды. */
private val StillPose: () -> PetPose = { PetPose() }

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
            .border(if (selected) StrokeBold else StrokeThin, FinneyInk, CircleShape)
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
                width = if (selected) StrokeBold else StrokeThin,
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

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
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
