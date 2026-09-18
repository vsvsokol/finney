package ru.finney.pet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGlare
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Кнопки из UI-кита. Общее у всех: синяя обводка снаружи, градиентная заливка
// и блик-эллипс слева сверху. Нажатие — не затемнение, как в Material, а смена
// заливки с жёлтой на персиковую: именно так нарисованы «нормальное» и
// «нажатое» состояния в макете.

/** Высота кнопки. Больше минимальных 48 dp из ТЗ п. 3.6 — по кнопке должен попадать ребёнок. */
private val ButtonHeight = 64.dp

private val OutlineWidth = 3.dp

/** Заливка в покое: жёлтая с переходом в персиковый снизу. */
private val RestBrush = Brush.verticalGradient(listOf(FinneyYellow, FinneyPeach))

/** Заливка под пальцем: та же форма целиком в персиковом. */
private val PressedBrush = Brush.verticalGradient(listOf(FinneyPeach, FinneyPeach))

/**
 * Основная кнопка: «Играть», «Закрыть», «Подтвердить план».
 *
 * [fillWidth] — растянуть по ширине родителя; иначе кнопка по размеру надписи.
 */
@Composable
fun FinneyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = ButtonHeight),
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(OutlineWidth, FinneyInk),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .background(if (pressed) PressedBrush else RestBrush, shape)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Glare(Modifier.align(Alignment.TopStart).offset(x = 6.dp, y = 2.dp))
            OutlinedText(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Круглая кнопка-иконка: действия с питомцем (кушать, мыться, магазин, свет)
 * и служебные кнопки вроде истории. Подпись не входит в саму кнопку —
 * под иконкой её ставит вызывающий экран, чтобы попадание было по кругу.
 */
@Composable
fun FinneyIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        // Содержимое кнопки — рисунок без текста, поэтому подпись для TalkBack
        // задаётся здесь и заменяет собой то, что внутри.
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(OutlineWidth, FinneyInk),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.background(if (pressed) PressedBrush else RestBrush, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Glare(
                modifier = Modifier.align(Alignment.TopStart).offset(x = size * 0.22f, y = size * 0.07f),
                width = size * 0.26f,
                height = size * 0.22f,
            )
            content()
        }
    }
}

/**
 * Блик в левом верхнем углу — белый эллипс. В макете он есть у каждой кнопки
 * и делает её «выпуклой»; без него форма выглядит плоской наклейкой.
 */
@Composable
private fun Glare(
    modifier: Modifier = Modifier,
    width: Dp = 26.dp,
    height: Dp = 20.dp,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(FinneyGlare),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun FinneyButtonPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FinneyButton("Играть", onClick = {})
            FinneyButton("Закрыть", onClick = {}, fillWidth = false)
            FinneyIconButton(onClick = {}, contentDescription = "Кушать") {
                OutlinedText("Ф", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
