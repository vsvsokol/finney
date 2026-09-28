package ru.finney.pet.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import ru.finney.pet.domain.model.GlossaryTerm
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyQuietButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.WarningBadge
import ru.finney.pet.ui.progress.TermPicture
import ru.finney.pet.ui.progress.hasPicture
import ru.finney.pet.ui.theme.FinneyBlue
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Сон со справочником. Пока питомец спит, над капсулой висит облачко-мысль, а в нём —
// книжка с вопросом. Нажал — карточка: объяснение из справочника и три слова, какое
// из них подходит. Сон — единственное место, где игра просто ждёт, а справочник
// (ТЗ п. 2.5.11) раньше находили только через «Прогресс»; здесь слова повторяются
// сами, между делом, как в Duolingo.
//
// Сон не превращается в экзамен: уложил — ничего не выскакивает, облачко просто
// висит, его можно не трогать. Неверный ответ ничего не отнимает и не ругает —
// «Почти!» и верное слово (ТЗ п. 8.1: без стыда и давления). Верный — сон сразу
// прибавляется, и питомец просыпается раньше (Game.answerSleepCard). Сколько сна за
// ответ и сколько карточек за сон — числа economy.json (sleepCards), экран их не придумывает.

/**
 * Карточка: объяснение [clue] и три слова [options], верное — [answer].
 * [termId] — чтобы показать рисунок термина и не повторять его следующим.
 */
data class SleepCard(val termId: String, val clue: String, val options: List<String>, val answer: String)

/**
 * Новая карточка из справочника. Подсказка — первая фраза объяснения: вторая часто
 * называет само слово («Сдача = сколько дал минус…») или уводит в сторону. Термин,
 * чьё объяснение называет само слово, в загадку не берётся. [avoid] — прошлая
 * карточка, чтобы та же не выпала подряд. null — терминов меньше трёх.
 */
fun sleepCard(terms: List<GlossaryTerm>, random: Random, avoid: String? = null): SleepCard? {
    fun clueOf(term: GlossaryTerm) = term.text.substringBefore(". ").trimEnd('.') + "."
    val askable = terms.filter { t ->
        val stem = t.term.lowercase().take(5)
        stem !in clueOf(t).lowercase()
    }
    if (terms.size < 3 || askable.isEmpty()) return null
    val pick = (askable.filter { it.id != avoid }.ifEmpty { askable }).random(random)
    val others = terms.filter { it.id != pick.id }.shuffled(random).take(2)
    return SleepCard(
        termId = pick.id,
        clue = clueOf(pick),
        options = (others.map { it.term } + pick.term).shuffled(random),
        answer = pick.term,
    )
}

/**
 * Облачко-мысль над спящим: три пузырька от головы и облако с книжкой и «?».
 * Покачивается, пока анимации включены. [done] — карточки на этот сон кончились:
 * в облаке «Zz», нажатие ничего не открывает. Размер облака — [DreamCloudSize].
 */
@Composable
fun DreamCloud(done: Boolean, animate: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bob = if (animate) {
        rememberInfiniteTransition(label = "dream").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Reverse),
            label = "bob",
        )
    } else null
    Box(
        modifier = modifier
            .size(DreamCloudSize)
            .graphicsLayer { translationY = (bob?.value ?: 0.5f) * -6.dp.toPx() }
            .clearAndSetSemantics {
                contentDescription = if (done) "Финни снится хороший сон" else "Сон-загадка: слово из справочника"
                if (!done) role = Role.Button
            }
            .then(
                if (done) Modifier else Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            ),
        contentAlignment = Alignment.TopEnd,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawThoughtCloud() }
        Box(Modifier.size(CloudBody), contentAlignment = Alignment.Center) {
            OutlinedText(if (done) "Zz" else "?", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

/** Всё облако с пузырьками: хвост из пузырьков идёт в левый нижний угол — к голове питомца. */
val DreamCloudSize = 112.dp

/** Само облако, без пузырьков: правый верхний угол [DreamCloudSize]. */
private val CloudBody = 84.dp

/** Облако из кругов с синей обводкой и пузырьки-хвост, как «мысль» в комиксе. */
private fun DrawScope.drawThoughtCloud() {
    val body = CloudBody.toPx()
    val left = size.width - body
    val stroke = 3.dp.toPx()
    // Облако — пять кругов внахлёст: сначала все обводки, потом все заливки, чтобы
    // внутренние линии пропали и остался один контур.
    val puffs = listOf(
        Offset(0.30f, 0.55f) to 0.24f,
        Offset(0.52f, 0.36f) to 0.28f,
        Offset(0.74f, 0.52f) to 0.24f,
        Offset(0.52f, 0.66f) to 0.24f,
        Offset(0.32f, 0.34f) to 0.18f,
    ).map { (c, r) -> Offset(left + c.x * body, c.y * body) to r * body }
    puffs.forEach { (c, r) -> drawCircle(FinneyInk, r + stroke, c) }
    puffs.forEach { (c, r) -> drawCircle(Color.White, r, c) }
    // Пузырьки хвоста — всё мельче к голове.
    listOf(
        Offset(left + body * 0.18f, body * 0.92f) to 7.dp.toPx(),
        Offset(left - body * 0.02f, body * 1.10f) to 5.dp.toPx(),
        Offset(left - body * 0.16f, body * 1.24f) to 3.5.dp.toPx(),
    ).forEach { (c, r) ->
        drawCircle(Color.White, r, c)
        drawCircle(FinneyInk, r, c, style = Stroke(stroke * 0.8f))
    }
}

/**
 * Карточка сна. [picked] — какое слово выбрано, null — ещё не отвечали. После ответа
 * кнопки слов гаснут, внизу — «Ещё» (если карточки на этот сон остались) и «Хватит».
 */
@Composable
fun SleepCardPanel(
    petName: String,
    card: SleepCard,
    picked: String?,
    cardsLeft: Int,
    sleepGain: Int,
    onPick: (String) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FinneyPanel(title = "Сон-загадка", onClose = onClose, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        ) {
            if (hasPicture(card.termId)) {
                Box(Modifier.size(72.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                    TermPicture(card.termId)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$petName снится слово:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Text("«${card.clue}»", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            }
        }
        Text(
            "Какое это слово?",
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        card.options.forEach { option ->
            WordButton(
                text = option,
                mark = when {
                    picked == null -> WordMark.NONE
                    option == card.answer -> WordMark.RIGHT
                    option == picked -> WordMark.MISSED
                    else -> WordMark.NONE
                },
                enabled = picked == null,
                onClick = { onPick(option) },
            )
        }
        if (picked != null) {
            val right = picked == card.answer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            ) {
                if (right) CheckBadge(size = 28.dp) else WarningBadge(size = 28.dp)
                Text(
                    text = if (right) "Верно! Сон +$sleepGain — $petName проснётся раньше" else "Почти! Это «${card.answer}»",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinneyInk,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (cardsLeft > 0) {
                    FinneyQuietButton(text = "Хватит", onClick = onClose, modifier = Modifier.weight(1f))
                    FinneyButton(text = "Ещё", onClick = onNext, modifier = Modifier.weight(1f))
                } else {
                    FinneyButton(text = "Пусть спит", onClick = onClose)
                }
            }
        }
    }
}

private enum class WordMark { NONE, RIGHT, MISSED }

/**
 * Слово-ответ карточкой. После ответа верное — с ✓ и жёлтым, выбранное неверное —
 * с «!», остальные просто гаснут: знак и слово, не только цвет (ТЗ п. 3.6).
 * Неверное — голубым предупреждением, а не розовой ошибкой: это загадка во сне, не экзамен.
 */
@Composable
private fun WordButton(text: String, mark: WordMark, enabled: Boolean, onClick: () -> Unit) {
    FinneyCard(
        accent = when (mark) {
            WordMark.RIGHT -> FinneyYellow
            WordMark.MISSED -> FinneyBlue
            WordMark.NONE -> null
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (!enabled && mark == WordMark.NONE) 0.5f else 1f }
            .semantics(mergeDescendants = true) { role = Role.Button }
            .then(if (enabled) Modifier.clickable(onClickLabel = "Ответить: $text", onClick = onClick) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text, style = MaterialTheme.typography.titleLarge, color = FinneyInk, modifier = Modifier.weight(1f))
            when (mark) {
                WordMark.RIGHT -> CheckBadge(size = 28.dp)
                WordMark.MISSED -> WarningBadge(size = 28.dp)
                WordMark.NONE -> Box(Modifier.width(28.dp))
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SleepCardPanelPreview() {
    val card = SleepCard("budget", "Все монетки, которые у тебя есть на этот раз.", listOf("Баланс", "Бюджет", "Запас"), "Бюджет")
    FinneyTheme {
        SleepCardPanel("Финни", card, picked = "Баланс", cardsLeft = 3, sleepGain = 15, onPick = {}, onNext = {}, onClose = {})
    }
}

@Preview
@Composable
private fun DreamCloudPreview() {
    FinneyTheme { DreamCloud(done = false, animate = false, onClick = {}) }
}
