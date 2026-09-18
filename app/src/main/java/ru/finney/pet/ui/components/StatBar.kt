package ru.finney.pet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Шкала потребности из макета: вертикальная капсула с обводкой, заполняется снизу.
// Рядом всегда подпись и значок — по ТЗ п. 3.6 цвет не может быть единственным
// способом показать состояние, поэтому низкое значение видно и без различения цветов.

private val OutlineWidth = 3.dp

/** Порог «низко»: ниже него шкала краснеет и подписывается явно. Совпадает с needsThreshold из economy.json. */
private const val LOW = 50

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
                .background(FinneySand)
                .border(OutlineWidth, FinneyInk, CircleShape),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            if (isLow) listOf(FinneyPink, FinneyPink) else listOf(FinneyYellow, FinneyPeach),
                        ),
                    ),
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

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun StatBarPreview() {
    FinneyTheme {
        Row(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatBar(label = "сытость", value = 80)
            StatBar(label = "чистота", value = 30)
            StatBar(label = "настроение", value = 55)
        }
    }
}
