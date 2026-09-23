package ru.finney.pet.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.ProgressRing
import ru.finney.pet.ui.theme.FinneyGlare
import ru.finney.pet.ui.theme.FinneyInk
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Мытьё как в «Говорящем Томе»: мылом водят по питомцу, и там, где провели,
// остаётся пена. Намылил почти всего — пена лопается, питомец чистый.
//
// «Почти всего» считается по сетке над питомцем (SCRUB_COLS × SCRUB_ROWS):
// клетка засчитана, если по ней хоть раз провели мылом. Сетка, а не число
// пузырей, — иначе можно тереть одно место, и оно засчиталось бы за всё.

private const val SCRUB_COLS = 5
private const val SCRUB_ROWS = 4
private const val SCRUB_DONE = 0.7f

/** Как часто в пути мыла вырастает пена, dp. Чаще — пена сливается в кашу. */
private const val FOAM_STEP = 12f

/** Больше пузырей не держим: столько хватает накрыть питомца целиком. */
private const val FOAM_MAX = 220

private class Bubble(val center: Offset, val radius: Float, val bornMs: Long, val phase: Float)

/** Мелкий пузырь, который отрывается от пены и улетает вверх. */
private class Floater(val start: Offset, val radius: Float, val bornMs: Long, val drift: Float)

private const val FLOATER_LIFE_MS = 1400L
private const val BUBBLE_GROW_MS = 180L
private const val POP_MS = 420L

@Stable
private class WashState {
    val bubbles = mutableStateListOf<Bubble>()
    val floaters = mutableStateListOf<Floater>()
    val scrubbed = mutableSetOf<Int>()
    var progress by mutableFloatStateOf(0f)
    var soap by mutableStateOf(Offset.Unspecified)
    var tilt by mutableFloatStateOf(0f)
    var holding by mutableStateOf(false)
    var nowMs by mutableLongStateOf(0L)
    var poppedAtMs by mutableLongStateOf(-1L)
    var lastFoam = Offset.Unspecified

    /** Сколько клеток уже намылено — для подписи TalkBack. */
    var scrubbedCount by mutableIntStateOf(0)

    fun soapAt(rest: Offset): Offset = if (soap.isSpecified) soap else rest
}

/**
 * [pet] — рамка питомца в координатах корня. Намыливается не вся она,
 * а видимая часть: в ванне ниже пояса питомца закрывает борт.
 * [onScrub] — новая намыленная клетка: питомцу щекотно.
 */
@Composable
fun WashingGame(
    itemId: String,
    pet: Rect,
    onScrub: () -> Unit,
    onClean: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val wash = remember { WashState() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentOnClean by rememberUpdatedState(onClean)
    val soapScale = remember { Animatable(1f) }

    // Часы пены: пузыри дышат и растут, мелкие улетают. Идут, пока игра на экране.
    LaunchedEffect(wash) {
        while (true) withFrameNanos { wash.nowMs = it / 1_000_000 }
    }

    CareGameFrame(hint = "Намыль питомца", onCancel = onCancel, modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { origin = it.positionInRoot() },
        ) {
            val px = with(density) { 1.dp.toPx() }
            val size = with(density) { ItemSize.toPx() }
            val rest = Offset(constraints.maxWidth / 2f, constraints.maxHeight - with(density) { ItemRestFromBottom.toPx() })
            // Видимая часть питомца: голова и плечи. Поля по бокам — пустой холст
            // вокруг силуэта, мылить там воздух неинтересно.
            val area = Rect(
                left = pet.left + pet.width * 0.14f - origin.x,
                top = pet.top + pet.height * 0.10f - origin.y,
                right = pet.right - pet.width * 0.14f - origin.x,
                bottom = pet.top + pet.height * 0.72f - origin.y,
            )

            fun finish() {
                if (wash.poppedAtMs >= 0) return
                wash.poppedAtMs = wash.nowMs
                scope.launch {
                    soapScale.animateTo(0f, tween(durationMillis = 200))
                    delay(POP_MS)
                    currentOnClean()
                }
            }

            fun scrub(at: Offset) {
                if (wash.poppedAtMs >= 0 || !area.contains(at)) return
                val last = wash.lastFoam
                if (last.isSpecified && (at - last).getDistance() < FOAM_STEP * px) return
                wash.lastFoam = at

                repeat(Random.nextInt(1, 3)) {
                    if (wash.bubbles.size >= FOAM_MAX) wash.bubbles.removeAt(0)
                    wash.bubbles += Bubble(
                        center = at + Offset(Random.nextFloat() - 0.5f, Random.nextFloat() - 0.5f) * 28f * px,
                        radius = (7f + Random.nextFloat() * 11f) * px,
                        bornMs = wash.nowMs,
                        phase = Random.nextFloat() * 2f * PI.toFloat(),
                    )
                }
                if (Random.nextFloat() < 0.35f) {
                    wash.floaters += Floater(at, (3f + Random.nextFloat() * 5f) * px, wash.nowMs, Random.nextFloat() - 0.5f)
                }
                wash.floaters.removeAll { wash.nowMs - it.bornMs > FLOATER_LIFE_MS }

                val col = ((at.x - area.left) / area.width * SCRUB_COLS).toInt().coerceIn(0, SCRUB_COLS - 1)
                val row = ((at.y - area.top) / area.height * SCRUB_ROWS).toInt().coerceIn(0, SCRUB_ROWS - 1)
                if (wash.scrubbed.add(row * SCRUB_COLS + col)) {
                    wash.scrubbedCount = wash.scrubbed.size
                    wash.progress = wash.scrubbed.size / (SCRUB_COLS * SCRUB_ROWS).toFloat()
                    currentOnScrub()
                    if (wash.progress >= SCRUB_DONE) finish()
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val now = wash.nowMs
                val pop = if (wash.poppedAtMs < 0) 0f else ((now - wash.poppedAtMs) / POP_MS.toFloat()).coerceIn(0f, 1f)
                wash.bubbles.forEachIndexed { index, bubble ->
                    // Лопаются не хором: каждый пузырь со своей задержкой.
                    val own = ((pop - (index % 7) * 0.06f) / 0.6f).coerceIn(0f, 1f)
                    if (own >= 1f) return@forEachIndexed
                    val grow = ((now - bubble.bornMs) / BUBBLE_GROW_MS.toFloat()).coerceIn(0f, 1f)
                    val breathe = 1f + 0.06f * sin(now / 380f + bubble.phase)
                    val r = bubble.radius * grow * breathe * (1f + own * 0.5f)
                    drawBubble(bubble.center, r, alpha = 1f - own)
                }
                wash.floaters.forEach { floater ->
                    val t = (now - floater.bornMs) / FLOATER_LIFE_MS.toFloat()
                    if (t !in 0f..1f) return@forEach
                    val rise = t * 140f * density.density
                    val sway = sin(t * 9f + floater.drift * 6f) * 10f * density.density
                    drawBubble(
                        center = floater.start + Offset(sway + floater.drift * 30f * density.density, -rise),
                        radius = floater.radius * (1f + t * 0.4f),
                        alpha = 1f - t,
                    )
                }
            }

            ItemPicture(
                itemId = itemId,
                fallback = FinneyIcons.Bath,
                size = ItemSize,
                modifier = Modifier
                    .offset {
                        val p = wash.soapAt(rest)
                        IntOffset((p.x - size / 2).roundToInt(), (p.y - size / 2).roundToInt())
                    }
                    .graphicsLayer {
                        val scale = soapScale.value * if (wash.holding) 1.1f else 1f
                        scaleX = scale
                        scaleY = scale
                        rotationZ = wash.tilt
                    }
                    .semantics {
                        contentDescription = "Мыло. Води им по питомцу"
                        // Для TalkBack натирание заменяется нажатием: питомец сразу чистый.
                        onClick(label = "Помыть") { finish(); true }
                    },
            )

            // Мыло прыгает под палец, где бы его ни коснулись: искать его на
            // экране ребёнку не нужно, сразу можно тереть.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(rest, area) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (wash.poppedAtMs >= 0) return@awaitEachGesture
                            wash.holding = true
                            wash.soap = down.position
                            wash.lastFoam = Offset.Unspecified
                            scrub(down.position)
                            drag(down.id) { change ->
                                val step = change.position - change.previousPosition
                                wash.soap = change.position
                                wash.tilt = (step.x * 1.5f).coerceIn(-20f, 20f)
                                scrub(change.position)
                                change.consume()
                            }
                            wash.holding = false
                            wash.tilt = 0f
                            // Отпустил — мыло возвращается на место.
                            val from = wash.soapAt(rest)
                            scope.launch {
                                val back = Animatable(0f)
                                back.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) {
                                    if (!wash.holding) wash.soap = from + (rest - from) * value
                                }
                            }
                        }
                    },
            )

            // Сколько осталось: кольцо вокруг значка ванны под подсказкой.
            // Цифр нет — ребёнку хватает «кольцо почти замкнулось».
            ProgressRing(
                diameter = 44.dp,
                progress = (wash.progress / SCRUB_DONE).coerceAtMost(1f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 150.dp)
                    .semantics { contentDescription = "Намылено ${wash.scrubbedCount} из ${(SCRUB_COLS * SCRUB_ROWS * SCRUB_DONE).roundToInt()}" },
            ) {
                FinneyIcon(FinneyIcons.Bath, size = 28.dp)
            }
        }
    }
}

/**
 * Пузырь пены в стиле кита: обводка ink и непрозрачный белый блик слева сверху.
 * Заливка полупрозрачная — сквозь пену чуть видно питомца, и блик на ней читается.
 * Обводка тоньше, чем 1/12 диаметра, иначе мелкие пузыри становятся кляксами.
 */
private fun DrawScope.drawBubble(center: Offset, radius: Float, alpha: Float) {
    if (radius <= 0f || alpha <= 0f) return
    drawCircle(FinneyGlare, radius, center, alpha = alpha * 0.75f)
    drawCircle(FinneyInk, radius, center, alpha = alpha, style = Stroke(width = maxOf(1.5f, radius * 0.14f)))
    // Размер и место блика — как у кнопок кита, в долях диаметра.
    drawOval(
        color = FinneyGlare,
        alpha = alpha,
        topLeft = center + Offset(-radius * 0.62f, -radius * 0.62f),
        size = Size(radius * 0.57f, radius * 0.4f),
    )
}
