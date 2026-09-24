package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.Spending
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow

// «Дождливый день» — спланированный случай непредвиденной траты, который ТЗ
// разрешает прямо (раздел 2). Сначала «Моя неделя»: траты конвертами, нужное
// всегда в плане, остаток падает в банку «Запас». Потом за окном дождь и
// ломается зонт. Запаса хватило — успех. Нет — ребёнок сам переносит желаемое;
// нужное перенести нельзя. Питомец не болеет и не пугается.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReserveGame(
    task: ReserveTask,
    character: PetCharacter,
    inputError: TaskInputError?,
    onInputSeen: () -> Unit,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var planned by remember { mutableStateOf(task.spendings.filter { it.category == Category.NEEDS }.map { it.id }.toSet()) }
    var weekStarted by remember { mutableStateOf(false) }
    var dropped by remember { mutableStateOf(emptySet<String>()) }
    val reserve = TaskEngines.reserveLeft(task, planned)

    if (!weekStarted) {
        GameScene(backdrop = Backdrop.ROOM, onClose = onClose, money = task.amount) {
            Column(
                Modifier.fillMaxSize().padding(top = HudHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScenePanel(title = "Моя неделя", modifier = Modifier.fillMaxWidth()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        task.spendings.forEach { s ->
                            Envelope(s, checked = s.id in planned) { planned = if (it) planned + s.id else planned - s.id }
                        }
                    }
                    ReserveJar(maxOf(0, reserve))
                    val spent = task.amount - reserve
                    Row {
                        Text(
                            if (reserve >= 0) "разложено $spent из ${task.amount}" else "не хватает ${-reserve}",
                            style = MaterialTheme.typography.titleMedium,
                            color = FinneyInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (reserve >= 0) "✓" else "!", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                    }
                    TwoPartMeter(spent = spent, total = task.amount)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScenePet(character, 100.dp, Modifier.width(100.dp))
                    Bubble("А если что-то случится?", Tail.LEFT, Modifier.weight(1f))
                }
                FinneyButton(text = "Начать неделю", onClick = { weekStarted = true }, enabled = reserve >= 0)
            }
        }
        return
    }

    val surprise = task.surprise
    val shortage = maxOf(0, surprise.price - reserve)
    val freed = task.spendings.filter { it.id in dropped }.sumOf { it.price }

    GameScene(backdrop = Backdrop.ROOM_RAIN, onClose = onClose, money = reserve) {
        Column(
            Modifier.fillMaxSize().padding(top = HudHeight).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(4.dp, FinneyInk, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shortage == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(FinneyBlue)
                                .border(3.dp, FinneyInk, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) { ItemPicture(surprise, surprise.label, 44.dp) }
                        Column {
                            OutlinedText(surprise.label, style = MaterialTheme.typography.titleLarge, fill = FinneyInk, outline = Color.White)
                            Text(surprise.text, style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                        }
                    }
                    FinneyButton(text = "Взять из запаса · ${surprise.price}", onClick = { onSubmit(TaskInput.Reserve(planned, dropped)) })
                } else {
                    OutlinedText("Нужно ${surprise.price}: ${surprise.label.lowercase().trimEnd('!')}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (reserve > 0) "В запасе $reserve, не хватает $shortage. Что перенесём на следующую неделю?"
                        else "Запаса нет. Что перенесём на следующую неделю?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FinneyInk,
                    )
                    task.spendings.filter { it.id in planned }.forEach { s ->
                        PostponeRow(s, checked = s.id in dropped) {
                            dropped = if (it) dropped + s.id else dropped - s.id
                            onInputSeen()
                        }
                    }
                    Text(
                        "Нашлось $freed из $shortage" + if (freed >= shortage) " ✓" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = FinneyInk,
                    )
                    if (inputError != null) Note(inputErrorText(inputError), color = FinneyPeach)
                    FinneyButton(text = "Готово", onClick = { onSubmit(TaskInput.Reserve(planned, dropped)) }, enabled = freed >= shortage)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScenePet(character, 100.dp, Modifier.width(100.dp))
                Bubble(
                    if (shortage == 0) "Хорошо, что был запас! План на неделю цел" else "Желаемое купим потом. В следующий раз оставим запас",
                    Tail.LEFT,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

/** Конверт траты: день недели не важен, важно — что это и сколько. Нужное заперто замком. */
@Composable
private fun Envelope(spending: Spending, checked: Boolean, onChange: (Boolean) -> Unit) {
    val locked = spending.category == Category.NEEDS
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color.White else FinneyCream)
            .border(3.dp, if (checked) FinneyInk else FinneyInk.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = !locked, role = Role.Checkbox, onValueChange = onChange)
            .padding(6.dp),
    ) {
        ItemPicture(spending, spending.label, 36.dp)
        Text(spending.label, style = MaterialTheme.typography.labelMedium, color = FinneyInk, textAlign = TextAlign.Center)
        OutlinedText(spending.price.toString(), style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                locked -> "нужное"
                checked -> "✓ купить"
                else -> "в запас"
            },
            style = MaterialTheme.typography.labelMedium,
            color = FinneyInk,
        )
    }
}

/** Банка «Запас»: жёлтые монетки внутри — сколько отложено на всякий случай. */
@Composable
private fun ReserveJar(amount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FinneyGreen)
            .border(3.dp, FinneyInk, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Canvas(Modifier.size(width = 44.dp, height = 52.dp)) {
            val lid = 9.dp.toPx()
            val stroke = 3.dp.toPx()
            val body = Size(size.width, size.height - lid)
            drawRoundRect(FinneyCream, Offset(0f, lid), body, CornerRadius(10.dp.toPx()))
            drawRoundRect(FinneyYellow, Offset(0f, lid + body.height * 0.4f), Size(size.width, body.height * 0.6f), CornerRadius(10.dp.toPx()))
            drawRoundRect(FinneyInk, Offset(0f, lid), body, CornerRadius(10.dp.toPx()), style = Stroke(stroke))
            drawRoundRect(FinneyPeach, Offset(4.dp.toPx(), 0f), Size(size.width - 8.dp.toPx(), lid), CornerRadius(4.dp.toPx()))
            drawRoundRect(FinneyInk, Offset(4.dp.toPx(), 0f), Size(size.width - 8.dp.toPx(), lid), CornerRadius(4.dp.toPx()), style = Stroke(stroke))
        }
        Column {
            OutlinedText("Запас · $amount", style = MaterialTheme.typography.titleLarge)
            Text("деньги на всякий случай", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
        }
    }
}

/** Полоса: синяя — разложено по тратам, зелёная — запас. */
@Composable
private fun TwoPartMeter(spent: Int, total: Int) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(3.dp, FinneyInk, RoundedCornerShape(8.dp)),
    ) {
        val split = size.width * (spent.toFloat() / total).coerceIn(0f, 1f)
        drawRect(FinneyBlue, size = size.copy(width = split))
        drawRect(FinneyGreen, Offset(split, 0f), size.copy(width = size.width - split))
    }
}

/** Строка «что перенести». Нужное — пунктиром и замком: переносить нельзя. */
@Composable
private fun PostponeRow(spending: Spending, checked: Boolean, onChange: (Boolean) -> Unit) {
    val locked = spending.category == Category.NEEDS
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) FinneyPeach else FinneyCream)
            .then(
                if (locked) {
                    Modifier.dashedBorder()
                } else {
                    Modifier.border(3.dp, FinneyInk, RoundedCornerShape(12.dp))
                },
            )
            .toggleable(value = checked, enabled = !locked, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        ItemPicture(spending, spending.label, 32.dp)
        Text(spending.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(
            if (locked) "нужное" else "хочется",
            style = MaterialTheme.typography.labelMedium,
            color = if (locked) FinneyInk else Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (locked) FinneyGreen else FinneyPink)
                .padding(horizontal = 6.dp),
        )
        OutlinedText(spending.price.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

private fun Modifier.dashedBorder(): Modifier = drawBehind {
    drawRoundRect(
        FinneyInk.copy(alpha = 0.6f),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
    )
}
