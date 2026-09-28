package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.Spending
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.StrokeThin
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeBold
import ru.finney.pet.ui.theme.StrokeRegular

// «Дождливый день» — спланированный случай непредвиденной траты, который ТЗ
// разрешает прямо (раздел 2). Сначала «Моя неделя»: траты конвертами, нужное
// всегда в плане, остаток падает в банку «Запас». Потом начинается дождь и
// случается непредвиденное — от нуля до нескольких трат, заранее неизвестно. Запаса
// не хватило — ребёнок сам переносит траты. Выигрыш — баланс: хоть одна радость в плане
// и ничего не пришлось отменить; пустой план и жадный — оба «почти». Бонус — за запас на
// все сюрпризы, какие могли случиться, а не только выпавшие: запас откладывают заранее.
// Питомец не болеет и не пугается.

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
        GameScene(backdrop = Backdrop.DOTS, onClose = onClose, money = task.amount) {
            SceneBody(
                bottom = { FinneyButton(text = "Начать неделю", onClick = { weekStarted = true }, enabled = reserve >= 0) },
            ) {
                ScenePanel(title = "Моя неделя", modifier = Modifier.fillMaxWidth()) {
                    // По три конверта в ряд на всю ширину панели. С шириной числом они
                    // оставляли справа пустую полосу, а на широком экране — ещё шире.
                    task.spendings.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                            row.forEach { s ->
                                Envelope(s, checked = s.id in planned, Modifier.weight(1f).fillMaxHeight()) {
                                    planned = if (it) planned + s.id else planned - s.id
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    ReserveJar(maxOf(0, reserve))
                    val spent = task.amount - reserve
                    // Числом, без слова «разложено»: полоска ниже показывает то же. Перебор — «!» и минус.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = if (reserve >= 0) "разложено $spent из ${task.amount}" else "не хватает ${-reserve}"
                        },
                    ) {
                        OutlinedText(
                            if (reserve >= 0) "$spent / ${task.amount}" else "−${-reserve}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (reserve >= 0) "✓" else "!", style = MaterialTheme.typography.titleLarge, color = FinneyInk)
                    }
                    TwoPartMeter(spent = spent, total = task.amount)
                }
                Spacer(Modifier.weight(1f))
                PetSays(character, "А если что-то случится?", petSize = 100.dp)
            }
        }
        return
    }

    val surprises = task.surprises
    val total = TaskEngines.surprisesTotal(task)
    val shortage = maxOf(0, total - reserve)
    val freed = task.spendings.filter { it.id in dropped }.sumOf { it.price }

    GameScene(backdrop = Backdrop.DOTS_RAIN, onClose = onClose, money = reserve) {
        SceneBody {
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
                    if (surprises.isEmpty()) {
                        OutlinedText("Неделя прошла спокойно", style = MaterialTheme.typography.titleLarge, fill = FinneyInk, outline = Color.White)
                    }
                    surprises.forEach { surprise ->
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
                    }
                    FinneyButton(
                        text = if (total > 0) "Взять из запаса · $total" else "Дальше",
                        onClick = { onSubmit(TaskInput.Reserve(planned, dropped)) },
                    )
                } else {
                    OutlinedText(
                        "Нужно $total: " + surprises.joinToString { it.label.lowercase().trimEnd('!') },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    // Сколько не хватает — числом, вопрос — коротко: что делать, без слов не понять.
                    Text("Не хватает $shortage. Что перенесём?", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                    task.spendings.filter { it.id in planned }.forEach { s ->
                        PostponeRow(s, checked = s.id in dropped) {
                            dropped = if (it) dropped + s.id else dropped - s.id
                            onInputSeen()
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Нашлось $freed из $shortage" },
                    ) {
                        Meter(freed.toFloat() / shortage, Modifier.weight(1f).height(14.dp))
                        OutlinedText("$freed / $shortage" + if (freed >= shortage) " ✓" else "", style = MaterialTheme.typography.titleLarge)
                    }
                    if (inputError != null) Note(inputErrorText(inputError), color = FinneyPeach)
                    FinneyButton(text = "Готово", onClick = { onSubmit(TaskInput.Reserve(planned, dropped)) }, enabled = freed >= shortage)
                }
            }
            Spacer(Modifier.weight(1f))
            // Реплика «Хорошо, что был запас! / Желаемое купим потом…» повторяла объяснение
            // итога слово в слово — питомец просто стоит рядом.
            PetAtRight(character, 100.dp)
        }
    }
}

/**
 * Конверт траты: день недели не важен, важно — что это и сколько.
 *
 * Плейтест: невыбранные конверты (бледная рамка) выглядели закрытыми, «откроются
 * позже», а выбранные — вариантами на выбор. Теперь как у обычного выбора:
 * выбранное — жёлтое, в толстой рамке и с «✓»; невыбранное — белое, в обычной
 * рамке и с пустым кружком, который хочется отметить; нужное — выбрано навсегда,
 * вместо «✓» замок. Слова «купить / в запас» — для TalkBack.
 */
@Composable
private fun Envelope(spending: Spending, checked: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val locked = spending.category == Category.NEEDS
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) FinneyYellow else Color.White)
            .border(if (checked) StrokeBold else StrokeRegular, FinneyInk, RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = !locked, role = Role.Checkbox, onValueChange = onChange)
            .semantics { stateDescription = if (locked) "нужное, уже в плане" else if (checked) "купить" else "в запас" }
            .padding(6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            ItemPicture(spending, spending.label, 36.dp)
            Text(spending.label, style = MaterialTheme.typography.labelMedium, color = FinneyInk, textAlign = TextAlign.Center)
            OutlinedText(spending.price.toString(), style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            when {
                locked -> Box(
                    Modifier.size(24.dp).clip(CircleShape).background(FinneyCream).border(2.dp, FinneyInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { FinneyIcon(FinneyIcons.Lock, size = 16.dp) }
                checked -> CheckBadge(size = 24.dp)
                else -> Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White).border(2.dp, FinneyInk, CircleShape))
            }
        }
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
            .background(FinneyYellow)
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

/** Полоса: синяя — разложено по тратам, жёлтая — запас (деньги, а не «успех»). */
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
        drawRect(FinneyYellow, Offset(split, 0f), size.copy(width = size.width - split))
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
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        // Отметка «переносим» — тем же кружком, что у конвертов: пустой — на месте, «✓» — перенесли.
        when {
            locked -> FinneyIcon(FinneyIcons.Lock, size = 22.dp)
            checked -> CheckBadge(size = 24.dp)
            else -> Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White).border(2.dp, FinneyInk, CircleShape))
        }
        ItemPicture(spending, spending.label, 32.dp)
        Text(spending.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(
            if (locked) "нужное" else "хочется",
            style = MaterialTheme.typography.labelMedium,
            color = FinneyInk,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(FinneyCream)
                .border(StrokeThin, FinneyInk, RoundedCornerShape(6.dp))
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
