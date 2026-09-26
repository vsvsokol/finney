package ru.finney.pet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyInkFaded
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.RadiusCard
import ru.finney.pet.ui.theme.RadiusCheckbox
import ru.finney.pet.ui.theme.RadiusField
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.theme.StrokeThin

// Мелкие элементы управления из кадра UI-кита: чекбокс, слайдер, полоса
// прокрутки, поле ввода и карточка. Раньше их не было вовсе — экраны рисовали
// похожее вручную, и в проекте разъехались и радиусы, и толщины обводок.

/** Минимальная сторона интерактивного элемента по ТЗ п. 3.6. */
private val MinTouch = 48.dp

/**
 * Чекбокс из списка условий.
 *
 * Отмеченный — персиковый **с крестом**, снятый — жёлтый пустой. Крест здесь
 * обязателен, а не украшение: по ТЗ п. 3.6 цвет не может быть единственным
 * признаком состояния. Текст отмеченного пункта зачёркивается и бледнеет —
 * ровно как в эталоне.
 */
@Composable
fun FinneyCheckbox(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    boxSize: Dp = 32.dp,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = MinTouch)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(RadiusCheckbox))
                .background(if (checked) FinneyPeach else FinneyYellow)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(RadiusCheckbox)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(modifier = Modifier.size(boxSize * 0.62f)) {
                    val w = size.minDimension
                    val cap = StrokeCap.Round
                    val thickness = w * 0.18f
                    drawLine(FinneyInk, Offset(0f, 0f), Offset(w, w), thickness, cap)
                    drawLine(FinneyInk, Offset(w, 0f), Offset(0f, w), thickness, cap)
                }
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) FinneyInkFaded else FinneyInk,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
        )
    }
}

/**
 * Слайдер из панели «Настройки»: персиковая капсула с розовой ручкой.
 *
 * Своя отрисовка, а не Material `Slider`: у того ручка, дорожка и «тики»
 * тянут за собой тему Material, и покрасить его в плоский стиль макета
 * без переопределения половины `SliderDefaults` не выходит.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinneySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: FinneyIcons? = FinneyIcons.Speaker,
) {
    val fraction = value.coerceIn(0f, 1f)
    val trackHeight = 26.dp
    val knob = 34.dp

    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = MinTouch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { FinneyIcon(it, size = 28.dp) }

        androidx.compose.material3.Slider(
            value = fraction,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(CircleShape)
                        .background(FinneyPeach)
                        .border(StrokeRegular, FinneyInk, CircleShape),
                )
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(knob)
                        .clip(CircleShape)
                        .background(FinneyPink)
                        .border(StrokeRegular, FinneyInk, CircleShape),
                )
            },
        )
    }
}

/**
 * Полоса прокрутки из панели «История»: персиковая капсула с розовым бегунком.
 *
 * Декоративная — прокруткой управляет сам список, а не она. Поэтому скрыта
 * от TalkBack: озвучивать её отдельно нечем, содержимое читается списком.
 *
 * [fraction] — положение бегунка 0..1, [portion] — какую долю списка видно.
 */
@Composable
fun FinneyScrollbar(
    fraction: Float,
    modifier: Modifier = Modifier,
    portion: Float = 0.3f,
    width: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clearAndSetSemantics {}
            .clip(CircleShape)
            .background(FinneyPeach)
            .border(StrokeRegular, FinneyInk, CircleShape),
    ) {
        val visible = portion.coerceIn(0.1f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(visible)
                .align(BiasAlignmentVertical(fraction.coerceIn(0f, 1f)))
                .clip(CircleShape)
                .background(FinneyPink)
                .border(StrokeRegular, FinneyInk, CircleShape),
        )
    }
}

/** Выравнивание по доле высоты: 0 — верх, 1 — низ. */
private fun BiasAlignmentVertical(fraction: Float): Alignment =
    BiasAlignment(horizontalBias = 0f, verticalBias = fraction * 2f - 1f)

/**
 * Поле ввода на песочном фоне.
 *
 * Раньше `OutlinedTextField` раскрашивался руками прямо в экране создания
 * питомца — здесь то же самое, но один раз и для всех экранов.
 */
@Composable
fun FinneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        placeholder = placeholder?.let {
            { Text(it, style = MaterialTheme.typography.bodyLarge) }
        },
        isError = isError,
        singleLine = singleLine,
        enabled = enabled,
        supportingText = supportingText?.let {
            { Text(it, style = MaterialTheme.typography.bodyMedium) }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(RadiusField),
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
            focusedLabelColor = FinneyInk,
            unfocusedLabelColor = FinneyInk,
            cursorColor = FinneyInk,
        ),
    )
}

/**
 * Карточка внутри экрана: песочная подложка с обводкой.
 *
 * Свела четыре почти одинаковых инлайновых стиля из экрана плана в один.
 * [accent] — заливка вместо песочной: для итогов «уложился / не уложился»,
 * где карточка сама по себе несёт цвет.
 */
@Composable
fun FinneyCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(accent ?: FinneySand)
            .border(
                if (accent != null) StrokeRegular else StrokeThin,
                FinneyInk,
                RoundedCornerShape(RadiusCard),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun FinneyControlsPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FinneyCheckbox(checked = false, text = "посчитать деньги", onCheckedChange = {})
            FinneyCheckbox(checked = true, text = "купить еду в магазине", onCheckedChange = {})
            FinneySlider(value = 0.4f, onValueChange = {}, label = "Громкость")
            FinneyTextField(value = "Финни", onValueChange = {}, label = "Имя питомца")
            FinneyCard {
                Text("Обычная карточка", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            }
        }
    }
}
