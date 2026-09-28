package ru.finney.pet.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

// Игрушки в зале. Купленная игрушка лежит на полу — в ряд над кнопкой уровня,
// почти на одной линии со ступнями питомца. Рисуется она за питомцем: он может
// пройти перед ней, но сама она его не заслоняет. Её можно взять пальцем
// и поводить рядом с питомцем: он тянется к ней и радуется. Отпустил — игрушка
// сама возвращается на место. Что это даёт в игре, решает главный экран: сцена
// только сообщает, где игрушка и как её двигают.

/** Сторона игрушки — доля видимой ширины экрана. На телефоне около 45 dp. */
private const val TOY_SCREEN_SIZE = 0.115f

/**
 * Низ игрушки — доля высоты холста. Ступни питомца в зале — 0.70, верх кнопки
 * уровня — около 0.73: ряд лежит между ними и кнопку не задевает.
 */
private const val TOY_BOTTOM = 0.72f

/**
 * Места игрушек — середины, доли видимой ширины экрана. Сначала — перед капсулой
 * справа: там пол свободен, питомец стоит левее. Потом — слева, у торшера.
 */
private val ToyPlaces = listOf(0.64f, 0.88f, 0.76f, 0.09f, 0.21f, 0.52f)

/** Сколько игрушек помещается в зале. Магазин не продаёт больше (ContentTest). */
const val MAX_ROOM_TOYS = 6

/** Как игрушка лежит на полу: тень под нижним краем рисунка. */
private val ToyFooting = Footing(feetX = 0.5f, feetY = 0.92f)

/** Насколько игрушка крупнее в руке: её «подняли» ближе к экрану. */
private const val HELD_SCALE = 1.3f

/** Поднятая игрушка — над питомцем и дверью капсулы. */
private const val HELD_Z = 2f

/** Зона захвата — над всем в комнате, даже над поднятой игрушкой. */
private const val GRAB_Z = 3f

/**
 * Где лежат игрушки: [left]..[right] — видимая часть холста по ширине, доли его ширины.
 * Лишние сверх [MAX_ROOM_TOYS] не показываются.
 */
internal fun toySlots(count: Int, left: Float, right: Float): List<RelRect> {
    val span = right - left
    val size = span * TOY_SCREEN_SIZE
    val height = size * Room.CANVAS_RATIO
    return ToyPlaces.take(count).map { place ->
        val centre = left + span * place
        RelRect(centre - size / 2f, TOY_BOTTOM - height, centre + size / 2f, TOY_BOTTOM)
    }
}

/**
 * Одна игрушка на полу.
 *
 * @param onDrag игрушку тащат: где её середина сейчас (в координатах экрана) и на сколько сдвинули.
 * @param onDrop отпустили.
 */
@Composable
internal fun RoomToy(
    toyId: String,
    art: Int,
    place: RelRect,
    canvasW: Dp,
    canvasH: Dp,
    lighting: RoomLighting,
    enabled: Boolean,
    onDrag: (toyId: String, centre: Offset, delta: Offset) -> Unit,
    onDrop: (toyId: String) -> Unit,
) {
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var held by remember { mutableStateOf(false) }
    // Где игрушка лежит — без сдвига пальцем: сдвиг добавляется к этому.
    var home by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    val drop by rememberUpdatedState(onDrop)
    val move by rememberUpdatedState(onDrag)

    fun release() {
        if (!held) return
        held = false
        drop(toyId)
        scope.launch { drag.animateTo(Offset.Zero, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
    }

    // Рисунок лежит за питомцем, а ловит пальцы отдельная зона поверх него:
    // у питомца квадрат с прозрачными полями, и стоя рядом, он забирал бы касания себе.
    val box = Modifier
        .offset(canvasW * place.left, canvasH * place.top)
        .requiredSize(canvasW * place.width, canvasH * place.height)
    val lifted = Modifier.graphicsLayer {
        translationX = drag.value.x
        translationY = drag.value.y
        val s = if (held) HELD_SCALE else 1f
        scaleX = s
        scaleY = s
    }

    Box(
        modifier = box
            .zIndex(GRAB_Z)
            .onGloballyPositioned { home = it.boundsInRoot() }
            .then(lifted)
            .semantics { contentDescription = "Игрушка" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        held = true
                        scope.launch { drag.stop() }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val next = drag.value + amount
                        scope.launch { drag.snapTo(next) }
                        move(toyId, home.center + next, amount)
                    },
                    onDragEnd = ::release,
                    onDragCancel = ::release,
                )
            },
    )

    LitBody(
        lighting = lighting,
        place = place,
        footing = ToyFooting,
        // Пока игрушка в руке или летит обратно, тени на её месте на полу нет.
        visibility = { if (drag.value == Offset.Zero) 1f else 0f },
        // Лежит — за питомцем, поднятая — поверх него и остальных игрушек.
        modifier = box.zIndex(if (held) HELD_Z else 0f).then(lifted),
    ) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
