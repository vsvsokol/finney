package ru.finney.pet.ui.room

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// Комната собирается из слоёв одного холста 1440×2400, и каждый предмет уже стоит
// на своём месте в кадре — как у питомца, координаты подбирать не нужно. Числа
// лежат в Room, их печатает tools/pack_room.py при сборке ресурсов.
//
// Обводка у комнаты чёрная, а не FinneyInk: это отдельный пласт от интерфейса.
// Панели и кнопки плавают поверх и намеренно не сливаются с обстановкой.

/** Сколько комната дышит: пена колышется в этих пределах от своего размера. */
private const val FOAM_SWELL = 0.02f
private const val FOAM_PERIOD_MS = 3400

/** Пролёт НЛО: сколько летит через окно и сколько ждёт до следующего раза. */
private const val UFO_FLIGHT_MS = 7000
private const val UFO_PAUSE_MS = 48000

/**
 * Комната питомца.
 *
 * Сцена всегда рисуется в пропорции холста дизайнера и **заполняет экран
 * целиком**: комната вытянута (0.6), телефон уже (около 0.46), поэтому холст
 * масштабируется по высоте, а лишнее срезается по бокам. Растягивать нельзя —
 * круглые формы поплыли бы и обводка порвалась. Раньше холст вписывался по
 * ширине, и сверху оставалась кремовая полоса почти в четверть экрана.
 *
 * По горизонтали кадр не по центру, а вокруг [Room.FOCUS_X]: слева торшер,
 * который понадобится для света, и срезать его нельзя, а справа край занавески.
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

        // FOCUS_X встаёт в середину экрана, но холст не отъезжает от края:
        // пустой полосы сбоку быть не должно ни на каком экране.
        val shiftX = (maxWidth / 2 - canvasW * Room.FOCUS_X).coerceIn(maxWidth - canvasW, 0.dp)
        // Низ холста — к низу экрана. На телефоне холст ровно в высоту экрана,
        // и сдвиг нулевой; выше экрана он бывает только на широких экранах.
        val shiftY = maxHeight - canvasH

        Box(modifier = Modifier.offset(shiftX, shiftY).requiredSize(canvasW, canvasH)) {
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
        val flight = rememberInfiniteTransition(label = "ufo")
        val progress by flight.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // Пролёт занимает малую часть цикла: остальное НЛО ждёт за кадром,
                // иначе оно мелькает в окне постоянно и перестаёт быть событием.
                animation = tween(UFO_FLIGHT_MS + UFO_PAUSE_MS, easing = LinearEasing),
            ),
            label = "flight",
        )

        val ufo = Room.WindowUfo.rect
        val span = UFO_FLIGHT_MS.toFloat() / (UFO_FLIGHT_MS + UFO_PAUSE_MS)
        Image(
            painter = painterResource(Room.WindowUfo.image),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(canvasW * (ufo.left - hole.left), canvasH * (ufo.top - hole.top))
                .requiredSize(canvasW * ufo.width, canvasH * ufo.height)
                .graphicsLayer {
                    // Экспорт дизайнера — середина пути: отсюда НЛО уезжает на ширину
                    // окна влево и вправо, и положение на картинке оказывается тем,
                    // что видно в середине пролёта.
                    val travel = size.width * 6f
                    translationX = if (progress < span) {
                        (progress / span - 0.5f) * travel
                    } else {
                        travel // ждёт за краем проёма
                    }
                },
        )
    }

    Layer(Room.WindowFrame, canvasW, canvasH)
}

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
