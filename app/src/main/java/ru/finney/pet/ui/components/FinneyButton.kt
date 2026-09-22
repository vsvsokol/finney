package ru.finney.pet.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.strokeFor
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGlare
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyStrokeRatio
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Кнопки из UI-кита. Общее у всех: синяя обводка снаружи, плоская заливка,
// полоса потемнее по низу и блик-эллипс слева сверху. Нажатие — не затемнение,
// как в Material, а смена пары цветов с жёлтой на персиковую: именно так
// нарисованы «нормальное» и «нажатое» состояния в макете.
//
// Все числа ниже промерены по эталону design/reference/Figma export finney/Frame 3.png
// (кнопка 420 × 120) — не на глаз и не из ранних заметок. Силуэт со скруглением
// ровно в половину высоты («стадион»), обводка 10 (= 1/12 высоты), нижняя полоса
// начинается на y = 90 из 120, то есть ровно нижняя четверть, граница прямая.

/** Высота кнопки. Больше минимальных 48 dp из ТЗ п. 3.6 — по кнопке должен попадать ребёнок. */
private val ButtonHeight = 64.dp

/**
 * Доля высоты под нижнюю полосу. В эталоне полоса занимает y 90..119 из 120.
 * Это не тень-подложка и не градиент: плоский цвет с прямой границей,
 * обрезанный общей формой кнопки.
 */
private const val BottomBandFraction = 0.25f

// Блик — наклонённый эллипс, а не круг: в эталоне он лежит вдоль скругления угла.
// Всё в долях высоты кнопки. Размеры получены как bbox чисто белых пикселей
// в Frame 3.png: 34 × 24 при высоте 120 с отступом 29 / 24 от левого верхнего угла.
//
// Раньше здесь стояли 0.31 × 0.14 при 0.38 / 0.25, а в шапке файла — третий
// вариант. Числа ниже — замер, остальные версии удалены, чтобы не расходились снова.

/** Ширина блика — доля высоты кнопки. */
private const val GlareWidthFraction = 0.283f

/** Высота блика — доля высоты кнопки. */
private const val GlareHeightFraction = 0.200f

/** Отступ центра блика от левого края — доля высоты кнопки. */
private const val GlareCentreFraction = 0.242f + GlareWidthFraction / 2f

/** Отступ центра блика от верха — доля высоты кнопки. */
private const val GlareTopFraction = 0.200f + GlareHeightFraction / 2f

/**
 * Наклон блика. Отрицательный — против часовой, вдоль скругления угла.
 * Отдельным числом в макете не задан, подобран на глаз по эталону.
 */
private const val GlareAngle = -29.5f

// У круглой кнопки блик свой, заметно мельче. Промерено по кадру кита на кнопке
// с вилкой (диаметр 154): пятно чисто белых пикселей 22 × 25 с центром
// в 29.5 / 40 от левого верхнего угла. Числа от плоской кнопки давали блик
// вдвое шире — он забирал верх кружка и оставлял значку меньше места,
// чем в эталоне.

/** Ширина блика круглой кнопки — доля диаметра. */
private const val RoundGlareWidthFraction = 0.143f

/** Высота блика круглой кнопки — доля диаметра. */
private const val RoundGlareHeightFraction = 0.162f

/** Отступ центра блика круглой кнопки от левого края — доля диаметра. */
private const val RoundGlareCentreFraction = 0.192f

/** Отступ центра блика круглой кнопки от верха — доля диаметра. */
private const val RoundGlareTopFraction = 0.260f

/**
 * Сжатие под пальцем. Нажатие в игре должно отзываться телом, а не только цветом:
 * ребёнок 7–11 лет должен видеть, что игра его услышала.
 */
private const val PressedScale = 0.96f

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
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = ButtonHeight)
            // Масштаб читается внутри graphicsLayer, то есть на отрисовке:
            // пружина не пересобирает кнопку на каждом кадре.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .buttonFill(pressed, round = false)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            OutlinedText(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Круглая кнопка-иконка: действия с питомцем (кушать, мыться, магазин, свет)
 * и служебные кнопки вроде истории. Подпись не входит в саму кнопку —
 * под иконкой её ставит вызывающий экран, чтобы попадание было по кругу.
 *
 * [selected] — кнопка показывает то, что открыто сейчас. Берёт «нажатое
 * состояние» из кита: других состояний у круглой кнопки там не нарисовано,
 * а выбранное отличать надо. Уходит и в TalkBack: цвет — не единственный
 * признак (ТЗ п. 3.6).
 */
@Composable
fun FinneyIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )

    Surface(
        onClick = onClick,
        // Содержимое кнопки — рисунок без текста, поэтому подпись для TalkBack
        // задаётся здесь и заменяет собой то, что внутри.
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                this.contentDescription = contentDescription
                this.selected = selected
            },
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.clip(CircleShape).buttonFill(pressed || selected, round = true),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

// Кольцо шкалы вокруг круглой кнопки. В ките оно подписано «опускание/поднятие
// шкал потребностей (по часовой стрелке)» и нарисовано одинаково у кнопки
// действия и у значка уровня.
//
// Промерено по кадру кита на значке уровня (внешний диаметр 350 px): полосы
// идут снаружи внутрь — синее кольцо 15, дорожка 14, обводка самой кнопки 15,
// и дальше заливка. Дорожка пустая белая, заполненная часть розовая, и цвета
// эти в ките одни и те же в покое и под пальцем.

/**
 * Ширина дорожки — доля диаметра кнопки: 14 из 292 в кадре кита.
 *
 * Внешнее кольцо отдельным числом не задаётся: это обычная обводка, то есть
 * `strokeFor()`. В кадре она немного тоньше, но по всему приложению обводка
 * одна, и кольцо из общего правила не выбивается.
 */
private const val RingTrackFraction = 0.048f

/**
 * Кольцо-шкала вокруг круглого содержимого.
 *
 * [diameter] — диаметр того, что внутри; кольцо ложится снаружи, поэтому сам
 * элемент получается шире. [progress] в долях 0..1; null — шкалы нет, кольцо
 * не рисуется, но место под него остаётся: иначе кнопки в одном ряду разъедутся
 * по размеру и ряд перестанет читаться как ряд.
 */
@Composable
fun ProgressRing(
    diameter: Dp,
    progress: Float?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val track = diameter * RingTrackFraction
    val ring = strokeFor(diameter)
    val fraction by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        label = "ring",
    )

    Box(
        modifier = modifier
            .size(diameter + (track + ring) * 2)
            .drawBehind {
                if (progress == null) return@drawBehind

                val trackWidth = track.toPx()
                val ringWidth = ring.toPx()

                // Радиус по середине дорожки: она начинается ровно от края
                // содержимого, потому что обводка кнопки уже уложена внутрь
                // её собственного круга.
                val trackRadius = (diameter.toPx() + trackWidth) / 2f
                drawCircle(FinneyCream, radius = trackRadius, style = Stroke(trackWidth))
                drawArc(
                    color = FinneyPink,
                    // -90° — верх. Дуга растёт по часовой стрелке, как подписано в ките.
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                    size = Size(trackRadius * 2f, trackRadius * 2f),
                    style = Stroke(trackWidth),
                )

                drawCircle(
                    color = FinneyInk,
                    radius = (diameter.toPx() + trackWidth * 2f + ringWidth) / 2f,
                    style = Stroke(ringWidth),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Кнопка комнаты: круглая иконка со шкалой потребности вокруг.
 *
 * Шкалы живут здесь, а не отдельной полосой сверху, — так они нарисованы
 * в ките и так их видно ровно там, где эту потребность закрывают.
 *
 * [value] — шкала 0..100; null — за кнопкой нет потребности (зал и сон),
 * кольцо тогда пустое. Короткая дуга и есть нецветовой признак «мало»
 * (ТЗ п. 3.6): длина читается и без различения цветов, а число уходит
 * в TalkBack.
 */
@Composable
fun FinneyNeedButton(
    icon: FinneyIcons,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: Int? = null,
    selected: Boolean = false,
    size: Dp = 64.dp,
) {
    val clamped = value?.coerceIn(0, 100)
    ProgressRing(
        diameter = size,
        progress = clamped?.let { it / 100f },
        modifier = modifier,
    ) {
        FinneyIconButton(
            onClick = onClick,
            contentDescription = if (clamped == null) label else "$label, $clamped из 100",
            size = size,
            selected = selected,
        ) {
            FinneyIcon(icon, size = size / 2)
        }
    }
}

/**
 * Заливка кнопки: плоский цвет, полоса потемнее по низу, блик и обводка.
 *
 * Обе пары цветов идут по палитре: в покое жёлтый на персиковом (кнопка —
 * это «деньги и награда»), под пальцем всё сдвигается на шаг в тёплое.
 *
 * [round] — кнопка круглая. У прямоугольной нижняя полоса прямая, у круглой
 * это **серп с выпуклой верхней границей**: в эталоне видно, что персиковая
 * область у кружка глубже всего по центру и сходит на нет к краям. Прямая
 * полоса на круге выглядит как ошибка отрисовки.
 *
 * Обводка рисуется здесь, а не `BorderStroke` у `Surface`: `BorderStroke`
 * кладёт линию внутрь границ, а в макете `stroke position: outside`. Рисуем
 * сами с `Stroke` по краю — половина толщины уходит за границу формы и
 * обрезается `clip`, поэтому видимая обводка получается ровной и не съедает
 * заливку изнутри.
 */
private fun Modifier.buttonFill(pressed: Boolean, round: Boolean): Modifier = drawBehind {
    val face = if (pressed) FinneyPeach else FinneyYellow
    val band = if (pressed) FinneyPink else FinneyPeach
    drawRect(face)

    if (round) {
        // Серп: широкий плоский овал, чья верхняя дуга проходит по границе
        // BottomBandFraction в центре кнопки и поднимается к краям. Всё, что
        // вышло за круг, срезает clip.
        //
        // Овал намеренно шире кнопки: если взять его по ширине круга, дуга у
        // краёв уходит слишком высоко и персикового становится больше половины,
        // а в эталоне он занимает примерно нижнюю четверть.
        val top = size.height * (1f - BottomBandFraction)
        val overhang = size.width * 0.9f
        drawOval(
            color = band,
            topLeft = Offset(-overhang, top),
            size = Size(size.width + overhang * 2f, (size.height - top) * 2.4f),
        )
    } else {
        val bandHeight = size.height * BottomBandFraction
        drawRect(
            color = band,
            topLeft = Offset(0f, size.height - bandHeight),
            size = Size(size.width, bandHeight),
        )
    }

    // Блик. Доли высоты кнопки, но у круглой они свои: в ките её блик мельче
    // и сидит ближе к краю.
    val glareWidth = size.height * if (round) RoundGlareWidthFraction else GlareWidthFraction
    val glareHeight = size.height * if (round) RoundGlareHeightFraction else GlareHeightFraction
    val centre = if (round) {
        Offset(size.height * RoundGlareCentreFraction, size.height * RoundGlareTopFraction)
    } else {
        Offset(size.height * GlareCentreFraction, size.height * GlareTopFraction)
    }
    rotate(degrees = GlareAngle, pivot = centre) {
        drawOval(
            color = FinneyGlare,
            topLeft = Offset(centre.x - glareWidth / 2f, centre.y - glareHeight / 2f),
            size = Size(glareWidth, glareHeight),
        )
    }

    // Обводка по краю. Толщина — 1/12 высоты, как промерено в эталоне.
    val outline = size.height * FinneyStrokeRatio
    if (round) {
        drawCircle(
            color = FinneyInk,
            radius = (size.minDimension - outline) / 2f,
            style = Stroke(width = outline),
        )
    } else {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = FinneyInk,
            topLeft = Offset(outline / 2f, outline / 2f),
            size = Size(size.width - outline, size.height - outline),
            cornerRadius = radius,
            style = Stroke(width = outline),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinneyNeedButton(FinneyIcons.Lamp, "Зал и сон", onClick = {})
                FinneyNeedButton(FinneyIcons.Food, "Покормить", onClick = {}, value = 75)
                FinneyNeedButton(FinneyIcons.Bath, "Помыть", onClick = {}, value = 20, selected = true)
            }
        }
    }
}
