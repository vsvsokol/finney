package ru.finney.pet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finney.pet.ui.motion.motionEnabled
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.StrokeThin

/**
 * Кошелёк: монета-финка с суммой плашкой в правом нижнем углу, как счётчик на значке.
 * Один на главном и в магазине, в левом верхнем углу: плейтест 28.09 — в магазине сумма
 * стояла под заголовком по центру и уезжала при прокрутке, а искали её там же, где на главном.
 *
 * [onClick] — на главном кошелёк и есть вход в магазин; в магазине он только показывает (null).
 * Сумма берётся из состояния и на экране не пересчитывается.
 *
 * Монета того же размера, что «бургер» и «✕» по краям верхнего ряда: ряд читается как
 * круги по краям и главное посередине.
 */
@Composable
fun WalletButton(
    balance: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String = if (onClick != null) "$balance финок, открыть магазин" else "У тебя $balance финок",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "coinPress")

    // «Поп», когда монет стало больше: монета вздувается и пружинит обратно,
    // над ней всплывает «+N». Какая сумма уже показана, помнит rememberSaveable:
    // он переживает уход на другой экран, и вернувшись с мини-игры или итогов
    // периода, ребёнок видит прибавку. При первом открытии и при тратах — без попа.
    var shown by rememberSaveable { mutableStateOf<Int?>(null) }
    val pop = remember { Animatable(1f) }
    val rise = remember { Animatable(0f) }
    var gain by remember { mutableIntStateOf(0) }
    LaunchedEffect(balance) {
        val before = shown
        shown = balance
        if (before == null || balance <= before) return@LaunchedEffect
        gain = balance - before
        rise.snapTo(0f)
        // Без анимаций «поп» мелькнул бы кадром, а «+N» пропал бы сразу. Прибавку
        // видно и так: «+N» стоит над монетой, пока шёл бы подъём.
        if (!motionEnabled()) {
            delay(1_100)
            gain = 0
            return@LaunchedEffect
        }
        launch {
            pop.animateTo(1.35f, tween(durationMillis = 110))
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        rise.animateTo(1f, tween(durationMillis = 1100))
        gain = 0
    }
    val scale = pressScale * pop.value
    val click = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier

    // Один контейнер размером с монету: плашка привязана к его правому нижнему
    // углу и выходит за край смещением, поэтому ряд не раздвигается от длины суммы.
    //
    // Плашка и «+N» меряются без ограничения по ширине (wrapContentSize с unbounded):
    // контейнер всего 56 dp, и раньше сумма от четырёх цифр в него не влезала —
    // maxLines = 1 молча срезал хвост числа. Длинная сумма растёт влево, по монете.
    // Предки кнопки не должны гасить её через graphicsLayer: alpha меньше 1 рисует
    // слой вне экрана и обрезает всё, что вышло за контейнер, — так сумму и
    // срезало во сне, когда верхний ряд приглушался.
    Box(
        modifier = modifier
            .size(WalletCoinSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                    } else Modifier,
                ),
        ) {
            Coin(size = WalletCoinSize)
        }
        Text(
            text = balance.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 8.dp)
                .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                .clip(RoundedCornerShape(percent = 50))
                .background(FinneySand)
                .border(StrokeThin, FinneyInk, RoundedCornerShape(percent = 50))
                .then(click)
                .padding(horizontal = 7.dp),
        )
        if (gain > 0) {
            OutlinedText(
                text = "+$gain",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-4).dp)
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        translationY = -rise.value * 28.dp.toPx()
                        // Первую половину пути видна целиком, потом тает.
                        alpha = (2f - rise.value * 2f).coerceIn(0f, 1f)
                    }
                    .clearAndSetSemantics { contentDescription = "Получено $gain финок" },
            )
        }
    }
}

/** Монета того же размера, что «бургер» и «✕». */
val WalletCoinSize = 56.dp
