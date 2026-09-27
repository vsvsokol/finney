package ru.finney.pet.ui.goals

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.strokeFor

// «Положить» и «Взять» — две стороны одного движения, и выглядят они так же:
// одна капсула, разрезанная пополам, а посередине копилка. Раньше это были две
// одинаковые кнопки одна под другой, и на плейтесте их жали наугад.
//
// Направление несут стрелка и слово, а цвет только помогает (ТЗ п. 3.6):
// ↓ «Положить» — монеты падают в копилку, половина зелёная, как «успех»;
// ↑ «Взять» — достаёшь наружу, половина розовая: можно, но до цели станет дальше.
// Сумма одна на обе — та, что набрана «−/+» над кнопками, в подписи её нет.

private val SwitchHeight = 64.dp
private val PiggySize = 64.dp

@Composable
internal fun SavingsSwitch(
    amount: Int,
    onTake: () -> Unit,
    onPut: () -> Unit,
    canTake: Boolean,
    canPut: Boolean,
    modifier: Modifier = Modifier,
) {
    val stroke = strokeFor(SwitchHeight)
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(SwitchHeight)
                .clip(RoundedCornerShape(percent = 50))
                .border(stroke, FinneyInk, RoundedCornerShape(percent = 50)),
        ) {
            Half(
                label = "Взять",
                arrow = "↑",
                arrowFirst = true,
                description = "Взять $amount из копилки",
                color = FinneyPink,
                enabled = canTake,
                sound = Sfx.Tap,
                shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                onClick = onTake,
            )
            // Разрез между половинами — той же обводкой, что вокруг.
            Box(Modifier.fillMaxHeight().width(stroke).background(FinneyInk))
            Half(
                label = "Положить",
                arrow = "↓",
                arrowFirst = false,
                description = "Положить $amount в копилку",
                color = FinneyGreen,
                enabled = canPut,
                sound = Sfx.Tap,
                shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                onClick = onPut,
            )
        }
        // Копилка на стыке — общее у обеих половин.
        Box(
            Modifier
                .size(PiggySize)
                .clip(CircleShape)
                .background(FinneyCream)
                .border(stroke, FinneyInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) { FinneyIcon(FinneyIcons.Piggy, size = 36.dp) }
    }
}

@Composable
private fun RowScope.Half(
    label: String,
    arrow: String,
    arrowFirst: Boolean,
    description: String,
    color: Color,
    enabled: Boolean,
    sound: Sfx,
    shape: Shape,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(color)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { sounds.play(sound); onClick() },
            )
            // Погасшая половина бледнеет вся: и цвет, и слово — не только оттенок.
            .alpha(if (enabled) 1f else 0.4f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Отступ со стороны копилки — чтобы надпись не уходила под неё.
            .padding(
                start = if (arrowFirst) 12.dp else PiggySize / 2 + 4.dp,
                end = if (arrowFirst) PiggySize / 2 + 4.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        if (arrowFirst) OutlinedText(arrow, style = MaterialTheme.typography.titleLarge)
        OutlinedText(label, style = MaterialTheme.typography.titleLarge, fill = FinneyCream)
        if (!arrowFirst) OutlinedText(arrow, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun SavingsSwitchPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SavingsSwitch(amount = 15, onTake = {}, onPut = {}, canTake = true, canPut = true)
            SavingsSwitch(amount = 15, onTake = {}, onPut = {}, canTake = false, canPut = true)
        }
    }
}
