package ru.finney.pet.ui.tasks.games

import ru.finney.pet.ui.motion.motionEnabled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.RaceEvent
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.ui.components.AlertBadge
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FillBar
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.HeartIcon
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.pulse
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeBold
import ru.finney.pet.ui.theme.StrokeRegular

// «Дорога к цели», как ходилка в Тамагочи, только фишку двигает копилка, а не
// кубик. Каждый день Финни получает доход, и ребёнок решает, куда его деть:
// в копилку или на радость. Иногда по дороге соблазн — «Финни хочет наклейки»:
// купить или нет, ребёнок выбирает кнопкой.
//
// Рядом с копилкой — сердечки Финни. Отказался от соблазна дня — одно гаснет,
// позволил себе — загорается. Погасли все — Финни грустит до финиша, и цель не
// засчитана: копить учимся понемногу, а не «никогда ничего».
//
// Плейтест: без вступления ребёнок видел «осталось 45», полоску и «−5 / +5»,
// не понимал, что это, и жал «Готово» наугад; наклейки на втором ходу ставили
// в тупик, доход в 10 монет было не видно. Поэтому теперь экран объясняет себя
// сам: доход приходит монетами с «пришло +10», монеты лежат в двух подписанных
// ящиках, соблазн — с кнопками «Купить» и «Не надо», банка копилки наполняется,
// а следующий шаг пульсирует. Правила прежние — их считает ядро.

@Composable
internal fun GoalRaceGame(
    task: GoalRaceTask,
    character: PetCharacter,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var deposits by remember { mutableStateOf(emptyList<Int>()) }
    val day = deposits.size + 1
    val event = task.events.firstOrNull { it.day == day }
    // Каждый день начинается с того, что весь доход в копилке: в день без соблазна
    // это и есть лучший ход, а в день с соблазном ребёнок решает сам.
    var today by remember(day) { mutableIntStateOf(task.incomePerDay) }
    // С соблазном «Следующий день» ждёт решения: наугад дальше не пройти.
    var decided by remember(day) { mutableStateOf(event == null) }
    val saved = task.startSaved + deposits.sum()
    val mood = TaskEngines.raceMood(task, deposits).lastOrNull() ?: task.mood
    val firstEventDay = task.events.minOfOrNull { it.day }
    val sounds = LocalSounds.current

    // Доход дня звенит, когда монеты высыпаются в ящики.
    LaunchedEffect(day) {
        delay(IncomeDelayMs)
        sounds.play(Sfx.Coin)
    }

    fun split(toPiggy: Int) {
        today = toPiggy.coerceIn(0, task.incomePerDay)
        decided = true
    }

    GameScene(backdrop = Backdrop.FIELD, onClose = onClose) {
        SceneBody(
            bottom = {
                // Подсказка первого хода — пульсом: первый день и первый соблазн.
                val teach = day == 1 || day == firstEventDay
                FinneyButton(
                    text = if (day == task.days) "Финиш" else "Следующий день",
                    enabled = decided,
                    modifier = Modifier.pulse(decided && teach),
                    onClick = {
                        if (today > 0) sounds.play(Sfx.PiggyIn)
                        val all = deposits + today
                        if (all.size == task.days) onSubmit(TaskInput.DailyDeposits(all)) else deposits = all
                    },
                )
            },
        ) {
            Road(task, deposits, day, character, sad = mood == 0)
            PiggyCard(task, saved = saved, pending = today, mood = mood, lastDeposit = deposits.lastOrNull(), depositCount = deposits.size)
            Spacer(Modifier.weight(1f))
            ScenePanel(title = null, modifier = Modifier.fillMaxWidth()) {
                DayHeader(day, task.days, task.incomePerDay)
                Buckets(task, day = day, toPiggy = today, onPiggy = { split(today + task.step) }, onJoy = { split(today - task.step) })
                event?.let {
                    EventCard(
                        task = task,
                        event = it,
                        choice = if (decided) TaskEngines.raceTaken(task, it, today) else null,
                        moodCounts = mood > 0,
                        hint = !decided,
                        onBuy = { split((task.incomePerDay - it.price) / task.step * task.step) },
                        onSkip = { split(task.incomePerDay) },
                    )
                }
                if (mood == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlertBadge(size = 28.dp)
                        Text("Финни загрустил: без радостей цель не засчитается.", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                    }
                } else {
                    Forecast(task, savedAfterToday = saved + today, daysLeft = task.days - day)
                }
            }
        }
    }
}

/** Монеты дня высыпаются в ящики чуть позже начала дня — чтобы было видно, что они пришли. */
private const val IncomeDelayMs = 250L

/**
 * Хватит ли до цели: сколько ещё и успеваешь ли, если откладывать весь доход.
 * Цель — картинкой: склонять название из контента («до самоката») нельзя.
 */
@Composable
private fun Forecast(task: GoalRaceTask, savedAfterToday: Int, daysLeft: Int) {
    val left = task.goal.price - savedAfterToday
    // Набрано — дальше копить незачем, а Финни радость нужна: не толкаем отложить и это.
    if (left <= 0) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckBadge(size = 28.dp)
            Text("Набрано! Можно порадовать Финни.", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
        }
        return
    }
    val perDay = if (daysLeft == 0) Int.MAX_VALUE else ((left + daysLeft - 1) / daysLeft + task.step - 1) / task.step * task.step
    val inTime = perDay <= task.incomePerDay
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "До цели «${task.goal.label}» ещё $left, " + if (inTime) "успеваешь" else "к сроку не успеть"
        },
    ) {
        ItemPicture(task.goal, task.goal.label, 32.dp)
        Text("ещё $left", style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(if (inTime) "успеваешь" else "не успеть", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
        if (inTime) CheckBadge(size = 26.dp) else AlertBadge(size = 26.dp)
    }
}

/** «День 2» и доход дня: «пришло +10» с монеткой выскакивает, когда начинается день. */
@Composable
private fun DayHeader(day: Int, days: Int, income: Int) {
    val pop = remember(day) { Animatable(0f) }
    LaunchedEffect(day) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedText("День $day", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = "День $day из $days" })
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(RoundedCornerShape(50))
                .background(FinneyYellow)
                .border(StrokeRegular, FinneyInk, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .clearAndSetSemantics { contentDescription = "Сегодня пришло $income монет" },
        ) {
            Text("пришло", style = MaterialTheme.typography.labelMedium, color = FinneyInk)
            OutlinedText("+$income", style = MaterialTheme.typography.titleLarge)
            Coin(size = 22.dp)
        }
    }
}

/**
 * Два ящика с монетами дня: «в копилку» и «на радость». Нажатие на ящик
 * перекладывает в него шаг взноса из соседнего — монеты видно, куда ушли.
 * Раньше монеты делила черта, которую надо было тянуть, а подписей не было.
 */
@Composable
private fun Buckets(task: GoalRaceTask, day: Int, toPiggy: Int, onPiggy: () -> Unit, onJoy: () -> Unit) {
    // Монеты высыпаются в ящики каждый новый день — это и есть «пришёл доход».
    var arrived by remember(day) { mutableStateOf(false) }
    LaunchedEffect(day) {
        delay(IncomeDelayMs)
        arrived = true
    }
    val income = task.incomePerDay
    // Одна монетка — одна финка, пока их не больше двадцати; иначе монетка — шаг взноса.
    val unit = if (income <= 20) 1 else task.step
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Bucket(
            icon = { FinneyIcon(FinneyIcons.Piggy, size = 24.dp) },
            label = "в копилку",
            amount = toPiggy,
            slots = income / unit,
            unit = unit,
            fromStart = 0,
            arrived = arrived,
            coin = FinneyYellow,
            step = task.step,
            canTake = toPiggy + task.step <= income,
            onTake = onPiggy,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Bucket(
            icon = { HeartIcon(filled = true, size = 22.dp) },
            label = "на радость",
            amount = income - toPiggy,
            slots = income / unit,
            unit = unit,
            fromStart = toPiggy / unit,
            arrived = arrived,
            coin = FinneyPeach,
            step = task.step,
            canTake = toPiggy - task.step >= 0,
            onTake = onJoy,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

/**
 * Ящик: значок и подпись, монеты и сумма числом. Весь ящик — кнопка «+5 сюда».
 * [fromStart] — с какой по счёту монеты дня лежат монеты этого ящика: так при
 * перекладывании гаснут и загораются одни и те же монетки, а не все разом.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bucket(
    icon: @Composable () -> Unit,
    label: String,
    amount: Int,
    slots: Int,
    unit: Int,
    fromStart: Int,
    arrived: Boolean,
    coin: Color,
    step: Int,
    canTake: Boolean,
    onTake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val count = amount / unit
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(16.dp))
            .clickable(enabled = canTake, role = Role.Button, onClickLabel = "Положить $step $label") {
                sounds.play(Sfx.Coin)
                onTake()
            }
            .semantics { contentDescription = "$label $amount" }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            icon()
            Text(label, style = MaterialTheme.typography.labelMedium, color = FinneyInk, modifier = Modifier.weight(1f))
            OutlinedText(amount.toString(), style = MaterialTheme.typography.titleLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                maxItemsInEachRow = 5,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 18.dp),
            ) {
                // Все монеты дня на местах, лишние — невидимые: ящик не прыгает по высоте.
                repeat(slots) { i ->
                    val shown = arrived && i < count
                    val scale by animateFloatAsState(
                        if (shown) 1f else 0f,
                        tween(durationMillis = 180, delayMillis = (fromStart + i) * 35),
                        label = "coin",
                    )
                    Box(
                        Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(coin)
                            .border(2.dp, FinneyInk, CircleShape),
                    )
                }
            }
            // «+5» — что будет по нажатию. Взять больше нечего — метка прозрачная,
            // ящик не нажимается; место под неё остаётся, чтобы монеты не прыгали.
            Text(
                "+$step",
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
                modifier = Modifier
                    .alpha(if (canTake) 1f else 0f)
                    .clip(RoundedCornerShape(50))
                    .background(coin)
                    .border(2.dp, FinneyInk, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp),
            )
        }
    }
}

/**
 * Соблазн дня: что хочет Финни и две кнопки — «Купить» и «Не надо». На кнопках —
 * что станет с настроением. Кнопки лишь раскладывают монеты дня за ребёнка; взят
 * соблазн или нет, решает ядро по тому, сколько монет осталось на радость.
 * [choice] null — ещё не решено: кнопки пульсируют, дальше не пройти.
 */
@Composable
private fun EventCard(
    task: GoalRaceTask,
    event: RaceEvent,
    choice: Boolean?,
    moodCounts: Boolean,
    hint: Boolean,
    onBuy: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(16.dp))
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemPicture(event, event.label, 44.dp)
            Column(Modifier.weight(1f)) {
                Text("Финни хочет:", style = MaterialTheme.typography.labelMedium, color = FinneyInk)
                Text(event.label, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            }
            PriceTag(event.price.toString())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceButton(
                text = "Купить",
                up = true,
                selected = choice == true,
                enabled = event.price <= task.incomePerDay,
                moodCounts = moodCounts,
                modifier = Modifier.weight(1f).pulse(hint),
                onClick = onBuy,
            )
            ChoiceButton(
                text = "Не надо",
                up = false,
                selected = choice == false,
                enabled = true,
                moodCounts = moodCounts,
                modifier = Modifier.weight(1f).pulse(hint),
                onClick = onSkip,
            )
        }
    }
}

/** Кнопка выбора: слово и «+♥» / «−♥». Выбранная — жёлтая, с толстой рамкой и «✓». */
@Composable
private fun ChoiceButton(
    text: String,
    up: Boolean,
    selected: Boolean,
    enabled: Boolean,
    moodCounts: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(50))
            .background(if (selected) FinneyYellow else FinneyCream)
            .border(if (selected) StrokeBold else StrokeRegular, FinneyInk, RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button) {
                sounds.play(Sfx.Tap)
                onClick()
            }
            .semantics {
                this.selected = selected
                contentDescription = text + if (moodCounts) (if (up) ", настроение плюс одно" else ", настроение минус одно") else ""
            }
            .padding(horizontal = 8.dp),
    ) {
        if (selected) CheckBadge(size = 22.dp)
        Text(text, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        // Грустному Финни сердечки уже не вернуть — значок не обещает лишнего.
        if (moodCounts) {
            Text(if (up) "+" else "−", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            HeartIcon(filled = up, size = 18.dp)
        }
    }
}

/**
 * Копилка: банка наполняется монетами, рядом «сколько из скольки» с полосой и
 * настроение Финни. Сегодняшний взнос виден заранее — светлой полосой и уровнем
 * в банке; положили — над банкой всплывает «+5».
 */
@Composable
private fun PiggyCard(task: GoalRaceTask, saved: Int, pending: Int, mood: Int, lastDeposit: Int?, depositCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinneyCream)
            .border(StrokeRegular, FinneyInk, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            PiggyJar(saved = saved, pending = pending, price = task.goal.price)
            DepositPop(lastDeposit, depositCount)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Сердечки — в той же строке: их «+♥ / −♥» стоят на кнопках соблазна, так что
            // что они значат, видно и без подписи, а поле дороги не теряет высоту.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Копилка", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                OutlinedText("$saved / ${task.goal.price}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Hearts(mood, task.mood)
            }
            FillBar(
                value = saved,
                max = task.goal.price,
                pending = pending,
                modifier = Modifier.fillMaxWidth(),
                description = "В копилке $saved из ${task.goal.price}, сегодня плюс $pending",
            )
        }
    }
}

/** «+5» над банкой, когда взнос дня лёг в копилку. Всплывает и тает. */
@Composable
private fun DepositPop(amount: Int?, count: Int) {
    val rise = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count == 0 || (amount ?: 0) <= 0) return@LaunchedEffect
        rise.snapTo(0f)
        // Без анимаций «+N» пропал бы в тот же кадр: стоит на месте, пока всплывал бы.
        if (motionEnabled()) {
            rise.animateTo(1f, tween(durationMillis = 1000))
        } else {
            delay(1_000)
            rise.snapTo(1f)
        }
    }
    if (rise.value < 1f && amount != null) {
        OutlinedText(
            "+$amount",
            style = MaterialTheme.typography.titleLarge,
            fill = FinneyYellow,
            modifier = Modifier
                .graphicsLayer {
                    translationY = -rise.value * 28.dp.toPx()
                    alpha = (2f - rise.value * 2f).coerceIn(0f, 1f)
                }
                .clearAndSetSemantics {},
        )
    }
}

/** Банка копилки: жёлтые монеты поднимаются по мере накопления, сегодняшние — светлее. */
@Composable
private fun PiggyJar(saved: Int, pending: Int, price: Int) {
    val fill by animateFloatAsState((saved.toFloat() / price).coerceIn(0f, 1f), tween(700), label = "jar")
    val extra by animateFloatAsState((pending.toFloat() / price).coerceIn(0f, 1f), tween(300), label = "jarToday")
    Canvas(Modifier.size(width = 48.dp, height = 58.dp)) {
        val lid = 9.dp.toPx()
        val stroke = StrokeRegular.toPx()
        val radius = CornerRadius(12.dp.toPx())
        val body = Path().apply { addRoundRect(RoundRect(0f, lid, size.width, size.height, radius)) }
        val bodyHeight = size.height - lid
        val done = bodyHeight * fill
        val plus = bodyHeight * extra.coerceAtMost(1f - fill)
        clipPath(body) {
            drawRect(Color.White)
            drawRect(FinneyYellow.copy(alpha = 0.5f), topLeft = Offset(0f, size.height - done - plus), size = Size(size.width, plus))
            drawRect(FinneyYellow, topLeft = Offset(0f, size.height - done), size = Size(size.width, done))
            // Монеты стопками — ряды кружков по заполненной части, чтобы банка читалась как копилка.
            val coin = 5.dp.toPx()
            var y = size.height - coin
            while (y > size.height - done) {
                var x = coin * 1.4f
                while (x < size.width) {
                    drawCircle(FinneyInk.copy(alpha = 0.18f), coin, Offset(x, y), style = Stroke(1.5.dp.toPx()))
                    x += coin * 2.4f
                }
                y -= coin * 1.6f
            }
        }
        drawPath(body, FinneyInk, style = Stroke(stroke))
        drawRoundRect(FinneyPeach, Offset(4.dp.toPx(), 0f), Size(size.width - 8.dp.toPx(), lid), CornerRadius(4.dp.toPx()))
        drawRoundRect(FinneyInk, Offset(4.dp.toPx(), 0f), Size(size.width - 8.dp.toPx(), lid), CornerRadius(4.dp.toPx()), style = Stroke(stroke))
    }
}

/**
 * Дорога одной строкой: кружки дней, над текущим — фишка-питомец, под днями
 * с соблазном — его картинка (видно заранее, где будет трудно), на финише — цель
 * с ценой. Прошлый соблазн, от которого отказались, бледнеет.
 */
@Composable
private fun Road(task: GoalRaceTask, deposits: List<Int>, day: Int, character: PetCharacter, sad: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(RoadHeight)) {
        val goalSlot = 64.dp
        val stepX = (maxWidth - goalSlot) / task.days
        val circle = (stepX - 6.dp).coerceIn(26.dp, 40.dp)
        val token = 40.dp
        val cy = token - 4.dp + circle / 2
        fun cx(i: Int): Dp = stepX * i + stepX / 2
        val goalX = maxWidth - goalSlot / 2

        Canvas(Modifier.fillMaxSize()) {
            val y = cy.toPx()
            drawLine(FinneyCream, Offset(cx(0).toPx(), y), Offset(goalX.toPx(), y), 12.dp.toPx(), StrokeCap.Round)
            drawLine(
                FinneyInk,
                Offset(cx(0).toPx(), y),
                Offset(goalX.toPx(), y),
                3.dp.toPx(),
                StrokeCap.Round,
                PathEffect.dashPathEffect(floatArrayOf(4f, 18f)),
            )
        }

        (1..task.days).forEach { d ->
            val x = cx(d - 1)
            Box(
                Modifier
                    .offset(x - circle / 2, cy - circle / 2)
                    .size(circle)
                    .clip(CircleShape)
                    .background(if (d < day) FinneyYellow else if (d == day) Color.White else FinneyCream)
                    .border(if (d == day) StrokeBold else StrokeRegular, FinneyInk, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(d.toString(), style = MaterialTheme.typography.labelLarge, color = FinneyInk)
            }
            task.events.firstOrNull { it.day == d }?.let { event ->
                val refused = d < day && deposits.getOrNull(d - 1)?.let { !TaskEngines.raceTaken(task, event, it) } == true
                ItemPicture(
                    event,
                    event.label,
                    24.dp,
                    Modifier.offset(x - 12.dp, cy + circle / 2 + 2.dp).alpha(if (refused) 0.35f else 1f),
                )
            }
        }

        Column(Modifier.offset(goalX - goalSlot / 2, cy - 30.dp).width(goalSlot), horizontalAlignment = Alignment.CenterHorizontally) {
            ItemPicture(task.goal, task.goal.label, 44.dp)
            PriceTag(task.goal.price.toString())
        }

        // Фишка переходит на следующий день плавно, а не прыжком.
        val here by animateDpAsState(cx((day - 1).coerceIn(0, task.days - 1)), tween(500), label = "token")
        ScenePet(
            character,
            token,
            Modifier.width(token).offset(here - token / 2, cy - circle / 2 - token + 8.dp),
            mood = if (sad) PetMood.SAD else PetMood.HAPPY,
        )
    }
}

/** Высота дороги: фишка над кружком, сам кружок и картинка соблазна под ним. */
private val RoadHeight = 104.dp

/** Сердечки настроения Финни: [count] горят из [max]. */
@Composable
internal fun Hearts(count: Int, max: Int, size: Dp = 22.dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.semantics { contentDescription = "Настроение $count из $max" },
    ) {
        repeat(max) { HeartIcon(filled = it < count, size = size) }
    }
}
