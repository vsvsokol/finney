package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyIconButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetPose
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.room.RoomScene
import ru.finney.pet.ui.room.RoomSpot
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeRegular

// Сцена мини-игры, как в концептах: игра идёт не на пустом экране, а в месте —
// в комнате Финни, в магазине, на поле, у лавки. Сверху деньги и крестик,
// поверх фона — сама игра, питомец и его реплики.

/** Где идёт игра. Комнаты — настоящая комната с главного экрана, остальное нарисовано кодом. */
enum class Backdrop { ROOM, ROOM_RAIN, SHOP, FIELD, SKY, SUNSET, STORE }

/**
 * Экран игры: фон, верхняя полоса и содержимое.
 * [money] — число в кошельке этой игры; null — кошелёк не показываем.
 */
@Composable
internal fun GameScene(
    backdrop: Backdrop,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    money: Int? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        BackdropLayer(backdrop)
        Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = SceneEdge, vertical = 8.dp)) {
            content()
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (money != null) MoneyPill(money)
                Box(Modifier.weight(1f))
                FinneyIconButton(onClick = onClose, contentDescription = "Закрыть игру", size = 48.dp) {
                    OutlinedText("✕", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

/** Поле сцены по бокам. Кнопки и панели держатся в нём, а лента и полки выходят за него — см. [bleed]. */
internal val SceneEdge: Dp = 12.dp

/**
 * Растянуть на всю ширину экрана, за поле сцены. Для того, что по смыслу
 * уходит за край: лента конвейера, доска полки. С полем они обрывались
 * в 12 dp от края и выглядели недорисованными.
 */
internal fun Modifier.bleed(): Modifier = layout { measurable, constraints ->
    val extra = (SceneEdge * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** Высота верхней полосы: содержимое сцены начинается ниже, чтобы не залезть под крестик. */
internal val HudHeight: Dp = 60.dp

/**
 * Содержимое сцены под верхней полосой. Середина прокручивается, если не влезла,
 * а [bottom] — кнопки — всегда у нижнего края: их видно без прокрутки, и они
 * не уезжают из-под пальца, когда середина растёт.
 *
 * Короткая середина растягивается на всю высоту, поэтому `Spacer(Modifier.weight(1f))`
 * в ней работает: он забирает свободное место, а при прокрутке сжимается в ноль.
 */
@Composable
internal fun SceneBody(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().padding(top = HudHeight), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
        }
        bottom()
    }
}

@Composable
private fun MoneyPill(amount: Int) {
    CoinAmount(
        amount = amount,
        coinSize = 30.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun BackdropLayer(backdrop: Backdrop) {
    when (backdrop) {
        Backdrop.ROOM -> RoomScene(spot = RoomSpot.LIVING, modifier = Modifier.fillMaxSize())
        Backdrop.ROOM_RAIN -> Box(Modifier.fillMaxSize()) {
            RoomScene(spot = RoomSpot.LIVING, modifier = Modifier.fillMaxSize())
            Rain()
        }
        // Пол — под нижней полкой: полки прижаты к тележке, и стеллаж стоит на полу.
        Backdrop.SHOP -> Split(top = Color(0xFFF9E2B8), bottom = Color(0xFFC99A76), at = 0.85f, dots = true)
        Backdrop.FIELD -> Canvas(Modifier.fillMaxSize()) {
            drawRect(FinneyGreen)
            drawCircle(Color(0xFF9BD8A2), size.width * 0.55f, Offset(size.width * 0.3f, size.height * 0.2f))
            drawCircle(Color(0xFF9BD8A2), size.width * 0.4f, Offset(size.width * 0.85f, size.height * 0.62f))
        }
        Backdrop.SKY -> Box(Modifier.fillMaxSize()) {
            Split(top = Color(0xFF9CCBF0), bottom = FinneyGreen, at = 0.58f, dots = false)
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * 0.84f, size.height * 0.16f)
                drawCircle(FinneyYellow.copy(alpha = 0.5f), 42.dp.toPx(), c)
                drawCircle(FinneyInk, 32.dp.toPx(), c)
                drawCircle(FinneyYellow, 28.dp.toPx(), c)
            }
        }
        Backdrop.SUNSET -> Split(top = Color(0xFFF4A98E), bottom = Color(0xFF6FB679), at = 0.58f, dots = false)
        Backdrop.STORE -> Split(top = Color(0xFFCFE3F5), bottom = FinneyYellow, at = 0.42f, dots = true, desk = true)
    }
}

/** Стена и пол: верх до доли [at], низ после. [desk] — деревянная столешница на стыке, как у кассы. */
@Composable
private fun Split(top: Color, bottom: Color, at: Float, dots: Boolean, desk: Boolean = false) {
    Canvas(Modifier.fillMaxSize()) {
        val line = size.height * at
        drawRect(top, size = size.copy(height = line))
        drawRect(bottom, topLeft = Offset(0f, line), size = size.copy(height = size.height - line))
        if (dots) {
            val step = 28.dp.toPx()
            var y = step / 2
            while (y < line) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(FinneyInk.copy(alpha = 0.08f), 3.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        if (desk) {
            val h = 22.dp.toPx()
            drawRect(FinneyInk, topLeft = Offset(0f, line - 4.dp.toPx()), size = size.copy(height = h + 8.dp.toPx()))
            drawRect(Color(0xFFA0715A), topLeft = Offset(0f, line), size = size.copy(height = h))
        }
    }
}

/** Дождь за окном и в комнате: косые светлые штрихи и лёгкая синева. Без анимации — не отвлекает. */
@Composable
private fun Rain() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(FinneyInk.copy(alpha = 0.25f))
        val step = 40.dp.toPx()
        val len = 60.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawLine(
                    Color.White.copy(alpha = 0.5f),
                    Offset(x + y * 0.27f, y),
                    Offset(x + (y + len) * 0.27f, y + len),
                    strokeWidth = 2.dp.toPx(),
                )
                y += len * 2
            }
            x += step
        }
    }
}

/**
 * Цвет тела питомца игрока. Задаёт экран задания, читает [ScenePet]: так цвет не нужно
 * протаскивать параметром через все шесть игр. Гостей в сценах он не касается.
 */
internal val LocalPlayerBodyColor = staticCompositionLocalOf { BodyColor.A }

/** Питомец игрока в сцене: живой, дышит. */
@Composable
internal fun ScenePet(character: PetCharacter, size: Dp, modifier: Modifier = Modifier, mood: PetMood = PetMood.HAPPY) {
    val animation = rememberPetAnimation()
    PetView(
        character = character,
        bodyColor = LocalPlayerBodyColor.current,
        mood = mood,
        pose = rememberPoseProvider(animation),
        modifier = modifier.widthIn(max = size).fillMaxWidth(),
    )
}

/** Другие питомцы — гости и покупатели. Стоят спокойно, одной позой на всех. */
@Composable
internal fun Guest(character: PetCharacter, size: Dp, modifier: Modifier = Modifier) {
    PetView(character = character, mood = PetMood.HAPPY, pose = StillPose, modifier = modifier.widthIn(max = size).fillMaxWidth())
}

private val StillPose: () -> PetPose = { PetPose() }

/**
 * Питомец и его реплика. Пузырь стоит вплотную к питомцу и не шире, чем нужно
 * тексту: растянутый на всю строку, он отъезжал к краю, и хвостик смотрел в пустоту.
 */
@Composable
internal fun PetSays(character: PetCharacter, text: String, modifier: Modifier = Modifier, petSize: Dp = 120.dp) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScenePet(character, petSize, Modifier.width(petSize))
        Bubble(text, Tail.LEFT, Modifier.weight(1f, fill = false), maxWidth = 240.dp)
    }
}

/** Куда смотрит хвостик реплики — в сторону того, кто говорит. */
enum class Tail { LEFT, RIGHT, DOWN_LEFT, DOWN_RIGHT }

/** Реплика в белом пузыре, как в концептах. Текст короткий — одна-две строки. */
@Composable
internal fun Bubble(text: String, tail: Tail, modifier: Modifier = Modifier, maxWidth: Dp = 200.dp) {
    val tailSize = 12.dp
    val shape = remember(tail) { BubbleShape(tail, 16.dp, tailSize) }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = FinneyInk,
        modifier = modifier
            .widthIn(max = maxWidth)
            .background(Color.White, shape)
            .border(StrokeRegular, FinneyInk, shape)
            .padding(
                start = 12.dp + if (tail == Tail.LEFT) tailSize else 0.dp,
                end = 12.dp + if (tail == Tail.RIGHT) tailSize else 0.dp,
                top = 8.dp,
                bottom = 8.dp + if (tail == Tail.DOWN_LEFT || tail == Tail.DOWN_RIGHT) tailSize else 0.dp,
            ),
    )
}

/** Пузырь реплики: скруглённое тело и треугольный хвостик с одной стороны. */
private class BubbleShape(private val tail: Tail, private val radius: Dp, private val tailSize: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val t = with(density) { tailSize.toPx() }
        val down = tail == Tail.DOWN_LEFT || tail == Tail.DOWN_RIGHT
        val body = RoundRect(
            left = if (tail == Tail.LEFT) t else 0f,
            top = 0f,
            right = size.width - if (tail == Tail.RIGHT) t else 0f,
            bottom = size.height - if (down) t else 0f,
            cornerRadius = CornerRadius(r),
        )
        val path = Path().apply {
            addRoundRect(body)
            when (tail) {
                Tail.LEFT -> { moveTo(t, r); lineTo(0f, r + t * 0.6f); lineTo(t, r + t * 1.4f); close() }
                Tail.RIGHT -> { moveTo(size.width - t, r); lineTo(size.width, r + t * 0.6f); lineTo(size.width - t, r + t * 1.4f); close() }
                Tail.DOWN_LEFT -> { moveTo(r, body.bottom); lineTo(r + t * 0.3f, size.height); lineTo(r + t * 1.4f, body.bottom); close() }
                Tail.DOWN_RIGHT -> {
                    moveTo(size.width - r, body.bottom); lineTo(size.width - r - t * 0.3f, size.height)
                    lineTo(size.width - r - t * 1.4f, body.bottom); close()
                }
            }
        }
        return Outline.Generic(path)
    }
}

/** Кремовая панель с заголовком на рамке, как «Проверка», «Касса», «Итог дня» в концептах. */
@Composable
internal fun ScenePanel(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.padding(top = if (title != null) 18.dp else 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(FinneyCream)
                .border(4.dp, FinneyInk, RoundedCornerShape(22.dp))
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = if (title != null) 26.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        if (title != null) {
            OutlinedText(
                title,
                style = MaterialTheme.typography.headlineMedium,
                // Заголовок садится на рамку: половина над ней, половина — внутри.
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-22).dp),
            )
        }
    }
}

/** Полоса-подсказка жеста — розовая плашка, как «смахни вниз» в концептах. */
@Composable
internal fun GestureHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FinneyPink)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Шкала «сколько из скольки»: копилка, тележка. [extra] — сегодняшняя добавка штриховкой. */
@Composable
internal fun Meter(fraction: Float, modifier: Modifier = Modifier, extra: Float = 0f, color: Color = FinneyGreen) {
    Canvas(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF7EA))
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(8.dp)),
    ) {
        val done = fraction.coerceIn(0f, 1f)
        drawRect(color, size = size.copy(width = size.width * done))
        val plus = extra.coerceIn(0f, 1f - done)
        if (plus > 0f) {
            drawRect(
                Brush.linearGradient(listOf(FinneyYellow, Color.White, FinneyYellow)),
                topLeft = Offset(size.width * done, 0f),
                size = size.copy(width = size.width * plus),
            )
        }
    }
}
