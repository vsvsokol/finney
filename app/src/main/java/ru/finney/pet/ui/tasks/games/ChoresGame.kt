package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.Chore
import ru.finney.pet.domain.model.ChoresTask
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow

// «Подработка»: неделя дел. Выбираешь день, нажимаешь дела внизу — они встают в
// часики дня. Дело в дне нажатием убирается. Пустой день — отдых, и хотя бы
// один нужен: деньги за работу, но и время не бесконечно. Сколько заработано
// и сколько часиков занято, считает ядро.

@Composable
internal fun ChoresGame(task: ChoresTask, character: PetCharacter, onClose: () -> Unit, onSubmit: (TaskInput) -> Unit) {
    var week by remember { mutableStateOf(List(task.days) { emptyList<String>() }) }
    var selected by remember { mutableIntStateOf(0) }
    val sounds = LocalSounds.current
    val earned = TaskEngines.choresEarned(task, week)
    val rest = week.count { it.isEmpty() }
    val times = week.flatten().groupingBy { it }.eachCount()
    val freeToday = task.hoursPerDay - TaskEngines.dayHours(task, week[selected])

    fun canAdd(chore: Chore) = TaskEngines.choreOpen(chore, selected) && chore.hours <= freeToday &&
        (chore.maxTimes == null || (times[chore.id] ?: 0) < chore.maxTimes)

    GameScene(backdrop = Backdrop.ROOM, onClose = onClose) {
        SceneBody(
            bottom = {
                FinneyButton(text = "Готово", onClick = { onSubmit(TaskInput.Schedule(week)) })
            },
        ) {
            GoalCard(task, earned)
            PetSays(character, petLine(task, earned, rest), petSize = 72.dp)

            // По четыре дня в ряд; неполный ряд добит пустым местом, чтобы дни были одной ширины.
            week.withIndex().chunked(DAYS_IN_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (i, ids) ->
                        DayCard(
                            number = i + 1,
                            chores = ids.map { id -> task.chores.first { it.id == id } },
                            hours = task.hoursPerDay,
                            selected = i == selected,
                            modifier = Modifier.weight(1f),
                            onSelect = { sounds.play(Sfx.Tap); selected = i },
                            onRemove = { n ->
                                sounds.play(Sfx.Tap)
                                week = week.mapIndexed { d, day -> if (d == i) day.filterIndexed { k, _ -> k != n } else day }
                            },
                        )
                    }
                    repeat(DAYS_IN_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            ScenePanel(title = null, modifier = Modifier.fillMaxWidth()) {
                task.chores.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { chore ->
                            ChoreTile(
                                chore = chore,
                                fee = TaskEngines.choreFee(chore, selected),
                                left = chore.maxTimes?.let { it - (times[chore.id] ?: 0) },
                                enabled = canAdd(chore),
                                modifier = Modifier.weight(1f),
                                onAdd = {
                                    sounds.play(Sfx.Coin)
                                    week = week.mapIndexed { d, day -> if (d == selected) day + chore.id else day }
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Реплика: сколько не хватает, а когда хватает — про отдых. */
private fun petLine(task: ChoresTask, earned: Int, rest: Int): String {
    val left = task.goal.price - earned
    return when {
        left > 0 -> "Нужно ещё $left"
        rest < task.minRestDays -> "Хватает! Но мне нужен отдых"
        else -> "Хватает, и есть отдых!"
    }
}

/** Цель: рисунок, «заработано из цены» числом и полоской. */
@Composable
private fun GoalCard(task: ChoresTask, earned: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FinneyCream)
            .border(3.dp, FinneyInk, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        ItemPicture(task.goal, task.goal.label, 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedText("$earned / ${task.goal.price}", style = MaterialTheme.typography.titleLarge)
            }
            Meter(
                fraction = earned.toFloat() / task.goal.price,
                modifier = Modifier.height(14.dp).semantics { contentDescription = "Заработано $earned из ${task.goal.price}" },
            )
        }
    }
}

/** Клетка одного часика: 48 dp — дело в дне нажимается, чтобы убрать (ТЗ п. 3.6). */
private val SlotHeight = 48.dp
private val SlotGap = 4.dp
private const val DAYS_IN_ROW = 4

private fun slotsHeight(hours: Int) = SlotHeight * hours + SlotGap * (hours - 1)

/**
 * День: номер и столбик часиков. Дело занимает столько клеток, сколько часиков,
 * нажатие на него убирает. Свободные часики — пустые клетки. Пустой день — лампа
 * и «отдых» во весь столбик. Выбранный день — жёлтый и с толстой рамкой.
 */
@Composable
private fun DayCard(
    number: Int,
    chores: List<Chore>,
    hours: Int,
    selected: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SlotGap),
        modifier = modifier
            .clip(shape)
            .background(if (selected) FinneyYellow else FinneyCream)
            .border(if (selected) 4.dp else 2.dp, FinneyInk, shape)
            .clickable(role = Role.Tab, onClickLabel = "Выбрать день $number", onClick = onSelect)
            .semantics {
                this.selected = selected
                contentDescription = if (chores.isEmpty()) "День $number, отдых" else "День $number: " + chores.joinToString { it.label }
            }
            .padding(4.dp),
    ) {
        OutlinedText(number.toString(), style = MaterialTheme.typography.titleMedium)
        if (chores.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().height(slotsHeight(hours)),
            ) {
                FinneyIcon(FinneyIcons.Lamp, size = 36.dp)
                Text("отдых", style = MaterialTheme.typography.labelMedium, color = FinneyInk)
            }
        } else {
            chores.forEachIndexed { n, chore ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotsHeight(chore.hours))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(2.dp, FinneyInk, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Убрать ${chore.label}") { onRemove(n) },
                ) { ItemPicture(chore, chore.label, 32.dp) }
            }
            repeat(hours - chores.sumOf { it.hours }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(SlotHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, FinneyInk.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

/** Часики кружками — сколько времени занимает дело. */
@Composable
private fun Clocks(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(FinneyBlue).border(2.dp, FinneyInk, CircleShape))
        }
    }
}

/**
 * Дело плиткой: рисунок, награда и часики. Название — для TalkBack, на плитке его
 * заменяет рисунок. «×2» — сколько раз ещё дают. Нажатие кладёт дело в выбранный день.
 */
@Composable
private fun ChoreTile(chore: Chore, fee: Int, left: Int?, enabled: Boolean, modifier: Modifier, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color.White else FinneyCream)
            .border(3.dp, FinneyInk, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Добавить в день", onClick = onAdd)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${chore.label}: ${chore.hours} ч., $fee финок")
                    if (left != null) append(", осталось $left раз")
                }
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        // Бледнеет только содержимое: с alpha на всей плитке рамка и фон пропадали после смены доступности.
        val fade = Modifier.alpha(if (enabled) 1f else 0.45f)
        ItemPicture(chore, chore.label, 36.dp, fade)
        Column(fade.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedText("+$fee", style = MaterialTheme.typography.titleMedium)
                Coin(size = 18.dp)
            }
            Clocks(chore.hours)
        }
        if (left != null) {
            Box(
                fade.clip(RoundedCornerShape(8.dp)).background(FinneyPink).border(2.dp, FinneyInk, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp),
            ) { Text("×$left", style = MaterialTheme.typography.labelLarge, color = FinneyInk) }
        }
    }
}

