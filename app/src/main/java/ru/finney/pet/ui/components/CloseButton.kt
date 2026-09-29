package ru.finney.pet.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyGlare
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Кнопка «✕» по макету дизайнеров (`close_button.svg`, 29.09). Все числа ниже — из него,
// в единицах его холста 69 × 69: так кнопка собирается одинаково при любом размере.
// Отличие от прочих круглых кнопок кита — низ: здесь это серп (круг минус тот же круг,
// поднятый на 10), а не дуга эллипса, и крест нарисован линиями, а не буквой шрифта.

private const val Canvas69 = 69f

/**
 * Круглый «✕»: выход с экрана или из панели. 48 dp — меньше палец ребёнка не
 * попадает (ТЗ п. 3.6); для TalkBack — [description]. Под пальцем, как все кнопки
 * кита, шаг в тёплое (персиковый сверху, розовый серп) и сжатие пружиной.
 */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Назад",
    size: Dp = 48.dp,
) {
    val sounds = LocalSounds.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                sounds.play(Sfx.Back)
                onClick()
            }
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = this.size.minDimension / Canvas69
            val c = Offset(34.5f * u, 34.5f * u)
            val face = if (pressed) FinneyPeach else FinneyYellow
            val band = if (pressed) FinneyPink else FinneyPeach

            // Серп снизу: весь круг цветом полосы, сверху — тот же круг цветом лица, поднятый на 10.
            drawCircle(band, radius = 34.5f * u, center = c)
            drawCircle(face, radius = 34.5f * u, center = c.copy(y = c.y - 10f * u))
            // Обводка: радиус 32 и толщина 5 — внешний край ровно по краю круга.
            drawCircle(FinneyInk, radius = 32f * u, center = c, style = Stroke(5f * u))
            // Блик — белый эллипс, повёрнутый на −42.7°.
            val glare = Offset(18.1275f * u, 15.1275f * u)
            rotate(-42.6863f, pivot = glare) {
                drawOval(
                    FinneyGlare,
                    topLeft = Offset(glare.x - 6.10764f * u, glare.y - 2.55787f * u),
                    size = Size(2 * 6.10764f * u, 2 * 2.55787f * u),
                )
            }
            // Крест — две линии через (35, 34) с круглыми концами.
            val stroke = 5f * u
            drawLine(FinneyInk, Offset(23.3077f * u, 22.3077f * u), Offset(46.6923f * u, 45.6923f * u), stroke, StrokeCap.Round)
            drawLine(FinneyInk, Offset(46.6923f * u, 22.3077f * u), Offset(23.3077f * u, 45.6923f * u), stroke, StrokeCap.Round)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun CloseButtonPreview() {
    FinneyTheme { CloseButton(onClick = {}) }
}
