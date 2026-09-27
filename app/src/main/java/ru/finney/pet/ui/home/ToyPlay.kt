package ru.finney.pet.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.finney.pet.R
import ru.finney.pet.ui.components.HeartIcon
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import kotlin.math.roundToInt
import kotlin.random.Random

// Игра с игрушкой на главном: сердечки над питомцем и подсказка к шкале настроения.
// Обе — без слов: ребёнок 7 лет видит, что радость растёт, и видит, от чего.

/** Одно сердечко над питомцем. [filled] false — питомец наигрался, радости не прибавилось. */
internal data class FloatingHeart(val id: Long, val from: Offset, val filled: Boolean)

private const val HEART_RISE_MS = 1_100

/** Сердечки, всплывающие над питомцем, пока с ним играют. */
@Stable
class HeartBurst {
    private var next = 0L
    internal val hearts = mutableStateListOf<FloatingHeart>()

    /** Выпустить сердечко из точки [from] (координаты экрана). */
    fun emit(from: Offset, filled: Boolean) {
        if (hearts.size > 12) return
        hearts += FloatingHeart(next++, from, filled)
    }

    internal fun remove(heart: FloatingHeart) {
        hearts -= heart
    }
}

@Composable
fun rememberHeartBurst(): HeartBurst = remember { HeartBurst() }

/** Слой сердечек поверх всего экрана. Нажатия не ловит. */
@Composable
fun HeartBurstLayer(burst: HeartBurst) {
    val density = LocalDensity.current
    val half = with(density) { 14.dp.toPx() }
    val rise = with(density) { 90.dp.toPx() }
    Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        for (heart in burst.hearts) {
            key(heart.id) {
                val t = remember { Animatable(0f) }
                val drift = remember { (Random.nextFloat() - 0.5f) * half * 3 }
                LaunchedEffect(Unit) {
                    t.animateTo(1f, tween(HEART_RISE_MS, easing = LinearEasing))
                    burst.remove(heart)
                }
                HeartIcon(
                    filled = heart.filled,
                    size = 28.dp,
                    modifier = Modifier
                        .offset { IntOffset((heart.from.x - half).roundToInt(), (heart.from.y - half).roundToInt()) }
                        .graphicsLayer {
                            val p = t.value
                            translationX = drift * p
                            translationY = -rise * FastOutSlowInEasing.transform(p)
                            alpha = if (p < 0.7f) 1f else (1f - p) / 0.3f
                            val s = 0.6f + 0.4f * minOf(1f, p * 4f)
                            scaleX = s
                            scaleY = s
                        },
                )
            }
        }
    }
}

/**
 * Подсказка к шкале настроения — картинками, без слов: игрушка, конфета, шляпа
 * поднимают сердечко. Это ровно то, что растит настроение в игре: игра с игрушкой,
 * «хочется» из магазина и шляпы (покупка и цель).
 */
@Composable
fun MoodHint(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FinneyCream)
            .border(2.dp, FinneyInk, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clearAndSetSemantics {
                contentDescription = "Настроение поднимают игрушки, сладости и шляпы"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (art in listOf(R.drawable.item_toy_ball, R.drawable.item_treat_candy, R.drawable.acc_hat_cowboy)) {
            Image(painter = painterResource(art), contentDescription = null, modifier = Modifier.size(34.dp))
        }
        Text("→", style = MaterialTheme.typography.titleLarge, color = FinneyInk)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("↑", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            HeartIcon(filled = true, size = 30.dp)
        }
    }
}
