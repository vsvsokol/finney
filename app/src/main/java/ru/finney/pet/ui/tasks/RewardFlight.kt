package ru.finney.pet.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.motion.motionEnabled
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.sound.Sounds
import kotlin.math.roundToInt

/**
 * Награда за игру летит монетами из плашки «+15» в кошелёк, и кошелёк досчитывает
 * по монете. Плейтест 28.09, п. 25: «деньги на счёт» одной цифрой — неинтересно.
 *
 * Сумму считает ядро, баланс уже с наградой. Экран только растягивает прибавку
 * во времени: кошелёк показывает `баланс − награда + долю долетевших`.
 */
@Stable
internal class RewardFlight(val reward: Int) {

    /** Монеток меньше, чем финок: по одной на пять, но не меньше трёх и не больше восьми. */
    val coins: Int = if (reward > 0) (reward / CoinValue).coerceIn(MinCoins, MaxCoins) else 0

    private val totalMs = if (coins == 0) 0 else (coins - 1) * StaggerMs + FlightMs
    private val clock = Animatable(0f)

    /** Плашка награды выпрыгивает перед полётом; без награды она просто стоит. */
    val chipPop = Animatable(if (coins > 0) 0f else 1f)

    /** Кошелёк подпрыгивает от каждой долетевшей монеты. */
    val walletPop = Animatable(1f)

    /** Откуда и куда лететь — центры плашки и монетки кошелька, в координатах окна. */
    var from by mutableStateOf<Offset?>(null)
    var to by mutableStateOf<Offset?>(null)

    private val landed by derivedStateOf { (0 until coins).count { phase(it) >= 1f } }

    val flying by derivedStateOf { coins > 0 && clock.value > 0f && clock.value < totalMs }

    /** Что показывает кошелёк сейчас. */
    fun wallet(balance: Int): Int =
        if (coins == 0) balance else balance - reward + reward * landed / coins

    /** Где монета [i] на пути, 0…1. Монеты вылетают друг за другом, а не пачкой. */
    fun phase(i: Int): Float = ((clock.value - i * StaggerMs) / FlightMs).coerceIn(0f, 1f)

    /**
     * Точка на дуге: монета сначала взлетает вверх и в сторону, потом сворачивает к кошельку.
     * Соседние монеты уходят в разные стороны — летит россыпь, а не бусы на нитке.
     */
    fun position(i: Int, from: Offset, to: Offset, spread: Float): Offset {
        val t = FastOutSlowInEasing.transform(phase(i))
        val side = ((i % 3) - 1) * spread
        val start = from + Offset(side * 0.5f, 0f)
        val bend = Offset(from.x + side * 2f, to.y)
        val u = 1f - t
        return start * (u * u) + bend * (2f * u * t) + to * (t * t)
    }

    /**
     * Плашка выпрыгивает, монеты летят, каждая звенит чуть выше предыдущей.
     * Без анимаций сумма сразу полная и звенит один раз: полёт в один кадр — мелькание.
     */
    suspend fun play(sounds: Sounds) = coroutineScope {
        if (coins == 0) return@coroutineScope
        if (!motionEnabled()) {
            chipPop.snapTo(1f)
            clock.snapTo(totalMs.toFloat())
            delay(CoinDelayMs)
            sounds.play(Sfx.Coin)
            return@coroutineScope
        }
        launch { chipPop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        delay(CoinDelayMs)
        launch {
            // До последней монеты: дальше считать нечего.
            snapshotFlow { landed }.drop(1).transformWhile { emit(it); it < coins }.collect { n ->
                sounds.play(Sfx.Coin, 1f + 0.08f * (n - 1))
                this@coroutineScope.launch {
                    walletPop.snapTo(1.18f)
                    walletPop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                }
            }
        }
        clock.animateTo(totalMs.toFloat(), tween(totalMs, easing = LinearEasing))
    }
}

/** Слой с летящими монетами — поверх всей сцены, чтобы долететь до кошелька в верхней полосе. */
@Composable
internal fun RewardCoins(flight: RewardFlight, modifier: Modifier = Modifier) {
    val from = flight.from
    val to = flight.to
    if (from == null || to == null || !flight.flying) return
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val half = with(density) { FlyingCoin.toPx() / 2f }
    val spread = with(density) { 36.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clearAndSetSemantics {},
    ) {
        repeat(flight.coins) { i ->
            Coin(
                size = FlyingCoin,
                modifier = Modifier
                    .offset {
                        val p = flight.position(i, from - origin, to - origin, spread)
                        IntOffset((p.x - half).roundToInt(), (p.y - half).roundToInt())
                    }
                    .graphicsLayer {
                        val ph = flight.phase(i)
                        // Ещё не вылетела или уже в кошельке — не видна.
                        alpha = if (ph <= 0f || ph >= 1f) 0f else 1f
                        // У кошелька монета чуть меньше: она «входит» в его монетку.
                        val s = 1.15f - 0.35f * ph
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

private val FlyingCoin = 30.dp

private const val CoinValue = 5
private const val MinCoins = 3
private const val MaxCoins = 8

/** Монеты вылетают, когда джингл итога почти отыграл. */
private const val CoinDelayMs = 900L
private const val StaggerMs = 110
private const val FlightMs = 650
