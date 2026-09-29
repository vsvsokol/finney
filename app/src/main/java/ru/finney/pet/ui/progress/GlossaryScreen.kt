package ru.finney.pet.ui.progress

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.finney.pet.R
import ru.finney.pet.appContainer
import ru.finney.pet.domain.model.GlossaryTerm
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.PlanDonut
import ru.finney.pet.ui.components.JarGlass
import ru.finney.pet.ui.components.SavingsIcon
import ru.finney.pet.ui.theme.FinneyInk

/** Термины из glossary.json — учебный контент, не код (ТЗ п. 2.5.11, 3.2). */
class GlossaryViewModel(val terms: List<GlossaryTerm>) : ViewModel() {
    companion object {
        val Factory = viewModelFactory {
            initializer { GlossaryViewModel(appContainer().content.glossary) }
        }
    }
}

// Плейтест: справочник — «просто набор цифр, нереально скучно». Поэтому у термина
// рисунок из самой игры: круг плана, монеты, копилка, вещь цели. Ребёнок узнаёт
// то, что уже видел на экранах, и слово цепляется за картинку.

/** Сторона рисунка у термина. */
private val PictureSize = 72.dp

@Composable
fun GlossaryScreen(
    onBack: () -> Unit,
    viewModel: GlossaryViewModel = viewModel(factory = GlossaryViewModel.Factory),
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        onClose = onBack,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlossaryBook(size = 44.dp)
            OutlinedText("Справочник", style = MaterialTheme.typography.headlineLarge)
        }
        // Справочник и сон-загадка — одно: загадки берутся отсюда, и ребёнок должен
        // это знать с обеих сторон (плейтест 29.09).
        Text(
            text = "Короткие объяснения слов, которые встречаются в игре. " +
                "Эти же слова снятся питомцу: когда он спит, нажми на облачко с книжкой и отгадай.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )
        viewModel.terms.forEach { term -> TermCard(term) }
    }
}

/** Карточка термина: рисунок слева, слово и объяснение справа. Термин без рисунка — только слова. */
@Composable
private fun TermCard(term: GlossaryTerm) {
    FinneyCard(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hasPicture(term.id)) {
                // Рисунок только поясняет слово — диктор читает сам термин и текст.
                Box(
                    modifier = Modifier.size(PictureSize).clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) { TermPicture(term.id) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(term.term, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Text(term.text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            }
        }
    }
}

/** Термины с рисунком — по id из glossary.json. Новый термин без рисунка просто покажется словами. */
private val Pictured = setOf(
    "budget", "plan", "needs", "wants", "savings", "goal", "balance",
    "remainder", "fact", "period", "reserve", "change", "receipt", "earned",
)

internal fun hasPicture(id: String) = id in Pictured

/**
 * Рисунок к термину — то, что ребёнок видит в игре под этим словом:
 * план — круг с экрана плана, остаток — тот же круг с пустым куском,
 * факт — банка с чертой плана, как на «План и факт», запас — зонтик «на всякий случай».
 */
@Composable
internal fun TermPicture(id: String) {
    when (id) {
        "budget" -> Coins(count = 3)
        "balance" -> Coin(size = 48.dp)
        "change" -> Coins(count = 2)
        "plan" -> PlanDonut(budget = 50, needs = 25, wants = 15, savings = 10, diameter = PictureSize)
        "remainder" -> PlanDonut(budget = 50, needs = 20, wants = 10, savings = 5, diameter = PictureSize)
        "needs" -> TwoItems(R.drawable.item_food_apple, R.drawable.item_care_soap)
        "wants" -> TwoItems(R.drawable.item_treat_candy, R.drawable.item_toy_ball)
        "savings" -> FinneyIcon(SavingsIcon, size = 56.dp)
        "goal" -> Art(R.drawable.acc_hat_cowboy, PictureSize)
        "fact" -> PlanFactJar()
        "period" -> LevelBadge(level = 2, size = 60.dp)
        "reserve" -> Art(R.drawable.item_wear_umbrella, PictureSize)
        "receipt" -> FinneyIcon(FinneyIcons.Cart, size = 56.dp)
        "earned" -> Art(R.drawable.item_food_lemon, PictureSize)
    }
}

@Composable
private fun Art(@DrawableRes res: Int, size: Dp) {
    Image(painter = painterResource(res), contentDescription = null, modifier = Modifier.size(size))
}

/** Две вещи внахлёст: «нужное» — это и еда, и мыло, а не что-то одно. */
@Composable
private fun TwoItems(@DrawableRes first: Int, @DrawableRes second: Int) {
    Box(Modifier.size(PictureSize)) {
        Image(painterResource(first), null, Modifier.size(48.dp).align(Alignment.TopStart))
        Image(painterResource(second), null, Modifier.size(48.dp).align(Alignment.BottomEnd))
    }
}

/** Стопка монет лесенкой — «все монетки, что есть». */
@Composable
private fun Coins(count: Int) {
    Box(Modifier.size(PictureSize)) {
        repeat(count) { i ->
            Coin(size = 40.dp, modifier = Modifier.offset(x = (i * 14).dp, y = (28 - i * 12).dp))
        }
    }
}

/** Мини-копия «План и факт»: та же банка — черта задуманного и заливка того, что вышло. */
@Composable
private fun PlanFactJar() {
    JarGlass(line = 0.7f, shown = 0.45f, over = false, modifier = Modifier.size(width = 40.dp, height = PictureSize))
}

/**
 * Книжка справочника — тот же значок, что в облачке сна и на кнопке к справочнику:
 * одна картинка связывает загадку и справочник. Рисуется фигурами, без файла.
 */
@Composable
fun GlossaryBook(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawBook() }
}

/** Раскрытая книжка: две страницы с корешком посередине и строчки на них. */
private fun DrawScope.drawBook() {
    val w = size.width
    val h = size.height
    val stroke = w * 0.07f
    val top = h * 0.2f
    val bottom = h * 0.85f
    val mid = w / 2f
    fun page(left: Boolean) = Path().apply {
        val outer = if (left) w * 0.06f else w * 0.94f
        moveTo(mid, top + h * 0.06f)
        quadraticTo((mid + outer) / 2f, top - h * 0.06f, outer, top)
        lineTo(outer, bottom - h * 0.04f)
        quadraticTo((mid + outer) / 2f, bottom - h * 0.1f, mid, bottom)
        close()
    }
    listOf(page(true), page(false)).forEach { path ->
        drawPath(path, Color.White)
        drawPath(path, FinneyInk, style = Stroke(stroke))
    }
    for (i in 0 until 3) {
        val y = top + h * (0.18f + 0.15f * i)
        drawLine(FinneyInk, Offset(w * 0.16f, y), Offset(mid - w * 0.08f, y + h * 0.02f), stroke * 0.6f)
        drawLine(FinneyInk, Offset(mid + w * 0.08f, y + h * 0.02f), Offset(w * 0.84f, y), stroke * 0.6f)
    }
}
