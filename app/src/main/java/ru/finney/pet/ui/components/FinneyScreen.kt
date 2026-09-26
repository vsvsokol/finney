package ru.finney.pet.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream

// Подложка экрана: кремовый фон и одинаковые поля по краям. Отдельный компонент,
// чтобы отступы не разъезжались от экрана к экрану и их не приходилось помнить.

/** Поля экрана. 16 dp по бокам — на 360 dp ширины (ТЗ п. 3.1) содержимому остаётся 328 dp. */
private val ScreenPadding = 16.dp

/**
 * [scrollable] — экран длиннее высоты устройства: содержимое можно прокручивать.
 *
 * [bottom] — то, что должно быть видно всегда, обычно кнопка выхода. Прокручивается
 * только содержимое над ним. Без этого кнопка в конце длинного экрана уезжала за
 * нижний край, и ребёнок не находил, как уйти дальше.
 *
 * Прокрутка включается флагом, а не `Modifier.verticalScroll()` снаружи: порядок
 * модификаторов важен, и снаружи она встала бы раньше отступов под системные панели —
 * содержимое уезжало бы под строку состояния. Здесь порядок задан один раз и верно.
 */
@Composable
fun FinneyScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (bottom == null) {
        val scroll = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(FinneyCream)
                // Системные панели: под строкой состояния и кнопками навигации
                // содержимое оказаться не должно.
                .systemBarsPadding()
                .padding(ScreenPadding)
                .then(scroll),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinneyCream)
            .systemBarsPadding()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        val scroll = if (scrollable) Modifier.fadingScroll(rememberScrollState()) else Modifier
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().then(scroll),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
        bottom()
    }
}

/** Высота затухания у края прокрутки. */
private val FadeEdge = 24.dp

/**
 * Прокрутка, у которой край не режет содержимое по линейке, а растворяет его.
 *
 * Прокрутка обрезает всё по своему прямоугольнику. Над кнопкой «Назад» это
 * выглядело как кусок фона, срезанный ножом прямо по середине кнопки или карточки.
 * Теперь со стороны, куда ещё можно листать, содержимое плавно гаснет — видно,
 * что дальше что-то есть, и никакого прямоугольника.
 */
fun Modifier.fadingScroll(state: ScrollState, edge: Dp = FadeEdge): Modifier = this
    // Отдельный слой: DstIn должен гасить только содержимое, а не фон экрана под ним.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val px = edge.toPx().coerceAtMost(size.height / 2)
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = px),
                size = Size(size.width, px),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - px,
                    endY = size.height,
                ),
                topLeft = Offset(0f, size.height - px),
                size = Size(size.width, px),
                blendMode = BlendMode.DstIn,
            )
        }
    }
    .verticalScroll(state)
