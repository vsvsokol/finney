package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.BodyColor
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
import ru.finney.pet.ui.theme.FinneyInk
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

@Composable
fun PetSetupScreen(
    isEditing: Boolean,
    onSaved: () -> Unit,
    isDemo: Boolean = false,
    viewModel: PetSetupViewModel = viewModel(factory = PetSetupViewModel.factory(isEditing, isDemo)),
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
        onSave = viewModel::save,
    )
}

/**
 * Цвет тела рисуется сдвигом оттенка тех же слоёв (`BodyColor.colorFilter` в
 * ui/pet/PetView.kt): 5 питомцев × 3 цвета = 15 различимых вариантов (ТЗ п. 2.6).
 * Выбора глаз по-прежнему нет: варианты глаз не нарисованы, а выбор, который
 * ничего не меняет на экране, ребёнка только путает. Сеттер в [PetSetupViewModel] остался.
 */
@Composable
private fun PetSetupContent(
    state: PetSetupUiState,
    onNameChange: (String) -> Unit,
    onCharacterChange: (PetCharacter) -> Unit,
    onBodyColorChange: (BodyColor) -> Unit,
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
            bodyColor = state.appearance.bodyColor,
            enabled = !state.isLoading,
            onSelect = onCharacterChange,
        )

        SectionTitle("Какого цвета")
        ColorPicker(
            character = state.appearance.character,
            selected = state.appearance.bodyColor,
            enabled = !state.isLoading,
            onSelect = onBodyColorChange,
        )

        SectionTitle("Как его зовут")
        NameField(
            name = state.name,
            error = state.nameError,
            maxLength = state.maxNameLength,
            enabled = !state.isLoading,
            onNameChange = onNameChange,
        )

        FinneyButton(
            text = if (state.isEditing) "Сохранить" else "Начать игру",
            onClick = onSave,
            enabled = !state.isSaving && !state.isLoading,
        )
    }
}

private val BodyColorLabels = mapOf(
    BodyColor.A to "родной",
    BodyColor.B to "тёплый",
    BodyColor.C to "свежий",
)

/**
 * Цвет тела: три неподвижных питомца в своих цветах. Выбранный отмечен заливкой,
 * рамкой и словом — не только цветом (ТЗ п. 3.6).
 */
@Composable
private fun ColorPicker(
    character: PetCharacter,
    selected: BodyColor,
    enabled: Boolean,
    onSelect: (BodyColor) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BodyColor.entries.forEach { color ->
            val isSelected = color == selected
            val label = BodyColorLabels.getValue(color)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(RadiusCard))
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
                        onClick = { onSelect(color) },
                    )
                    .padding(6.dp)
                    .clearAndSetSemantics { contentDescription = "Цвет: $label" },
            ) {
                PetView(
                    character = character,
                    bodyColor = color,
                    mood = PetMood.HAPPY,
                    pose = StillPose,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = FinneyInk,
                    textAlign = TextAlign.Center,
                )
            }
        }
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
    bodyColor: BodyColor,
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
                bodyColor = bodyColor,
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
    bodyColor: BodyColor,
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
            bodyColor = bodyColor,
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

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun PetSetupContentPreview() {
    FinneyTheme {
        PetSetupContent(
            state = PetSetupUiState(isEditing = false, name = "Финни"),
            onNameChange = {}, onCharacterChange = {}, onBodyColorChange = {}, onSave = {},
        )
    }
}
