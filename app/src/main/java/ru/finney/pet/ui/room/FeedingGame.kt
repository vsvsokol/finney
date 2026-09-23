package ru.finney.pet.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.finney.pet.ui.components.FinneyIcons
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

// Кормление как в «Говорящем Томе»: еду тянут пальцем и бросают питомцу в рот.
//
// Физика простая, плоская: бросок задаёт скорость, дальше тянет вниз сила
// тяжести. Ребёнку 7 лет в рот размером с ноготь не попасть, поэтому рядом
// со ртом еду подтягивает к нему (MAGNET_*), а сам рот ловит с запасом.
// Кто не умеет бросать, может просто донести еду до рта — это тоже считается.

private enum class FoodPhase { REST, DRAG, FLY, EATEN }

/** Всё, что меняется каждый кадр. Читается в layout и draw, не в композиции. */
@Stable
private class FoodState {
    var phase by mutableStateOf(FoodPhase.REST)
    var position by mutableStateOf(Offset.Unspecified)
    var rotation by mutableFloatStateOf(0f)
    val scale = Animatable(1f)
    var velocity = Offset.Zero

    /** До первого касания еда лежит на месте; где это место, знает только разметка. */
    fun at(rest: Offset): Offset = if (position.isSpecified) position else rest
}

// Скорости в dp/с, ускорения в dp/с². Подобраны так, чтобы с места над
// кнопками до рта хватало уверенного взмаха, а не броска изо всех сил.
private const val GRAVITY = 2400f
private const val MIN_THROW = 500f
private const val MAX_THROW = 3000f
private const val MAGNET_RADIUS = 150f
private const val MAGNET_PULL = 9000f
private const val SPIN = 240f

/**
 * [pet] и [mouth] — в координатах корня: их меряет главный экран на самом
 * питомце. [onMouthOpen] открывает рот навстречу еде и закрывает при промахе.
 */
@Composable
fun FeedingGame(
    itemId: String,
    pet: Rect,
    mouth: Offset,
    onMouthOpen: (Boolean) -> Unit,
    onEaten: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val food = remember { FoodState() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentMouth by rememberUpdatedState(mouth)
    val currentOnMouthOpen by rememberUpdatedState(onMouthOpen)
    val currentOnEaten by rememberUpdatedState(onEaten)

    CareGameFrame(
        hint = "Брось еду питомцу в рот",
        onCancel = {
            currentOnMouthOpen(false)
            onCancel()
        },
        modifier = modifier,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { origin = it.positionInRoot() },
        ) {
            val px = with(density) { 1.dp.toPx() }
            val size = with(density) { ItemSize.toPx() }
            val rest = Offset(constraints.maxWidth / 2f, constraints.maxHeight - with(density) { ItemRestFromBottom.toPx() })
            val bounds = Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            // Рот ловит с запасом: и по размеру питомца, и не меньше пальца.
            val catchRadius = maxOf(48 * px, pet.width * 0.12f)
            val openRadius = pet.width * 0.8f

            fun mouthLocal() = currentMouth - origin

            fun respawn() {
                currentOnMouthOpen(false)
                food.phase = FoodPhase.REST
                food.position = rest
                food.rotation = 0f
                scope.launch {
                    food.scale.snapTo(0f)
                    food.scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }

            fun eat() {
                food.phase = FoodPhase.EATEN
                scope.launch {
                    val from = food.at(rest)
                    val to = mouthLocal()
                    val move = Animatable(0f)
                    launch { food.scale.animateTo(0f, tween(durationMillis = 160)) }
                    move.animateTo(1f, tween(durationMillis = 160)) {
                        food.position = from + (to - from) * value
                    }
                    currentOnEaten()
                }
            }

            // Полёт: кадр за кадром, пока еда в воздухе.
            LaunchedEffect(food.phase) {
                if (food.phase != FoodPhase.FLY) return@LaunchedEffect
                var last = withFrameNanos { it }
                var mouthOpen = false
                while (food.phase == FoodPhase.FLY) {
                    withFrameNanos { now ->
                        val dt = ((now - last) / 1e9f).coerceAtMost(1f / 30)
                        last = now
                        val target = mouthLocal()
                        val before = food.position
                        val toMouth = target - before
                        val distance = toMouth.getDistance()

                        var v = food.velocity + Offset(0f, GRAVITY * px * dt)
                        if (distance < MAGNET_RADIUS * px && distance > 0f) {
                            // Чем ближе, тем сильнее: у самого рта еда будто всасывается.
                            val pull = MAGNET_PULL * px * (1f - distance / (MAGNET_RADIUS * px))
                            v += toMouth / distance * pull * dt
                        }
                        food.velocity = v
                        val after = before + v * dt
                        food.position = after
                        food.rotation += SPIN * dt * if (v.x < 0) -1 else 1

                        val approaching = (v.x * toMouth.x + v.y * toMouth.y) > 0
                        if (!mouthOpen && approaching && distance < openRadius) {
                            mouthOpen = true
                            currentOnMouthOpen(true)
                        }

                        when {
                            segmentDistance(target, before, after) < catchRadius -> eat()
                            // Упала мимо: ниже питомца на пути вниз или за краем экрана.
                            v.y > 0 && after.y > target.y + pet.height * 0.6f -> respawn()
                            !bounds.inflate(size).contains(after) -> respawn()
                        }
                    }
                }
            }

            ItemPicture(
                itemId = itemId,
                fallback = FinneyIcons.Food,
                size = ItemSize,
                modifier = Modifier
                    .offset {
                        val p = food.at(rest)
                        IntOffset((p.x - size / 2).roundToInt(), (p.y - size / 2).roundToInt())
                    }
                    .graphicsLayer {
                        scaleX = food.scale.value
                        scaleY = food.scale.value
                        rotationZ = food.rotation
                    }
                    // Для TalkBack бросок заменяется нажатием: еда сразу летит в рот.
                    .semantics {
                        contentDescription = "Еда. Потяни вверх и отпусти, чтобы бросить"
                        onClick(label = "Покормить") { eat(); true }
                    },
            )

            // Жесты — на всей площади: еду можно схватить с запасом вокруг неё,
            // в палец ребёнка 96 dp попадают не с первого раза.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(rest, pet) {
                        val grabRadius = size
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (food.phase != FoodPhase.REST) return@awaitEachGesture
                            if ((down.position - food.at(rest)).getDistance() > grabRadius) return@awaitEachGesture
                            food.phase = FoodPhase.DRAG
                            val grab = food.at(rest) - down.position
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var mouthOpen = false
                            drag(down.id) { change ->
                                if (food.phase != FoodPhase.DRAG) return@drag
                                food.position = change.position + grab
                                tracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                                val distance = (mouthLocal() - food.position).getDistance()
                                if (distance < catchRadius) {
                                    eat()
                                } else if ((distance < openRadius) != mouthOpen) {
                                    mouthOpen = !mouthOpen
                                    currentOnMouthOpen(mouthOpen)
                                }
                            }
                            if (food.phase != FoodPhase.DRAG) return@awaitEachGesture
                            val velocity = tracker.calculateVelocity()
                            val v = Offset(velocity.x, velocity.y)
                            val speed = v.getDistance()
                            if (v.y < -MIN_THROW * px) {
                                food.velocity = v * (min(speed, MAX_THROW * px) / speed)
                                food.phase = FoodPhase.FLY
                            } else {
                                // Не бросил, а отпустил — еда возвращается на место.
                                respawn()
                            }
                        }
                    },
            )
        }
    }
}

/** Расстояние от точки [p] до отрезка [a]–[b]: за кадр быстрая еда пролетает рот насквозь. */
private fun segmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val lengthSquared = ab.x * ab.x + ab.y * ab.y
    if (lengthSquared == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / lengthSquared).coerceIn(0f, 1f)
    val closest = a + ab * t
    return hypot(p.x - closest.x, p.y - closest.y)
}
