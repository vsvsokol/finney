package ru.finney.pet.ui.room

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Комната собирается из слоёв одного холста 1440×2400, и каждый предмет уже стоит
// на своём месте в кадре — как у питомца, координаты подбирать не нужно. Числа
// лежат в Room, их печатает tools/pack_room.py при сборке ресурсов.
//
// Обводка у комнаты чёрная, а не FinneyInk: это отдельный пласт от интерфейса.
// Панели и кнопки плавают поверх и намеренно не сливаются с обстановкой.

/** Сколько комната дышит: пена колышется в этих пределах от своего размера. */
private const val FOAM_SWELL = 0.02f
private const val FOAM_PERIOD_MS = 3400

/**
 * Паузы между пролётами НЛО, мс. Первая короткая — чтобы ребёнок успел его
 * увидеть, пока смотрит на комнату, дальше реже: постоянно мелькающее НЛО
 * перестаёт быть событием. Точный срок каждый раз случайный — по часам
 * его ждать неинтересно.
 */
private val UfoFirstPauseMs = 3_000L..8_000L
private val UfoPauseMs = 20_000L..45_000L

/**
 * Комната питомца.
 *
 * Сцена всегда рисуется в пропорции холста дизайнера и **заполняет экран
 * целиком**: комната вытянута (0.6), телефон уже (около 0.46), поэтому холст
 * масштабируется по высоте, а лишнее срезается по бокам. Растягивать нельзя —
 * круглые формы поплыли бы и обводка порвалась. Раньше холст вписывался по
 * ширине, и сверху оставалась кремовая полоса почти в четверть экрана.
 *
 * По горизонтали кадр стоит на оси комнаты [Room.ROOM_AXIS_X], но не дальше
 * [Room.FRAME_MIN_LEFT]: слева торшер, который понадобится для света, и ванна,
 * а срезается край занавески.
 * Если экран шире холста (планшет), холст подгоняется по ширине и срезается
 * сверху — пол, стол и ванна нужны целиком, а верх стены пустой.
 *
 * Предмет в комнате ровно один — тот, что выбран в [spot]. Стены, окно и лампа
 * общие для всех комнат и не перерисовываются при переключении.
 *
 * @param pet встаёт туда, где ему положено быть в текущей комнате.
 * @param onTapItem нажатие по самому предмету: тому же, что делает нижняя кнопка.
 */
@Composable
fun RoomScene(
    spot: RoomSpot,
    modifier: Modifier = Modifier,
    onTapItem: (() -> Unit)? = null,
    pet: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.clipToBounds().background(FinneyCream)) {
        // Масштаб «накрыть экран»: холст не меньше экрана ни по одной стороне.
        val canvasW = maxOf(maxWidth, maxHeight * Room.CANVAS_RATIO)
        val canvasH = canvasW / Room.CANVAS_RATIO

        // Ось комнаты встаёт в середину экрана, но левее FRAME_MIN_LEFT кадр не
        // уходит, и холст не отъезжает от края: пустой полосы сбоку быть
        // не должно ни на каком экране.
        val shiftX = (maxWidth / 2 - canvasW * Room.ROOM_AXIS_X).coerceIn(
            minimumValue = maxOf(maxWidth - canvasW, -canvasW * Room.FRAME_MIN_LEFT),
            maximumValue = 0.dp,
        )
        // Низ холста — к низу экрана. На телефоне холст ровно в высоту экрана,
        // и сдвиг нулевой; выше экрана он бывает только на широких экранах.
        val shiftY = maxHeight - canvasH

        // wrapContentSize обязателен: холст больше экрана, и requiredSize без него
        // центрирует его в родителе — комната уезжала влево ещё на полразницы
        // ширин (около 40 dp) мимо shiftX, и торшер срезало наполовину.
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(shiftX, shiftY)
                .requiredSize(canvasW, canvasH),
        ) {
            Layer(Room.Back, canvasW, canvasH)

            Window(canvasW, canvasH)
            Layer(Room.Lamp, canvasW, canvasH)

            // Предмет не подменяется мгновенно: ребёнок нажал кнопку внизу, и
            // комната должна успеть показать, что изменилась. Питомец внутри
            // перехода, поэтому он переезжает вместе с обстановкой.
            Crossfade(targetState = spot, label = "spot") { current ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val ground = Room.petGround(current)

                    when (current) {
                        RoomSpot.LIVING -> Pet(ground, canvasW, canvasH, pet)

                        // Питомец рисуется раньше стола: столешница перекрывает
                        // ему низ, и получается, что он сидит за столом, а не на нём.
                        RoomSpot.KITCHEN -> {
                            Pet(ground, canvasW, canvasH, pet)
                            Layer(Room.Table, canvasW, canvasH)
                            TapZone(Room.Table.rect, canvasW, canvasH, "Покормить", onTapItem)
                        }

                        // Питомец сидит в ванне, а не перед ней, поэтому чаша
                        // рисуется поверх него: она непрозрачна ниже 0.664
                        // и прячет всё, что должно быть под водой. Пена заходит
                        // выше борта (0.527 против 0.619) и делится на две:
                        // задняя за питомцем, передняя перед ним.
                        RoomSpot.BATH -> {
                            Foam(Room.BathFoamBack, canvasW, canvasH, phase = 0f)
                            Pet(ground, canvasW, canvasH, pet)
                            Layer(Room.Bath, canvasW, canvasH)
                            Foam(Room.BathFoamFront, canvasW, canvasH, phase = 0.5f)
                            TapZone(Room.Bath.rect, canvasW, canvasH, "Помыть", onTapItem)
                        }
                    }
                }
            }
        }
    }
}

/** Питомец на своём месте в комнате. Квадрат по ширине холста — как у слоёв. */
@Composable
private fun Pet(
    ground: RelRect,
    canvasW: Dp,
    canvasH: Dp,
    pet: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(canvasW * ground.left, canvasH * ground.top)
            .requiredSize(canvasW * ground.width),
        content = pet,
    )
}

/**
 * Окно: вид, НЛО, рама.
 *
 * Рама шире вида (0.350..0.979 против 0.428..0.881) и кладётся последней —
 * она перекрывает его края, и стык не виден. НЛО летит между ними, обрезанный
 * проёмом: за рамой ему делать нечего.
 *
 * Между пролётами НЛО не рисуется вовсе, а не ждёт за краем: раньше оно
 * стояло там всю паузу, и на экране торчал его обрезанный край.
 */
@Composable
private fun Window(canvasW: Dp, canvasH: Dp) {
    Layer(Room.WindowView, canvasW, canvasH)

    val hole = Room.WindowHole
    Box(
        modifier = Modifier
            .offset(canvasW * hole.left, canvasH * hole.top)
            .requiredSize(canvasW * hole.width, canvasH * hole.height)
            .clipToBounds(),
    ) {
        var flight by remember { mutableStateOf<UfoFlight?>(null) }
        val progress = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            var pause = UfoFirstPauseMs
            while (true) {
                delay(Random.nextLong(pause.first, pause.last))
                val next = Random.nextUfoFlight()
                flight = next
                progress.snapTo(0f)
                progress.animateTo(1f, tween(next.durationMs, easing = LinearEasing))
                flight = null
                pause = UfoPauseMs
            }
        }

        val ufo = Room.WindowUfo.rect
        flight?.let { current ->
            Image(
                painter = painterResource(Room.WindowUfo.image),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(canvasW * ufo.width, canvasH * ufo.height)
                    // Прогресс читается только здесь, на отрисовке: кадры пролёта
                    // двигают готовую картинку и не пересобирают окно.
                    .graphicsLayer {
                        val t = progress.value
                        val holeW = (canvasW * hole.width).toPx()
                        val holeH = (canvasH * hole.height).toPx()

                        // Путь от «целиком за одним краем проёма» до «целиком
                        // за другим»: пролёт кончается, только когда НЛО ушло
                        // из окна полностью, и не обрывается на полпути.
                        val along = current.along(t)
                        val x = -size.width + along * (holeW + size.width)
                        translationX = if (current.fromLeft) x else holeW - size.width - x
                        translationY = current.height(t) * (holeH - size.height)
                        rotationZ = current.tilt(t)
                    },
            )
        }
    }

    Layer(Room.WindowFrame, canvasW, canvasH)
}

/**
 * Один пролёт НЛО.
 *
 * Пролёты не повторяются: каждый раз заново выбирается, с какой стороны оно
 * летит, на какой высоте, как сильно покачивается и не зависнет ли посреди
 * окна — поглазеть на питомца. Одинаковый ровный пролёт на третий раз
 * смотреть уже скучно.
 *
 * Всё в долях: путь 0..1 от «целиком за краем, откуда летит» до «целиком
 * за противоположным», высота 0..1 от верха проёма до низа.
 */
@Immutable
private data class UfoFlight(
    val durationMs: Int,
    val fromLeft: Boolean,
    val startHeight: Float,
    val endHeight: Float,
    /** Размах покачивания вверх-вниз, доля высоты проёма. */
    val bob: Float,
    /** Сколько раз НЛО качнётся за пролёт. */
    val bobCycles: Float,
    /** Где на пути зависнуть; null — пролететь не останавливаясь. */
    val hoverAt: Float?,
    /** Повисев, улететь туда же, откуда прилетело. */
    val turnBack: Boolean,
) {
    /** Сколько пути пройдено к моменту [t]. */
    fun along(t: Float): Float {
        val stop = hoverAt ?: return t
        return when {
            // Подлетает с торможением, висит, уходит с разгоном: как будто
            // заметило что-то в комнате, а потом спохватилось.
            t < HOVER_IN -> stop * FastOutSlowInEasing.transform(t / HOVER_IN)
            t < HOVER_OUT -> stop + HOVER_DRIFT * sin((t - HOVER_IN) / (HOVER_OUT - HOVER_IN) * PI.toFloat())
            else -> {
                val exit = FastOutLinearInEasing.transform((t - HOVER_OUT) / (1f - HOVER_OUT))
                val target = if (turnBack) 0f else 1f
                stop + (target - stop) * exit
            }
        }
    }

    /** Высота к моменту [t]: плавный уход с одной высоты на другую плюс покачивание. */
    fun height(t: Float): Float {
        val drift = startHeight + (endHeight - startHeight) * t
        return (drift + bob * sin(t * bobCycles * 2f * PI.toFloat())).coerceIn(0f, 1f)
    }

    /**
     * Наклон по ходу движения: тарелка клюёт носом туда, куда летит, и
     * выравнивается, когда зависает. Скорость берётся разностью — путь
     * кусочный, и производную по формулам пришлось бы писать на каждый кусок.
     */
    fun tilt(t: Float): Float {
        val dt = 0.01f
        val speed = (along((t + dt).coerceAtMost(1f)) - along(t)) / dt
        val direction = if (fromLeft) 1f else -1f
        return (speed * direction * UFO_TILT_PER_SPEED).coerceIn(-UFO_MAX_TILT, UFO_MAX_TILT)
    }
}

/** Доли пролёта с зависанием: до HOVER_IN подлетает, до HOVER_OUT висит. */
private const val HOVER_IN = 0.35f
private const val HOVER_OUT = 0.65f

/** Пока висит, НЛО чуть подаётся вперёд и обратно — неподвижное выглядит картинкой. */
private const val HOVER_DRIFT = 0.02f

/** Градусов наклона на единицу скорости: ровный пролёт — около 6°. */
private const val UFO_TILT_PER_SPEED = 6f
private const val UFO_MAX_TILT = 14f

private fun Random.nextUfoFlight(): UfoFlight {
    val hover = nextFloat() < 0.4f
    // Высоты — в средней части проёма: рама заходит на края вида, и у самого
    // верха или низа НЛО наполовину пряталось бы под ней.
    val start = between(0.2f, 0.8f)
    return UfoFlight(
        // Без зависания пролёт бывает и стремительным, и ленивым.
        durationMs = if (hover) nextInt(7_000, 10_000) else nextInt(2_500, 7_500),
        fromLeft = nextBoolean(),
        startHeight = start,
        endHeight = (start + between(-0.35f, 0.35f)).coerceIn(0.1f, 0.9f),
        bob = between(0f, 0.12f),
        bobCycles = between(1f, 4f),
        hoverAt = if (hover) between(0.35f, 0.6f) else null,
        turnBack = hover && nextBoolean(),
    )
}

private fun Random.between(from: Float, until: Float): Float = from + nextFloat() * (until - from)

/** Пена тихо колышется. Слои дышат в противофазе, иначе движение читается как рывок всей ванны. */
@Composable
private fun Foam(layer: RoomLayer, canvasW: Dp, canvasH: Dp, phase: Float) {
    val breath = rememberInfiniteTransition(label = "foam")
    val swell by breath.animateFloat(
        initialValue = -FOAM_SWELL,
        targetValue = FOAM_SWELL,
        animationSpec = infiniteRepeatable(
            animation = tween(FOAM_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((FOAM_PERIOD_MS * phase).toInt()),
        ),
        label = "swell",
    )

    Image(
        painter = painterResource(layer.image),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(canvasW * layer.rect.left, canvasH * layer.rect.top)
            .requiredSize(canvasW * layer.rect.width, canvasH * layer.rect.height)
            // Масштаб читается внутри graphicsLayer, чтобы кадры анимации не
            // пересобирали слой — тот же приём, что у позы питомца.
            .graphicsLayer {
                scaleX = 1f + swell
                scaleY = 1f + swell
                // Пена растёт от низа: верх её края должен гулять, а посадка в ванну — нет.
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    )
}

@Composable
private fun Layer(layer: RoomLayer, canvasW: Dp, canvasH: Dp) {
    Image(
        painter = painterResource(layer.image),
        contentDescription = null,
        // Размер уже точный — слой обрезан ровно по своим границам, растягивать нечего.
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(canvasW * layer.rect.left, canvasH * layer.rect.top)
            .requiredSize(canvasW * layer.rect.width, canvasH * layer.rect.height),
    )
}

/**
 * Нажимаемое место на мебели.
 *
 * Зона невидима: подсказкой служит значок, который кладут поверх (см. HomeScreen).
 * Название действия уходит в TalkBack — по картинке прочесть его нельзя.
 */
@Composable
private fun TapZone(rect: RelRect, canvasW: Dp, canvasH: Dp, action: String, onTap: (() -> Unit)?) {
    if (onTap == null) return
    Box(
        modifier = Modifier
            .offset(canvasW * rect.left, canvasH * rect.top)
            .requiredSize(canvasW * rect.width, canvasH * rect.height)
            .semantics { contentDescription = action }
            .clickable(onClickLabel = action, onClick = onTap),
    )
}

@Preview(widthDp = 360, heightDp = 740)
@Composable
private fun RoomSceneKitchenPreview() {
    FinneyTheme {
        RoomScene(spot = RoomSpot.KITCHEN, modifier = Modifier.size(360.dp, 740.dp))
    }
}

@Preview(widthDp = 360, heightDp = 740)
@Composable
private fun RoomSceneBathPreview() {
    FinneyTheme {
        RoomScene(spot = RoomSpot.BATH, modifier = Modifier.size(360.dp, 740.dp))
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun RoomSceneLivingPreview() {
    FinneyTheme {
        RoomScene(spot = RoomSpot.LIVING, modifier = Modifier.size(412.dp, 892.dp))
    }
}
