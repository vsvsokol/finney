package ru.finney.pet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.theme.StrokeThin

// Картинка рядом с числом «10 из 50». Плейтест показал, что голые дроби дети
// пролистывают: полоса или кольцо читаются с одного взгляда. Заливка жёлтая —
// цвет денег и достижений; полное отмечено «✓» (ТЗ п. 3.6: длина понятна и без цвета).
//
// Раньше цвет шёл от розового к зелёному, и копилка в начале уровня была розовой,
// как ошибка, а полная — зелёной, как «получилось»; рядом полоса нужного была жёлтой.
// На плейтесте спросили, почему нужное жёлтое, а копилка зелёная. Розовый и зелёный
// у нас — только итог действия, а «сколько накоплено» — не итог.
//
// Два вида шкал не путать. [FillBar] и [FillRing] — прогресс: копилка, уровень,
// верные ответы, где больше — лучше. [SpendBar] — траты по плану: там больше не
// значит лучше, и зелёного у неё нет.

/** Пустая часть шкалы — светлее кремового фона, чтобы заливка не сливалась с ним. */
private val Track = Color(0xFFFFF7EA)

/** Цвет заливки. Один при любой доле — см. комментарий в начале файла; доля оставлена для вызовов. */
@Suppress("UNUSED_PARAMETER")
fun fillColor(fraction: Float): Color = FinneyYellow

/** Доля [value] от [max]. Когда из нуля ничего не набрано, шкала пустая, а не полная. */
private fun fractionOf(value: Int, max: Int): Float = when {
    max > 0 -> value.toFloat() / max
    value > 0 -> 1f
    else -> 0f
}

/**
 * Полоса прогресса: [value] из [max]. Полная — с «✓» в конце.
 * Подпись для TalkBack — [description]; без неё полоса читается как «value из max».
 *
 * [pending] — превью того, что вот-вот изменится, до нажатия. Показано штриховкой,
 * а не бледным цветом: «ещё не случилось» должно читаться и без цвета (ТЗ п. 3.6).
 * - [removing] = false: штрихи «/» продолжают заливку вправо — столько добавится;
 * - [removing] = true: штрихи крест-накрест закрывают правый край заливки — столько уйдёт.
 * Рядом подпись «+15» или «−15»: знак даёт направление словом, а не оттенком.
 * [previews] — полоса бывает с превью: тогда место под подпись держится всегда,
 * даже когда превью нет, и полоса не прыгает, пока выбирают сумму.
 */
@Composable
fun FillBar(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    pending: Int = 0,
    removing: Boolean = false,
    height: Dp = 14.dp,
    description: String? = null,
    previews: Boolean = pending > 0,
) {
    val fraction = fractionOf(value, max)
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "fill")
    // Превью едет той же пружиной, что и заливка: при смене суммы полоса растёт
    // и убывает плавно, а не прыгает.
    val extra by animateFloatAsState(fractionOf(pending, max).coerceIn(0f, 1f), label = "pending")
    val spoken = description ?: "$value из $max"
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = when {
                pending <= 0 -> spoken
                removing -> "$spoken, уйдёт $pending"
                else -> "$spoken, добавится $pending"
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            Modifier
                .weight(1f)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Track)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(50)),
        ) {
            if (removing) {
                // Уходящая часть — с конца заливки: что останется, залито сплошным.
                val cut = extra.coerceAtMost(shown)
                val kept = size.width * (shown - cut)
                drawRect(fillColor(shown - cut), size = size.copy(width = kept))
                hatch(from = kept, to = size.width * shown, underlay = Track, cross = true)
            } else {
                val done = size.width * shown
                drawRect(fillColor(shown), size = size.copy(width = done))
                val plus = extra.coerceAtMost(1f - shown)
                // Подложка — цветом того, что будет после добавки, но бледно: цвет лишь помогает.
                hatch(from = done, to = done + size.width * plus, underlay = fillColor(shown + plus).copy(alpha = 0.35f), cross = false)
            }
        }
        // «+15» и «✓» появляются и пропадают, а полоса от этого ширины не меняет:
        // место под них отмерено заранее — под самую длинную подпись «+$max».
        val badge = height + 10.dp
        ReserveWidth(
            *(if (previews) arrayOf("−$max") else emptyArray()),
            sample = { PendingTag(it) },
            modifier = Modifier.widthIn(min = badge),
            contentAlignment = Alignment.CenterStart,
        ) {
            when {
                pending > 0 -> PendingTag(if (removing) "−$pending" else "+$pending")
                fraction >= 1f -> CheckBadge(size = badge)
            }
        }
    }
}

/** Шаг штриховки и толщина штриха. Плотно, чтобы полоса читалась как «штриховка», а не как пунктир. */
private val HatchGap = 5.dp
private val HatchWidth = 1.5.dp

/**
 * Штриховка отрезка полосы [from]..[to] по x: подложка, линии цветом ink под 45°
 * и черта на границе. [cross] — вторые линии навстречу первым: «уйдёт» отличается
 * от «добавится» рисунком, а не цветом. Обрезается по отрезку и по форме полосы
 * (форму режет clip у Canvas).
 */
private fun DrawScope.hatch(from: Float, to: Float, underlay: Color, cross: Boolean) {
    if (to - from < 0.5f) return
    val gap = HatchGap.toPx()
    val stroke = HatchWidth.toPx()
    val h = size.height
    clipRect(left = from, right = to) {
        drawRect(underlay, topLeft = Offset(from, 0f), size = Size(to - from, h))
        // Линии заводятся от from − h, чтобы у левого края отрезка не было пустого угла.
        var x = from - h
        while (x < to) {
            drawLine(FinneyInk, Offset(x, h), Offset(x + h, 0f), stroke)
            if (cross) drawLine(FinneyInk, Offset(x, 0f), Offset(x + h, h), stroke)
            x += gap
        }
    }
    // Граница «что есть | что изменится» — сплошной чертой.
    drawLine(FinneyInk, Offset(from, 0f), Offset(from, h), stroke * 1.5f)
}

/** Подпись превью у полосы: «+15» или «−15». Знак несёт смысл, фон — только рамка. */
@Composable
private fun PendingTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = FinneyInk,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(50))
            .padding(horizontal = 8.dp),
    )
}

/**
 * Кольцо прогресса вокруг значка — для тесных мест вроде плашки на главном.
 * Дуга растёт по часовой от верха, как кольца потребностей.
 */
@Composable
fun FillRing(
    value: Int,
    max: Int,
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val shown by animateFloatAsState(fractionOf(value, max).coerceIn(0f, 1f), label = "ring")
    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                val track = size.minDimension * 0.16f
                val radius = (size.minDimension - track) / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                drawCircle(Track, radius, style = Stroke(track))
                drawArc(
                    color = fillColor(shown),
                    startAngle = -90f,
                    sweepAngle = 360f * shown,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(track, cap = StrokeCap.Round),
                )
                drawCircle(FinneyInk, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(StrokeThin.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Траты из плана: [spent] из [planned]. Заливка жёлтая, как деньги, — «потратил
 * больше» здесь не успех. Вышел за план — полоса полная, розовая, и «!» с перебором
 * числом: перерасход видно не только цветом (ТЗ п. 3.6).
 */
@Composable
fun SpendBar(spent: Int, planned: Int, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val over = spent - planned
    val shown by animateFloatAsState(fractionOf(spent, planned).coerceIn(0f, 1f), label = "spend")
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = if (over > 0) "$spent из $planned, больше плана на $over" else "$spent из $planned"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            Modifier
                .weight(1f)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Track)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(50)),
        ) {
            drawRect(if (over > 0) FinneyPink else FinneyYellow, size = size.copy(width = size.width * shown))
        }
        if (over > 0) AlertBadge(size = height + 10.dp)
    }
}

/** Круглый «✓» на зелёном: готово, выбрано, полно. Один на все экраны. */
@Composable
fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Badge("✓", FinneyGreen, size, modifier)
}

/** Круглый «!» на розовом: не хватает, сверх плана, ошибка. */
@Composable
fun AlertBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Badge("!", FinneyPink, size, modifier)
}

/**
 * Круглый «!» на голубом: предупреждение — ещё не ошибка, но так дальше нельзя
 * (разложено больше, чем есть). Розовый [AlertBadge] — для того, что уже не вышло.
 */
@Composable
fun WarningBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Badge("!", FinneyBlue, size, modifier)
}

@Composable
private fun Badge(mark: String, color: Color, size: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(StrokeThin, FinneyInk, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // Кегль от размера кружка: это значок, знак обязан в него влезать.
        Text(mark, style = MaterialTheme.typography.labelLarge.copy(fontSize = (size.value * 0.62f).sp, lineHeight = (size.value * 0.8f).sp), color = FinneyInk)
    }
}

/**
 * Точки по порядку вместо «1 из 3»: закрашено [done] из [total]. [current] — где
 * ребёнок сейчас (с 0): эта точка крупнее и с обводкой толще.
 */
@Composable
fun StepDots(done: Int, total: Int, modifier: Modifier = Modifier, current: Int? = null, dot: Dp = 12.dp) {
    val spoken = if (current != null) current + 1 else done
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$spoken из $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val here = current == i
            Box(
                Modifier
                    .size(if (here) dot + 4.dp else dot)
                    .clip(CircleShape)
                    .background(if (i < done || here) FinneyYellow else FinneyCream)
                    .border(if (here) StrokeRegular else StrokeThin, FinneyInk, CircleShape),
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun GaugePreview() {
    FinneyTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FillBar(5, 50, Modifier.fillMaxWidth())
            FillBar(25, 50, Modifier.fillMaxWidth(), pending = 10)
            FillBar(25, 50, Modifier.fillMaxWidth(), pending = 10, removing = true)
            FillBar(20, 50, Modifier.fillMaxWidth(), pending = 15, height = 22.dp)
            FillBar(50, 50, Modifier.fillMaxWidth())
            SpendBar(15, 20, Modifier.fillMaxWidth())
            SpendBar(25, 20, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FillRing(10, 50, 36.dp) { FinneyIcon(FinneyIcons.Piggy, size = 20.dp) }
                FillRing(50, 50, 36.dp) { FinneyIcon(FinneyIcons.Piggy, size = 20.dp) }
            }
            StepDots(done = 0, total = 3, current = 1)
        }
    }
}
