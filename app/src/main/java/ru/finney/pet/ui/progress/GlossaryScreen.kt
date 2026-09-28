package ru.finney.pet.ui.progress

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import ru.finney.pet.ui.components.SavingsIcon
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.StrokeThin

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
        OutlinedText("Справочник", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Короткие объяснения слов, которые встречаются в игре.",
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

private fun hasPicture(id: String) = id in Pictured

/**
 * Рисунок к термину — то, что ребёнок видит в игре под этим словом:
 * план — круг с экрана плана, остаток — тот же круг с пустым куском,
 * факт — полосы «собирался / потратил», запас — зонтик «на всякий случай».
 */
@Composable
private fun TermPicture(id: String) {
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
        "fact" -> PlanFactBars()
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

/** Мини-копия «План и факт»: полоса задуманного и полоса того, что вышло. */
@Composable
private fun PlanFactBars() {
    Column(Modifier.size(width = PictureSize, height = 40.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        MiniBar(0.8f, FinneyPeach)
        MiniBar(0.6f, FinneyYellow)
    }
}

@Composable
private fun MiniBar(fraction: Float, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(FinneyCream)
            .border(StrokeThin, FinneyInk, RoundedCornerShape(50)),
    ) {
        drawRect(color, size = size.copy(width = size.width * fraction))
    }
}
