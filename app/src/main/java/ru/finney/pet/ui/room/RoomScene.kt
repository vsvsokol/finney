package ru.finney.pet.ui.room

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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
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

/** Как питомец стоит на полу: см. [Footing]. Ступни на 0.87 квадрата. Мебель — в [Solids]. */
private val PetFooting = Footing(feetX = 0.5f, feetY = 0.87f)

/**
 * Переход между комнатами — чёрная шторка с мягким краем. Она едет в ту
 * сторону, куда идёт питомец: в комнату справа — наползает справа, закрывает
 * экран и уходит влево, открывая новую комнату тоже справа. В комнату слева —
 * зеркально. Как смена сцены в мультфильме: ясно, куда перешли, и две комнаты
 * не видны одновременно. Закрывается и открывается с одной скоростью и ровно,
 * без разгона и торможения: шторка идёт одним движением через весь экран.
 */
private const val WIPE_HALF_MS = 170

/** Ширина мягкого края шторки, доля ширины экрана. */
private const val WIPE_SOFT = 0.35f

/**
 * Прогулка по залу: раз в несколько секунд питомец сам скачет в случайное место.
 * Пауза случайная — по часам его прыжков не ждут, и он выглядит живым, а не заведённым.
 */
private val StrollPauseMs = 6_000L..14_000L

/** Длина одного скачка, доля ширины холста: дальние перебежки — несколько скачков подряд. */
private const val HOP_LENGTH = 0.09f

/** Скачок по времени совпадает с прыжком [ru.finney.pet.ui.pet.PetAnimation.playJoy]: присел, взлетел, приземлился. */
private const val HOP_CROUCH_MS = 140L
private const val HOP_FLIGHT_MS = 530

/**
 * Прозрачные поля вокруг питомца в его квадрате, доля стороны: край квадрата
 * может уйти за край экрана, а сам питомец — нет.
 */
private const val PET_MARGIN = 0.12f

/** Сколько комната дышит: пена колышется в этих пределах от своего размера. */
private const val FOAM_SWELL = 0.02f
private const val FOAM_PERIOD_MS = 3400

/**
 * За сколько небо за окном сдвигается на ширину своей полосы (около 215 dp
 * на телефоне), то есть примерно 2 dp в секунду. Быстрее — и небо уже
 * «едет» и отвлекает, медленнее — движения не заметно вовсе.
 */
private const val SKY_LOOP_MS = 120_000

/** Фон неба в экспорте, им же залиты края полосы. */
private val SkyColor = Color(0xFF261F46)

/**
 * Паузы между пролётами НЛО, мс. Первая короткая — чтобы ребёнок успел его
 * увидеть, пока смотрит на комнату, дальше реже: постоянно мелькающее НЛО
 * перестаёт быть событием. Точный срок каждый раз случайный — по часам
 * его ждать неинтересно.
 */
private val UfoFirstPauseMs = 3_000L..8_000L

/** Сколько закрывается и открывается дверь капсулы. */
internal const val DOOR_MS = 600

/** Сколько питомец идёт в капсулу и обратно. */
internal const val WALK_MS = 900
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
 * Комнаты три — зал, кухня, ванная, — и при переключении [spot] одна
 * гаснет в чёрное и проступает другая. В каждой свой набор: в зале торшер, окно и капсула, на кухне стол
 * у окна без торшера, в ванной ванна под торшером без окна. Свет у каждой
 * комнаты свой, потому что источники в них разные.
 *
 * В комнате ночь (см. RoomLight.kt): светят торшер, окно и пролетающее НЛО.
 * Стены и пол освещаются одним слоем, питомец и мебель — каждый своим,
 * и у каждого своя тень.
 *
 * @param lampOn горит ли торшер. Выключателя нет, торшер горит всегда — и во сне:
 *   ночь и так видна за окном, а тёмная комната пугала.
 * @param capsule стоит ли в зале капсула для сна. Мини-играм комната нужна
 *   фоном, и капсула во весь экран там только мешает.
 * @param asleep питомец спит: уходит в капсулу, торшер гаснет, дверь закрывается.
 *   Проснулся — всё в обратном порядке.
 * @param pet встаёт туда, где ему положено быть в текущей комнате.
 * @param onTapItem нажатие по самому предмету: тому же, что делает нижняя кнопка.
 * @param wander гулять ли питомцу по залу самому. Выключается, пока с ним что-то
 *   делают: во сне, в играх ухода, в других комнатах.
 * @param onHop питомец скакнул — запустить прыжок в его анимации: поза живёт снаружи.
 */
@Composable
fun RoomScene(
    spot: RoomSpot,
    modifier: Modifier = Modifier,
    lampOn: Boolean = true,
    capsule: Boolean = true,
    asleep: Boolean = false,
    onTapItem: (() -> Unit)? = null,
    wander: Boolean = false,
    onHop: () -> Unit = {},
    pet: @Composable BoxScope.() -> Unit = {},
) {
    // Где питомец в зале сейчас: сдвиг от его обычного места, доля ширины холста.
    val stroll = remember { Animatable(0f) }
    val ufo = remember { UfoState() }
    // Свет у каждой комнаты свой: источники в них разные. Во время перехода
    // видны две комнаты сразу, и каждая освещена по-своему.
    val lights = RoomSpot.entries.associateWith { room ->
        rememberRoomLighting(
            lampOn = lampOn && (room.hasLamp || room.ceilingLight),
            ufo = ufo::light,
            window = room.hasWindow,
            ceiling = room.ceilingLight,
        )
    }
    // НЛО летает одно на все комнаты: окна во время перехода два, а пролёт один.
    LaunchedEffect(ufo) { ufo.fly() }

    // Отход ко сну: питомец идёт в капсулу (walk 0 → 1), потом закрывается дверь.
    // Живёт на всю сцену, а не на зал: переход между комнатами его не сбрасывает.
    val walk = remember { Animatable(if (asleep) 1f else 0f) }
    val door = remember { Animatable(if (asleep) 1f else 0f) }
    LaunchedEffect(asleep) {
        if (asleep) {
            walk.animateTo(1f, tween(WALK_MS, easing = FastOutSlowInEasing))
            door.animateTo(1f, tween(DOOR_MS, easing = FastOutSlowInEasing))
        } else {
            door.animateTo(0f, tween(DOOR_MS, easing = FastOutSlowInEasing))
            walk.animateTo(0f, tween(WALK_MS, easing = FastOutSlowInEasing))
        }
    }

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

        // Куда можно скакать: только в пределах видимой части холста, чтобы
        // питомец не ушёл за край экрана.
        val home = Room.petGround(RoomSpot.LIVING)
        // Слева у края экрана шкала настроения: за неё питомец не заходит.
        val visibleLeft = (-shiftX + 56.dp) / canvasW
        val visibleRight = (maxWidth - shiftX) / canvasW
        val strollMin = visibleLeft - home.left - home.width * PET_MARGIN
        val strollMax = visibleRight - home.right + home.width * PET_MARGIN
        // Какая комната на экране сейчас: во время затемнения это ещё старая.
        var shown by remember { mutableStateOf(spot) }
        // Шторка: 0 — нет её, 0..1 — наползает, 1 — экран чёрный, 1..2 — уходит.
        // Направление: 1 — идём вправо, шторка едет справа налево; −1 — наоборот.
        val wipe = remember { Animatable(0f) }
        var wipeDir by remember { mutableIntStateOf(1) }
        LaunchedEffect(spot) {
            if (spot == shown) return@LaunchedEffect
            // Передумали посреди открытия — шторка закрывается заново с того же места.
            if (wipe.value > 1f) wipe.snapTo(2f - wipe.value)
            wipeDir = if (spot.ordinal > shown.ordinal) 1 else -1
            wipe.animateTo(1f, tween(WIPE_HALF_MS, easing = LinearEasing))
            shown = spot
            wipe.animateTo(2f, tween(WIPE_HALF_MS, easing = LinearEasing))
            wipe.snapTo(0f)
        }

        val canWander = wander && spot == RoomSpot.LIVING && shown == RoomSpot.LIVING && !asleep
        LaunchedEffect(canWander, strollMin, strollMax) {
            if (!canWander || strollMax <= strollMin) return@LaunchedEffect
            while (true) {
                delay(Random.nextLong(StrollPauseMs.first, StrollPauseMs.last))
                val target = Random.nextFloat() * (strollMax - strollMin) + strollMin
                val from = stroll.value
                val hops = maxOf(1, kotlin.math.ceil(kotlin.math.abs(target - from) / HOP_LENGTH).toInt())
                for (i in 1..hops) {
                    onHop()
                    delay(HOP_CROUCH_MS)
                    stroll.animateTo(
                        from + (target - from) * i / hops,
                        tween(HOP_FLIGHT_MS, easing = FastOutSlowInEasing),
                    )
                }
            }
        }

        key(shown) {
            RoomCanvas(
                room = shown,
                lighting = lights.getValue(shown),
                canvasW = canvasW,
                canvasH = canvasH,
                shiftX = shiftX,
                shiftY = shiftY,
                ufo = ufo,
                capsule = capsule,
                walk = { walk.value },
                stroll = { stroll.value },
                door = { door.value },
                onTapItem = onTapItem,
                pet = pet,
            )
        }
        // Затемнение поверх комнаты, но под интерфейсом: RoomScene — фон экрана.
        // Прозрачность читается на отрисовке и не пересобирает комнату.
        Box(modifier = Modifier.fillMaxSize().drawBehind { drawWipe(wipe.value, wipeDir) })
    }
}

/**
 * Одна комната целиком: стены, её окно и торшер, мебель и питомец.
 *
 * wrapContentSize обязателен: холст больше экрана, и requiredSize без него
 * центрирует его в родителе — комната уезжала влево ещё на полразницы
 * ширин (около 40 dp) мимо shiftX, и торшер срезало наполовину.
 */
@Composable
private fun RoomCanvas(
    room: RoomSpot,
    lighting: RoomLighting,
    canvasW: Dp,
    canvasH: Dp,
    shiftX: Dp,
    shiftY: Dp,
    ufo: UfoState,
    capsule: Boolean,
    walk: () -> Float,
    stroll: () -> Float,
    door: () -> Float,
    onTapItem: (() -> Unit)?,
    pet: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(shiftX, shiftY)
                .requiredSize(canvasW, canvasH),
        ) {
            Box(modifier = Modifier.fillMaxSize().roomLight(lighting)) {
                Layer(Room.Back, canvasW, canvasH)
                if (room.hasWindow) Window(canvasW, canvasH, ufo)
                if (room.hasLamp) Layer(Room.Lamp, canvasW, canvasH)
            }

            val visibility = { 1f }
            val ground = Room.petGround(room).let { if (room == RoomSpot.LIVING) it.shiftedX(stroll()) else it }

            // Тень у питомца только в зале: за столом и в ванне она
            // упала бы на мебель, а мебель пола не знает.
            when (room) {
                RoomSpot.LIVING -> if (capsule) {
                    // Капсула в углу, питомец перед ней. Засыпая, он уходит
                    // в капсулу и становится меньше — она дальше от зрителя.
                    // Дверь закрывается перед ним, стекло у неё полупрозрачное,
                    // и спящего видно. Тень на полу — только пока он стоит на месте.
                    LitLayer(Room.Capsule, canvasW, canvasH, lighting, Solids.Capsule, visibility)
                    TapZone(Room.Capsule.rect, canvasW, canvasH, "Уложить спать", onTapItem)
                    val t = walk()
                    val footing = if (t == 0f) PetFooting else null
                    Pet(ground.lerp(Room.PetInCapsule, t), canvasW, canvasH, lighting, footing, visibility, pet)
                    CapsuleDoor(canvasW, canvasH, lighting, shut = door)
                } else {
                    Pet(ground, canvasW, canvasH, lighting, PetFooting, visibility, pet)
                }

                // Питомец рисуется раньше стола: столешница перекрывает
                // ему низ, и получается, что он сидит за столом, а не на нём.
                RoomSpot.KITCHEN -> {
                    Pet(ground, canvasW, canvasH, lighting, footing = null, visibility, pet)
                    LitLayer(Room.Table, canvasW, canvasH, lighting, Solids.Table, visibility)
                    TapZone(Room.Table.rect, canvasW, canvasH, "Покормить", onTapItem)
                }

                // Питомец сидит в ванне, а не перед ней, поэтому чаша
                // рисуется поверх него: она непрозрачна ниже 0.664
                // и прячет всё, что должно быть под водой. Пена заходит
                // выше борта (0.527 против 0.619) и делится на две:
                // задняя за питомцем, передняя перед ним.
                RoomSpot.BATH -> {
                    Foam(Room.BathFoamBack, canvasW, canvasH, lighting, phase = 0f)
                    Pet(ground, canvasW, canvasH, lighting, footing = null, visibility, pet)
                    LitLayer(Room.Bath, canvasW, canvasH, lighting, Solids.Bath, visibility)
                    Foam(Room.BathFoamFront, canvasW, canvasH, lighting, phase = 0.5f)
                    TapZone(Room.Bath.rect, canvasW, canvasH, "Помыть", onTapItem)
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
    lighting: RoomLighting,
    footing: Footing?,
    visibility: () -> Float,
    pet: @Composable BoxScope.() -> Unit,
) {
    LitBody(
        lighting = lighting,
        place = ground,
        footing = footing,
        visibility = visibility,
        pad = PET_PAD,
        modifier = Modifier
            .offset(canvasW * ground.left, canvasH * ground.top)
            .requiredSize(canvasW * ground.width),
        content = pet,
    )
}

/**
 * Окно: небо, НЛО, рама.
 *
 * Рама шире проёма (0.350..0.979 против 0.428..0.881) и кладётся последней —
 * она перекрывает его края, и стык не виден. Небо и НЛО обрезаны проёмом:
 * за рамой им делать нечего.
 *
 * Небо медленно плывёт влево — полоса [Room.WindowSky] повторяется встык и
 * сдвигается на свою ширину за [SKY_LOOP_MS]. Камеры в комнате нет, поэтому
 * движение идёт от времени. Края полосы — ровный фон неба (см. pack_room.py),
 * так что на стыке шва нет, а в конце круга сдвиг ровно на ширину полосы,
 * и перескок в начало не виден.
 *
 * Между пролётами НЛО не рисуется вовсе, а не ждёт за краем: раньше оно
 * стояло там всю паузу, и на экране торчал его обрезанный край.
 */
@Composable
private fun Window(canvasW: Dp, canvasH: Dp, ufo: UfoState) {
    val hole = Room.WindowHole
    val sky = ImageBitmap.imageResource(Room.WindowSky.image)
    val drift by rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SKY_LOOP_MS, easing = LinearEasing)),
        label = "drift",
    )

    Box(
        modifier = Modifier
            .offset(canvasW * hole.left, canvasH * hole.top)
            .requiredSize(canvasW * hole.width, canvasH * hole.height)
            .clipToBounds()
            // Сдвиг читается только на отрисовке: небо плывёт, не пересобирая окно.
            .drawBehind {
                drawRect(SkyColor)
                val strip = Room.WindowSky.rect
                val tileW = size.width * strip.width / hole.width
                val tileH = size.height * strip.height / hole.height
                val top = size.height * (strip.top - hole.top) / hole.height
                var x = size.width * (strip.left - hole.left) / hole.width - drift * tileW
                while (x > 0f) x -= tileW
                // Сдвиг дробный, и полоса рисуется масштабом, а не в целых
                // пикселях: иначе на такой скорости небо ползло бы рывками.
                while (x < size.width) {
                    translate(x, top) {
                        scale(tileW / sky.width, tileH / sky.height, pivot = Offset.Zero) { drawImage(sky) }
                    }
                    x += tileW
                }
            },
    ) {

        val size = Room.WindowUfo.rect
        ufo.flight?.let { current ->
            Image(
                painter = painterResource(Room.WindowUfo.image),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(canvasW * size.width, canvasH * size.height)
                    // Прогресс читается только здесь, на отрисовке: кадры пролёта
                    // двигают готовую картинку и не пересобирают окно.
                    .graphicsLayer {
                        val t = ufo.progress.value
                        val corner = ufo.corner(current, t)
                        translationX = corner.x * canvasW.toPx()
                        translationY = corner.y * canvasH.toPx()
                        rotationZ = current.tilt(t)
                    },
            )
        }
    }

    Layer(Room.WindowFrame, canvasW, canvasH)
}

/**
 * НЛО за окном: пролёты и где оно сейчас.
 *
 * Живёт в сцене, а не в окне: где НЛО, нужно ещё и свету — пролетая,
 * оно подсвечивает комнату зелёным, и тень питомца поворачивается за ним.
 */
@Stable
private class UfoState {
    var flight by mutableStateOf<UfoFlight?>(null)
        private set
    val progress = Animatable(0f)

    suspend fun fly() {
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

    /**
     * Левый верхний угол НЛО относительно проёма, в долях холста.
     *
     * Путь от «целиком за одним краем проёма» до «целиком за другим»: пролёт
     * кончается, только когда НЛО ушло из окна полностью, и не обрывается
     * на полпути.
     */
    fun corner(current: UfoFlight, t: Float): Offset {
        val hole = Room.WindowHole
        val ufo = Room.WindowUfo.rect
        val x = -ufo.width + current.along(t) * (hole.width + ufo.width)
        return Offset(
            x = if (current.fromLeft) x else hole.width - ufo.width - x,
            y = current.height(t) * (hole.height - ufo.height),
        )
    }

    /** Середина НЛО в долях холста и насколько оно в проёме; null — пролёта нет. */
    fun light(): Pair<Offset, Float>? {
        val current = flight ?: return null
        val hole = Room.WindowHole
        val ufo = Room.WindowUfo.rect
        val corner = corner(current, progress.value)
        val centre = Offset(
            hole.left + corner.x + ufo.width / 2f,
            hole.top + corner.y + ufo.height / 2f,
        )
        // Свет входит в комнату через стекло: пока НЛО за рамой, комнату
        // оно не освещает, и зелёный не вспыхивает из ниоткуда.
        val presence = ((centre.x - hole.left) / ufo.width).coerceIn(0f, 1f) *
            ((hole.right - centre.x) / ufo.width).coerceIn(0f, 1f)
        return centre to presence
    }
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
private fun Foam(layer: RoomLayer, canvasW: Dp, canvasH: Dp, lighting: RoomLighting, phase: Float) {
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

    LitBody(
        lighting = lighting,
        place = layer.rect,
        footing = null,
        // Пена на вдохе растёт на FOAM_SWELL — запас ровно под это.
        pad = FOAM_SWELL * 2f,
        modifier = Modifier
            .offset(canvasW * layer.rect.left, canvasH * layer.rect.top)
            .requiredSize(canvasW * layer.rect.width, canvasH * layer.rect.height),
    ) {
        Image(
            painter = painterResource(layer.image),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
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
}

/**
 * Дверь капсулы. Закрывается, пока питомец спит: выезжает сверху, как
 * шторка, и проявляется. Своей тени у двери нет — её тень уже в капсуле.
 *
 * @param shut насколько закрыта, 0..1. Лямбдой: читается на отрисовке.
 */
@Composable
private fun CapsuleDoor(canvasW: Dp, canvasH: Dp, lighting: RoomLighting, shut: () -> Float) {
    if (shut() == 0f) return
    val rect = Room.CapsuleDoor.rect
    LitBody(
        lighting = lighting,
        place = rect,
        footing = null,
        modifier = Modifier
            .offset(canvasW * rect.left, canvasH * rect.top)
            .requiredSize(canvasW * rect.width, canvasH * rect.height)
            .graphicsLayer {
                alpha = shut()
                translationY = -(1f - shut()) * size.height * 0.25f
            },
    ) {
        Image(
            painter = painterResource(Room.CapsuleDoor.image),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Мебель: освещается там, где стоит, и отбрасывает тень на пол. */
@Composable
private fun LitLayer(
    layer: RoomLayer,
    canvasW: Dp,
    canvasH: Dp,
    lighting: RoomLighting,
    solid: Solid,
    visibility: () -> Float,
) {
    LitBody(
        lighting = lighting,
        place = layer.rect,
        footing = null,
        solid = solid,
        visibility = visibility,
        modifier = Modifier
            .offset(canvasW * layer.rect.left, canvasH * layer.rect.top)
            .requiredSize(canvasW * layer.rect.width, canvasH * layer.rect.height),
    ) {
        Image(
            painter = painterResource(layer.image),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
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

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun RoomSceneLampOffPreview() {
    FinneyTheme {
        RoomScene(spot = RoomSpot.LIVING, lampOn = false, modifier = Modifier.size(412.dp, 892.dp))
    }
}

/**
 * Чёрная шторка перехода на позиции [progress] (см. [WIPE_SOFT]). Рисуется как
 * для хода вправо — шторка едет справа налево, — а ход влево зеркалит её.
 */
private fun DrawScope.drawWipe(progress: Float, direction: Int) {
    if (progress <= 0f || progress >= 2f) return
    val w = size.width
    val soft = w * WIPE_SOFT
    scale(scaleX = direction.toFloat(), scaleY = 1f) {
        if (progress <= 1f) {
            // Наползает: сплошное чёрное справа от края, мягкий переход левее него.
            val edge = w + soft - progress * (w + 2 * soft)
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = edge - soft, endX = edge),
                topLeft = Offset(edge - soft, 0f),
                size = Size(soft, size.height),
            )
            drawRect(Color.Black, topLeft = Offset(edge, 0f), size = Size(w - edge, size.height))
        } else {
            // Уходит влево: чёрное остаётся слева от края, справа уже новая комната.
            val edge = w - (progress - 1f) * (w + soft)
            drawRect(Color.Black, size = Size(edge.coerceAtLeast(0f), size.height))
            drawRect(
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = edge, endX = edge + soft),
                topLeft = Offset(edge, 0f),
                size = Size(soft, size.height),
            )
        }
    }
}
