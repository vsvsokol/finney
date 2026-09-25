package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow
import kotlin.math.roundToInt

// «Дорога к цели», как ходилка в Тамагочи, только фишку двигает копилка, а не
// кубик. Каждый день приходит доход; ребёнок делит монеты розовой чертой:
// слева — в копилку, справа — можно потратить на соблазн дня. Прогноз
// «успею или нет» пересчитывается сразу, пока двигаешь черту.

@Composable
internal fun GoalRaceGame(
    task: GoalRaceTask,
    character: PetCharacter,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var deposits by remember { mutableStateOf(emptyList<Int>()) }
    var today by remember { mutableIntStateOf(task.incomePerDay / 2 / task.step * task.step) }
    val day = deposits.size + 1
    val saved = task.startSaved + deposits.sum()
    val event = task.events.firstOrNull { it.day == day }

    GameScene(backdrop = Backdrop.FIELD, onClose = onClose) {
        Column(Modifier.fillMaxSize().padding(top = HudHeight), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PiggyCard(saved = saved, adding = today, task = task)

            Board(task = task, day = day, character = character, modifier = Modifier.weight(1f).fillMaxWidth())

            ScenePanel(title = null, modifier = Modifier.fillMaxWidth()) {
                OutlinedText("День $day · получил ${task.incomePerDay}", style = MaterialTheme.typography.titleLarge)
                CoinSplit(income = task.incomePerDay, step = task.step, deposit = today, onChange = { today = it })
                event?.let {
                    val affordable = task.incomePerDay - today >= it.price
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(3.dp, FinneyInk, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                    ) {
                        ItemPicture(it, it.label, 36.dp)
                        Column {
                            Text("Событие дня: ${it.label}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                            Text(
                                if (affordable) "стоит ${it.price} — хватает" else "стоит ${it.price} — можно отказаться",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FinneyInk,
                            )
                        }
                    }
                }
                Text(forecast(task, saved + today, task.days - day), style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                FinneyButton(
                    text = if (day == task.days) "Финиш" else "Готово",
                    onClick = {
                        val all = deposits + today
                        if (all.size == task.days) onSubmit(TaskInput.DailyDeposits(all)) else deposits = all
                    },
                )
            }
        }
    }
}

/** Прогноз одной фразой: сколько откладывать дальше, чтобы успеть. */
private fun forecast(task: GoalRaceTask, savedAfterToday: Int, daysLeft: Int): String {
    val left = task.goal.price - savedAfterToday
    if (left <= 0) return "Успеешь! ${task.goal.label} уже набран."
    if (daysLeft == 0) return "Не хватит $left. Накопленное останется."
    val perDay = ((left + daysLeft - 1) / daysLeft + task.step - 1) / task.step * task.step
    return if (perDay <= task.incomePerDay) {
        "Успею? Осталось $left — откладывай по $perDay в день."
    } else {
        "Осталось $left — к сроку уже не успеть, но копилка не пропадёт."
    }
}

@Composable
private fun PiggyCard(saved: Int, adding: Int, task: GoalRaceTask) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FinneyCream)
            .border(3.dp, FinneyInk, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text("Копилка $saved", style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
            Text("${task.goal.label.lowercase()} ${task.goal.price}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        }
        Meter(
            fraction = saved.toFloat() / task.goal.price,
            extra = adding.toFloat() / task.goal.price,
            modifier = Modifier.height(14.dp).semantics { contentDescription = "В копилке $saved из ${task.goal.price}, сегодня плюс $adding" },
        )
    }
}

/**
 * Поле: кружки дней змейкой по четыре, между ними дорожка. Пройденные — жёлтые,
 * у дней с соблазном — розовая точка. Фишка — сам питомец, на финише — цель.
 */
@Composable
private fun Board(task: GoalRaceTask, day: Int, character: PetCharacter, modifier: Modifier) {
    val eventDays = task.events.map { it.day }.toSet()
    BoxWithConstraints(modifier) {
        // Дни и финиш — в два ряда: при восьми днях по пять в ряд, иначе третий ряд
        // не помещался над панелью дня и поле налезало на копилку.
        val perRow = if (task.days + 1 > 8) 5 else 4
        val pointRows = (task.days + perRow) / perRow
        val tile = 48.dp
        val stepX = (maxWidth - tile) / perRow
        // Сверху — место под цель на финише, снизу — под нижний ряд кружков.
        val top = 56.dp
        val bottom = tile / 2 + 8.dp
        val stepY = if (pointRows > 1) ((maxHeight - top - bottom) / (pointRows - 1)).coerceIn(96.dp, 150.dp) else 0.dp
        // Поле посередине свободного места, а не прижато к панели: иначе над ним пустовало полэкрана.
        val lift = ((maxHeight - top - bottom - stepY * (pointRows - 1)) / 2).coerceAtLeast(0.dp)

        // Змейка снизу вверх: первый ряд слева направо, второй справа налево. Последняя точка — цель.
        fun centre(i: Int): DpOffset {
            val row = i / perRow
            val col = i % perRow
            val x = if (row % 2 == 0) col else perRow - 1 - col
            return DpOffset(tile / 2 + stepX * x + stepX / 2, maxHeight - lift - bottom - stepY * row)
        }
        val points = (0..task.days).map(::centre)

        Canvas(Modifier.fillMaxSize()) {
            val path = Path()
            points.forEachIndexed { i, p ->
                val o = Offset(p.x.toPx(), p.y.toPx())
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, FinneyCream, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                path,
                FinneyInk,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 22f))),
            )
        }

        (1..task.days).forEach { d ->
            val p = points[d - 1]
            Box(Modifier.offset(p.x - tile / 2, p.y - tile / 2), contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier
                        .size(tile)
                        .clip(CircleShape)
                        .background(if (d < day) FinneyYellow else FinneyCream)
                        .border(if (d == day) 5.dp else 4.dp, FinneyInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { OutlinedText(d.toString(), style = MaterialTheme.typography.titleLarge) }
                if (d in eventDays) {
                    Box(Modifier.offset(4.dp, (-4).dp).size(16.dp).clip(CircleShape).background(FinneyPink).border(2.dp, FinneyInk, CircleShape))
                }
            }
        }

        val finish = points.last()
        Column(Modifier.offset(finish.x - 34.dp, finish.y - 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ItemPicture(task.goal, task.goal.label, 60.dp)
            PriceTag(task.goal.price.toString())
        }

        // Фишка стоит на своём кружке, а не над ним: над кружком она задевала
        // ценник цели в ряду выше. Какой сейчас день, написано в панели.
        val here = points[(day - 1).coerceIn(0, task.days - 1)]
        ScenePet(character, 60.dp, Modifier.width(60.dp).offset(here.x - 30.dp, here.y - 50.dp))
    }
}

/**
 * Монеты дня в ряд и розовая черта между «в копилку» и «потратить». Черту тянут
 * пальцем или ставят нажатием на монету; она встаёт только на шаг взноса.
 */
@Composable
private fun CoinSplit(income: Int, step: Int, deposit: Int, onChange: (Int) -> Unit) {
    // Одна монетка — одна финка, пока их не больше двадцати; иначе монетка — шаг взноса.
    val unit = if (income <= 20) 1 else step
    val count = income / unit
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()
        fun snap(x: Float): Int {
            val raw = (x / widthPx).coerceIn(0f, 1f) * income
            return ((raw / step).roundToInt() * step).coerceIn(0, income)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(income, step) {
                    detectHorizontalDragGestures { change, _ -> onChange(snap(change.position.x)) }
                }
                .pointerInput(income, step) { detectTapGestures { onChange(snap(it.x)) } }
                .semantics {
                    contentDescription = "Монеты дня: сколько в копилку"
                    stateDescription = "в копилку $deposit из $income"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val split = deposit / unit
            repeat(count) { i ->
                if (i == split) Divider()
                Box(
                    Modifier
                        .size(coinSize(count))
                        .clip(CircleShape)
                        .background(if (i < split) FinneyYellow else FinneyPeach.copy(alpha = 0.55f))
                        .border(2.dp, FinneyInk, CircleShape),
                )
            }
            if (split == count) Divider()
        }
    }
    // Для тех, кому неудобно тянуть: те же шаги кнопками. Подписи между ними —
    // одной строкой с кнопками, чтобы панель дня не отнимала высоту у поля.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallButton("− $step", enabled = deposit >= step) { onChange(deposit - step) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("в копилку $deposit", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            Text("потратить ${income - deposit}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        }
        SmallButton("+ $step", enabled = deposit + step <= income) { onChange(deposit + step) }
    }
}

private fun coinSize(count: Int): Dp = if (count <= 10) 24.dp else 16.dp

@Composable
private fun Divider() {
    Box(Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(FinneyPink).border(2.dp, FinneyInk, RoundedCornerShape(3.dp)))
}

/** Кнопка шага «− 5» / «+ 5». Высота — от 48 dp, как у всех нажимаемых элементов (ТЗ п. 3.6). */
@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) FinneyGreen else FinneyCream)
            .border(3.dp, FinneyInk, RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) FinneyInk else FinneyInk.copy(alpha = 0.35f),
        )
    }
}
