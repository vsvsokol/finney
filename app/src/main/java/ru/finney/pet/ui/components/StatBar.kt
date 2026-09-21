package ru.finney.pet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.StrokeRegular
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Шкала потребности из макета: вертикальная капсула с обводкой, заполняется снизу.
// Рядом всегда подпись и значок — по ТЗ п. 3.6 цвет не может быть единственным
// способом показать состояние, поэтому низкое значение видно и без различения цветов.
//
// Заливка плоская. Здесь был единственный во всём проекте градиент
// (жёлтый → персиковый), и он спорил с макетом: в эталоне
// у шкалы ровный персиковый столбик без перехода.

private val OutlineWidth = StrokeRegular

/** Порог «низко»: ниже него шкала краснеет и подписывается явно. Совпадает с needsThreshold из economy.json. */
private const val LOW = 50

/**
 * Высота лежачей капсулы в [StatPill].
 *
 * 26, а не 18: при 18 на обводку уходило по 3 сверху и снизу, внутри оставалось
 * 12, и заполнение, обрезанное капсулой, вырождалось в незаметную полоску —
 * на устройстве шкала читалась как пустая.
 */
private val PillHeight = 26.dp

/**
 * Вертикальная шкала 0..100.
 *
 * [label] — что за шкала: «сытость», «чистота», «настроение». Она же подпись
 * под капсулой. Значение и признак «мало» уходят в TalkBack одной фразой.
 */
@Composable
fun StatBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    width: Dp = 36.dp,
) {
    val clamped = value.coerceIn(0, 100)
    val fraction by animateFloatAsState(clamped / 100f, label = "stat")
    val isLow = clamped < LOW

    Column(
        modifier = modifier.semantics {
            contentDescription = "$label $clamped из 100" + if (isLow) ", мало" else ""
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(CircleShape)
                .background(FinneyYellow)
                .border(OutlineWidth, FinneyInk, CircleShape)
                .padding(OutlineWidth),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(CircleShape)
                    .background(if (isLow) FinneyPink else FinneyPeach),
            )
        }

        // Подпись обязательна: она и есть тот «не цвет», которым различается состояние.
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FinneyInk,
        )
    }
}

/**
 * Компактная шкала: лежачая капсула со значком слева.
 *
 * Для главного экрана, где место занимает питомец, а не приборы. Три вертикальные
 * шкалы с подписями читались как панель управления и спорили с питомцем за
 * внимание; здесь то же самое умещается в узкую полосу над сценой.
 *
 * [icon] — значок вместо подписи: на главном экране роль шкалы понятна
 * по нему, а полное название уходит в TalkBack. По ТЗ п. 3.6 цвет остаётся
 * не единственным признаком: при низком значении рядом со значком загорается «!».
 */
@Composable
fun StatPill(
    label: String,
    icon: FinneyIcons,
    value: Int,
    modifier: Modifier = Modifier,
) {
    val clamped = value.coerceIn(0, 100)
    val fraction by animateFloatAsState(clamped / 100f, label = "stat")
    val isLow = clamped < LOW

    Row(
        modifier = modifier.semantics {
            contentDescription = "$label $clamped из 100" + if (isLow) ", мало" else ""
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Значок и «!» — тот самый не-цветовой признак: и значок, и восклицательный
        // знак видно, даже если цвета не различаются.
        FinneyIcon(icon, size = 20.dp)
        if (isLow) {
            Text(
                text = "!",
                style = MaterialTheme.typography.labelLarge,
                color = FinneyInk,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(PillHeight)
                .clip(CircleShape)
                .background(FinneyYellow)
                .border(OutlineWidth, FinneyInk, CircleShape)
                // Отступ на обводку задаётся контейнеру: если навесить его на
                // само заполнение, padding сначала срежет доступную ширину,
                // и fillMaxWidth(fraction) посчитает долю уже от неё — полоса
                // схлопывалась в ноль и шкала выглядела пустой.
                .padding(OutlineWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (isLow) FinneyPink else FinneyPeach),
            )
        }
    }
}

/**
 * Шкала счастья из эталона: вертикальная капсула с кружком-«лицом» у основания.
 *
 * Лицо — не украшение, а второй, нецветовой способ прочитать состояние
 * (ТЗ п. 3.6): улыбка переворачивается в грусть, когда значение падает ниже
 * [LOW]. Ребёнок видит настроение питомца, даже не различая цвета.
 */
@Composable
fun HappinessBar(
    value: Int,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    width: Dp = 44.dp,
) {
    val clamped = value.coerceIn(0, 100)
    val fraction by animateFloatAsState(clamped / 100f, label = "happiness")
    val isLow = clamped < LOW
    val faceSize = width * 1.5f

    Column(
        modifier = modifier.semantics {
            contentDescription = "настроение $clamped из 100" + if (isLow) ", мало" else ""
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(CircleShape)
                .background(FinneyYellow)
                .border(StrokeRegular, FinneyInk, CircleShape),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(CircleShape)
                    .background(if (isLow) FinneyPink else FinneyPeach),
            )
        }

        // Кружок налезает на низ капсулы — в эталоне он перекрывает её край.
        Box(
            modifier = Modifier
                .offset(y = -faceSize / 3f)
                .size(faceSize)
                .clip(CircleShape)
                .background(FinneyYellow)
                .border(StrokeRegular, FinneyInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(faceSize * 0.55f)) {
                val s = size.minDimension
                val eye = s * 0.09f
                drawCircle(FinneyInk, eye, Offset(s * 0.30f, s * 0.32f))
                drawCircle(FinneyInk, eye, Offset(s * 0.70f, s * 0.32f))
                // Рот: дуга вниз — улыбка, вверх — грусть.
                drawArc(
                    color = FinneyInk,
                    startAngle = if (isLow) 200f else 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(s * 0.26f, if (isLow) s * 0.62f else s * 0.42f),
                    size = Size(s * 0.48f, s * 0.34f),
                    style = Stroke(width = s * 0.10f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun StatBarPreview() {
    FinneyTheme {
        Row(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatBar(label = "сытость", value = 80)
            StatBar(label = "чистота", value = 30)
            HappinessBar(value = 70)
            HappinessBar(value = 20)
        }
    }
}
