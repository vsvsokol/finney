package ru.finney.pet.ui.tasks.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow

// «Лимонадная лавка», как подработки в Pou, но с расходами. Утром закупка,
// днём ребёнок сам наливает каждому гостю — без таймера, гости ждут. Лавка
// закрылась — итог дня «получил − потратил = заработал» на экране итога.
// Оценивается только закупка: ровно столько сырья, сколько нужно на всех гостей.

@Composable
internal fun StandGame(
    task: StandTask,
    character: PetCharacter,
    inputError: TaskInputError?,
    onInputSeen: () -> Unit,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var count by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    val cost = count * task.ingredient.price

    if (!open) {
        Morning(task, character, count, inputError, onClose, onChange = { count = it; onInputSeen() }, onOpen = { open = true })
    } else {
        Day(task, character, count, money = task.budget - cost, onClose = onClose, onDone = { onSubmit(TaskInput.Stock(count)) })
    }
}

@Composable
private fun Morning(
    task: StandTask,
    character: PetCharacter,
    count: Int,
    inputError: TaskInputError?,
    onClose: () -> Unit,
    onChange: (Int) -> Unit,
    onOpen: () -> Unit,
) {
    val i = task.ingredient
    GameScene(backdrop = Backdrop.SKY, onClose = onClose, money = task.budget - count * i.price) {
        Column(
            Modifier.fillMaxSize().padding(top = HudHeight).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScenePet(character, 110.dp, Modifier.width(110.dp))
            ScenePanel(title = "Закупка", modifier = Modifier.fillMaxWidth()) {
                Text("☀ ${task.forecast}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(3.dp, FinneyInk, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                ) {
                    ItemPicture(i, i.label, 36.dp)
                    Text(i.label, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
                    Stepper(
                        value = count.toString(),
                        onMinus = { onChange(count - 1) },
                        onPlus = { onChange(count + 1) },
                        minusEnabled = count > 0,
                        plusEnabled = (count + 1) * i.price <= task.budget,
                        what = i.label,
                    )
                }
                SumRow("1 ${i.label.lowercase()} = ${i.yields} стакана", "${count * i.yields} стаканов")
                SumRow("${i.label} стоит ${i.price}", "${i.price} × $count = ${count * i.price}")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(FinneyYellow).padding(horizontal = 6.dp),
                ) { SumRow("стакан продаём за", task.cupPrice.toString()) }
                if (inputError != null) Note(inputErrorText(inputError), color = FinneyPeach)
                FinneyButton(text = "Открыть лавку", onClick = onOpen, enabled = count > 0)
            }
        }
    }
}

@Composable
private fun Day(task: StandTask, character: PetCharacter, count: Int, money: Int, onClose: () -> Unit, onDone: () -> Unit) {
    var served by remember { mutableIntStateOf(0) }
    val cups = count * task.ingredient.yields
    val waiting = task.guests - served
    val cupsLeft = cups - served
    val done = waiting == 0 || cupsLeft == 0

    GameScene(backdrop = Backdrop.SKY, onClose = onClose, money = money + served * task.cupPrice) {
        Column(Modifier.fillMaxSize().padding(top = HudHeight), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CupStrip(cups = cups, poured = served)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Stall(character, task.cupPrice)
                PourPop(served, task.cupPrice, Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 8.dp))
            }

            when {
                waiting == 0 && cupsLeft > 0 -> Note("Гости кончились, а стаканов осталось $cupsLeft.", color = FinneyPeach)
                cupsLeft == 0 && waiting > 0 -> Note("Лимонад кончился! Без лимонада ушли $waiting.", color = FinneyPeach)
                done -> Note("Всем хватило, и ничего не осталось!", color = FinneyGreen)
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                    // Больше четырёх гостей не рисуем — очередь видна и числом.
                    val others = PetCharacter.entries.filter { it != character }
                    repeat(minOf(waiting, 4)) { n -> Guest(others[n % others.size], 72.dp, Modifier.width(72.dp)) }
                }
                if (done) {
                    FinneyButton(text = "Закрыть лавку", onClick = onDone, fillWidth = false)
                } else {
                    PourButton { served++ }
                }
            }
            Text("Гостей ждёт: $waiting", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        }
    }
}

/** Стаканы дня: налитые — жёлтые, пустые — белые. */
@Composable
private fun CupStrip(cups: Int, poured: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FinneyCream)
            .border(3.dp, FinneyInk, RoundedCornerShape(12.dp))
            .padding(6.dp)
            .semantics { contentDescription = "Налито $poured из $cups стаканов" },
    ) {
        repeat(cups.coerceAtMost(14)) { n ->
            Canvas(Modifier.size(width = 20.dp, height = 26.dp)) {
                val cup = Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width * 0.85f, size.height); lineTo(size.width * 0.15f, size.height); close()
                }
                drawPath(cup, if (n < poured) Color(0xFFF9E27A) else Color.White)
                drawPath(cup, FinneyInk, style = Stroke(3.dp.toPx()))
            }
        }
    }
}

/** Лавка: полосатый навес с фестонами, столбики, питомец за прилавком. */
@Composable
private fun Stall(character: PetCharacter, cupPrice: Int) {
    Box(Modifier.fillMaxWidth(0.85f).height(230.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val awning = 44.dp.toPx()
            val stripe = 26.dp.toPx()
            val stroke = 4.dp.toPx()
            // Столбики
            listOf(10.dp.toPx(), size.width - 18.dp.toPx()).forEach { x ->
                drawRect(Color(0xFFA0715A), Offset(x, awning), Size(8.dp.toPx(), size.height - awning))
                drawRect(FinneyInk, Offset(x, awning), Size(8.dp.toPx(), size.height - awning), style = Stroke(3.dp.toPx()))
            }
            // Навес
            var x = 0f
            var odd = false
            while (x < size.width) {
                drawRect(if (odd) FinneyPeach else FinneyYellow, Offset(x, 0f), Size(minOf(stripe, size.width - x), awning))
                x += stripe
                odd = !odd
            }
            var s = 0f
            while (s < size.width) {
                drawCircle(FinneyPeach, stripe / 2, Offset(s + stripe / 2, awning))
                drawCircle(FinneyInk, stripe / 2, Offset(s + stripe / 2, awning), style = Stroke(3.dp.toPx()))
                s += stripe
            }
            drawRect(FinneyInk, Offset.Zero, Size(size.width, awning), style = Stroke(stroke))
        }
        ScenePet(character, 120.dp, Modifier.width(120.dp).align(Alignment.Center).offset(y = 10.dp))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(4.dp, FinneyInk, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { OutlinedText("Лимонад · $cupPrice", style = MaterialTheme.typography.headlineMedium, fill = FinneyPink) }
    }
}

/** «+5» над лавкой после каждого налитого стакана — монетка ушла в кассу. */
@Composable
private fun PourPop(served: Int, cupPrice: Int, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(served) {
        if (served == 0) return@LaunchedEffect
        visible = true
        delay(700)
        visible = false
    }
    AnimatedVisibility(visible, modifier, exit = fadeOut() + slideOutVertically { -it }) {
        OutlinedText("+$cupPrice", style = MaterialTheme.typography.headlineMedium, fill = FinneyGreen)
    }
}

/** Большая круглая кнопка «Налить» — одно нажатие, один стакан. */
@Composable
private fun PourButton(onPour: () -> Unit) {
    Box(
        Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(FinneyYellow)
            .border(4.dp, FinneyInk, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Налить лимонад", onClick = onPour),
        contentAlignment = Alignment.Center,
    ) { OutlinedText("Налить", style = MaterialTheme.typography.titleLarge, fill = FinneyInk, outline = FinneyCream) }
}
