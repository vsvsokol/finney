package ru.finney.pet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme

// Значки интерфейса. Рисуются кодом: иконок в ресурсах нет, а выдумывать пути
// к несуществующим файлам нельзя.
//
// В эталоне значки — плотные силуэты цветом обводки (#382C92) внутри круглой
// кнопки, без своей обводки и без полутонов. Поэтому здесь всё рисуется одним
// цветом и сплошной заливкой: эмодзи, стоявшие тут раньше, были многоцветными
// и спорили с плоским стилем макета.
//
// Все координаты — доли стороны квадрата, поэтому значок одинаково ложится
// на кнопку любого размера.

/** Что рисовать. Набор ровно тот, что нужен экранам и нарисован в макете. */
enum class FinneyIcons {
    /** Лампа — «зал/спать». */
    Lamp,

    /** Вилка и нож — «кушать». */
    Food,

    /** Ванна — «мыться». */
    Bath,

    /** Тележка — «магазин». */
    Cart,

    /** Динамик — громкость в настройках. */
    Speaker,

    /** Знак вопроса — подсказка. */
    Help,

    /** Три полосы — меню взрослого. */
    Menu,

    /** Монетка-копилка — цели и накопления. */
    Piggy,

    /** Звезда — задания. */
    Star,

    /** Кубок — прогресс. */
    Trophy,

    /** Тетрадь — план расходов. */
    Plan,
}

/**
 * Значок [icon] размером [size].
 *
 * Без `contentDescription`: значки стоят внутри кнопок, у которых подпись для
 * TalkBack задана своя, а дублирующая подпись заставила бы читать её дважды.
 */
@Composable
fun FinneyIcon(
    icon: FinneyIcons,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = FinneyInk,
) {
    Canvas(modifier = modifier.size(size)) {
        when (icon) {
            FinneyIcons.Lamp -> drawLamp(tint)
            FinneyIcons.Food -> drawFood(tint)
            FinneyIcons.Bath -> drawBath(tint)
            FinneyIcons.Cart -> drawCart(tint)
            FinneyIcons.Speaker -> drawSpeaker(tint)
            FinneyIcons.Help -> drawHelp(tint)
            FinneyIcons.Menu -> drawMenu(tint)
            FinneyIcons.Piggy -> drawPiggy(tint)
            FinneyIcons.Star -> drawStar(tint)
            FinneyIcons.Trophy -> drawTrophy(tint)
            FinneyIcons.Plan -> drawPlan(tint)
        }
    }
}

// Лампа: трапеция абажура, ножка и основание — как в эталоне.
private fun DrawScope.drawLamp(tint: Color) {
    val s = size.minDimension
    val shade = Path().apply {
        moveTo(s * 0.30f, s * 0.52f)
        lineTo(s * 0.70f, s * 0.52f)
        lineTo(s * 0.60f, s * 0.22f)
        lineTo(s * 0.40f, s * 0.22f)
        close()
    }
    drawPath(shade, tint)
    drawRect(tint, Offset(s * 0.465f, s * 0.52f), Size(s * 0.07f, s * 0.22f))
    drawRoundRectSolid(tint, s * 0.32f, s * 0.74f, s * 0.36f, s * 0.07f)
}

// Вилка и нож.
private fun DrawScope.drawFood(tint: Color) {
    val s = size.minDimension
    val stroke = s * 0.075f

    // Вилка: три зубца и ножка.
    for (i in 0..2) {
        val x = s * 0.24f + i * s * 0.085f
        drawLine(tint, Offset(x, s * 0.18f), Offset(x, s * 0.40f), stroke, StrokeCap.Round)
    }
    drawLine(
        tint,
        Offset(s * 0.24f, s * 0.40f),
        Offset(s * 0.41f, s * 0.40f),
        stroke,
        StrokeCap.Round,
    )
    drawLine(
        tint,
        Offset(s * 0.325f, s * 0.40f),
        Offset(s * 0.325f, s * 0.82f),
        stroke * 1.2f,
        StrokeCap.Round,
    )

    // Нож: лезвие каплей и ручка.
    val blade = Path().apply {
        moveTo(s * 0.68f, s * 0.18f)
        quadraticTo(s * 0.80f, s * 0.34f, s * 0.71f, s * 0.52f)
        lineTo(s * 0.64f, s * 0.52f)
        lineTo(s * 0.64f, s * 0.24f)
        close()
    }
    drawPath(blade, tint)
    drawLine(
        tint,
        Offset(s * 0.675f, s * 0.52f),
        Offset(s * 0.675f, s * 0.82f),
        stroke * 1.2f,
        StrokeCap.Round,
    )
}

// Ванна: чаша, ножки и кран.
private fun DrawScope.drawBath(tint: Color) {
    val s = size.minDimension
    val stroke = s * 0.075f

    // Кран сверху слева.
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(s * 0.26f, s * 0.20f),
        size = Size(s * 0.20f, s * 0.20f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawLine(
        tint,
        Offset(s * 0.46f, s * 0.30f),
        Offset(s * 0.46f, s * 0.44f),
        stroke,
        StrokeCap.Round,
    )

    // Чаша — полукруг со срезанным верхом.
    val bowl = Path().apply {
        addRect(Rect(Offset(s * 0.16f, s * 0.48f), Size(s * 0.68f, s * 0.26f)))
    }
    val round = Path().apply {
        addOval(Rect(Offset(s * 0.16f, s * 0.30f), Size(s * 0.68f, s * 0.62f)))
    }
    drawPath(Path().apply { op(bowl, round, PathOperation.Intersect) }, tint)

    // Ножки.
    drawRect(tint, Offset(s * 0.26f, s * 0.74f), Size(s * 0.06f, s * 0.10f))
    drawRect(tint, Offset(s * 0.68f, s * 0.74f), Size(s * 0.06f, s * 0.10f))
}

// Тележка: корзина, ручка и два колеса.
private fun DrawScope.drawCart(tint: Color) {
    val s = size.minDimension
    val stroke = s * 0.085f

    // Ручка.
    drawLine(
        tint,
        Offset(s * 0.14f, s * 0.22f),
        Offset(s * 0.30f, s * 0.22f),
        stroke,
        StrokeCap.Round,
    )

    // Корзина — трапеция.
    val basket = Path().apply {
        moveTo(s * 0.28f, s * 0.30f)
        lineTo(s * 0.88f, s * 0.30f)
        lineTo(s * 0.76f, s * 0.60f)
        lineTo(s * 0.38f, s * 0.60f)
        close()
    }
    drawPath(basket, tint)
    drawLine(
        tint,
        Offset(s * 0.28f, s * 0.24f),
        Offset(s * 0.40f, s * 0.62f),
        stroke,
        StrokeCap.Round,
    )

    drawCircle(tint, s * 0.075f, Offset(s * 0.46f, s * 0.76f))
    drawCircle(tint, s * 0.075f, Offset(s * 0.70f, s * 0.76f))
}

// Динамик: квадрат с раструбом и две дуги звука.
private fun DrawScope.drawSpeaker(tint: Color) {
    val s = size.minDimension
    val body = Path().apply {
        moveTo(s * 0.16f, s * 0.38f)
        lineTo(s * 0.32f, s * 0.38f)
        lineTo(s * 0.54f, s * 0.18f)
        lineTo(s * 0.54f, s * 0.82f)
        lineTo(s * 0.32f, s * 0.62f)
        lineTo(s * 0.16f, s * 0.62f)
        close()
    }
    drawPath(body, tint)
    drawArc(
        color = tint,
        startAngle = -55f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(s * 0.48f, s * 0.30f),
        size = Size(s * 0.28f, s * 0.40f),
        style = Stroke(width = s * 0.075f, cap = StrokeCap.Round),
    )
}

// Вопросительный знак — рисуется текстом-формой: дуга и точка.
private fun DrawScope.drawHelp(tint: Color) {
    val s = size.minDimension
    val stroke = s * 0.11f
    drawArc(
        color = tint,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(s * 0.30f, s * 0.16f),
        size = Size(s * 0.40f, s * 0.40f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawLine(
        tint,
        Offset(s * 0.50f, s * 0.50f),
        Offset(s * 0.50f, s * 0.64f),
        stroke,
        StrokeCap.Round,
    )
    drawCircle(tint, s * 0.07f, Offset(s * 0.50f, s * 0.80f))
}

// Три полосы меню. В эталоне у кнопки-бургера полосы толстые и со скруглением.
private fun DrawScope.drawMenu(tint: Color) {
    val s = size.minDimension
    val h = s * 0.10f
    for (i in 0..2) {
        drawRoundRectSolid(tint, s * 0.22f, s * 0.26f + i * s * 0.21f, s * 0.56f, h)
    }
}

// Копилка: банка с прорезью и монеткой над ней. Раньше это был круг с полосой
// поперёк — на устройстве читался как знак «проезд запрещён», а не как копилка.
private fun DrawScope.drawPiggy(tint: Color) {
    val s = size.minDimension

    // Монетка, падающая в прорезь.
    drawCircle(tint, s * 0.10f, Offset(s * 0.50f, s * 0.16f))

    // Корпус копилки — трапеция с широким низом, с вырезанной прорезью.
    //
    // Прорезь именно вырезается (Difference), а не закрашивается цветом фона:
    // кнопка под значком меняет цвет при нажатии, и закрашенная прорезь
    // осталась бы пятном прежнего фона.
    val body = Path().apply {
        moveTo(s * 0.22f, s * 0.40f)
        lineTo(s * 0.78f, s * 0.40f)
        lineTo(s * 0.84f, s * 0.84f)
        lineTo(s * 0.16f, s * 0.84f)
        close()
    }
    val slot = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = s * 0.38f,
                top = s * 0.48f,
                right = s * 0.62f,
                bottom = s * 0.55f,
                cornerRadius = CornerRadius(s * 0.035f),
            ),
        )
    }
    drawPath(Path().apply { op(body, slot, PathOperation.Difference) }, tint)
}

// Звезда о пяти лучах.
private fun DrawScope.drawStar(tint: Color) {
    val s = size.minDimension
    val cx = s / 2f
    val cy = s * 0.52f
    val outer = s * 0.36f
    val inner = outer * 0.42f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        // Начинаем с вершины: -90° — это верх.
        val a = Math.toRadians((-90f + i * 36f).toDouble())
        val x = cx + r * kotlin.math.cos(a).toFloat()
        val y = cy + r * kotlin.math.sin(a).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, tint)
}

// Кубок: чаша, ручки, ножка и подставка.
private fun DrawScope.drawTrophy(tint: Color) {
    val s = size.minDimension
    val cup = Path().apply {
        moveTo(s * 0.30f, s * 0.18f)
        lineTo(s * 0.70f, s * 0.18f)
        lineTo(s * 0.64f, s * 0.52f)
        lineTo(s * 0.36f, s * 0.52f)
        close()
    }
    drawPath(cup, tint)
    val handle = Stroke(width = s * 0.07f, cap = StrokeCap.Round)
    drawArc(
        color = tint,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(s * 0.16f, s * 0.20f),
        size = Size(s * 0.20f, s * 0.22f),
        style = handle,
    )
    drawArc(
        color = tint,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(s * 0.64f, s * 0.20f),
        size = Size(s * 0.20f, s * 0.22f),
        style = handle,
    )
    drawRect(tint, Offset(s * 0.465f, s * 0.52f), Size(s * 0.07f, s * 0.18f))
    drawRoundRectSolid(tint, s * 0.32f, s * 0.70f, s * 0.36f, s * 0.09f)
}

// Тетрадь плана: страница в рамке со строками.
//
// Контур, а не сплошная заливка с «дырками»: значок лежит на кнопке, цвет
// которой меняется при нажатии, и вырезать строки фиксированным цветом
// подложки нельзя — на нажатой кнопке они оставались бы от прежнего фона.
private fun DrawScope.drawPlan(tint: Color) {
    val s = size.minDimension
    val line = s * 0.075f
    drawRoundRect(
        color = tint,
        topLeft = Offset(s * 0.22f + line / 2f, s * 0.16f + line / 2f),
        size = Size(s * 0.56f - line, s * 0.68f - line),
        cornerRadius = CornerRadius(s * 0.10f),
        style = Stroke(width = line),
    )
    for (i in 0..2) {
        drawLine(
            tint,
            Offset(s * 0.34f, s * 0.34f + i * s * 0.14f),
            Offset(s * 0.66f, s * 0.34f + i * s * 0.14f),
            line * 0.8f,
            StrokeCap.Round,
        )
    }
}

/** Скруглённый прямоугольник сплошной заливкой — самая частая фигура в значках. */
private fun DrawScope.drawRoundRectSolid(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radius: Float = h / 2f,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun FinneyIconPreview() {
    FinneyTheme {
        Row(
            modifier = Modifier.background(FinneyCream).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FinneyIcons.entries.forEach { FinneyIcon(it, size = 30.dp) }
        }
    }
}
