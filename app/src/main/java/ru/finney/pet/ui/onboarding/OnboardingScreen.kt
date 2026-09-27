package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.SavingsIcon
import ru.finney.pet.ui.components.StepDots
import ru.finney.pet.ui.components.categoryIcon
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow
import ru.finney.pet.ui.theme.GapBlock
import ru.finney.pet.ui.theme.GapInner
import ru.finney.pet.ui.theme.GapSection
import ru.finney.pet.ui.theme.strokeFor

// Знакомство — ТЗ п. 2.5.1: цель игры и три типа решений (нужное, желаемое, отложить).
// Три коротких страницы вместо одной длинной: ребёнок 7 лет читает фразу за фразой,
// а не абзац. Тот же экран открывается кнопкой «?» на главном — это подсказка,
// к которой можно вернуться в любой момент.

private const val PAGES = 3

/**
 * [isReplay] — открыто повторно, с главного: в конце «Понятно» и назад.
 * Иначе это первый запуск: в конце — создание питомца или тестовый профиль для проверки.
 */
@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    onFinish: () -> Unit,
    onStartDemo: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = page == PAGES - 1

    FinneyScreen(
        scrollable = true,
        // Разделы страницы — с воздухом: на плейтесте приветствие назвали тесным.
        verticalArrangement = Arrangement.spacedBy(GapSection),
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(GapInner)) {
                FinneyButton(
                    text = when {
                        !last -> "Дальше"
                        isReplay -> "Понятно"
                        else -> "Создать питомца"
                    },
                    onClick = { if (last) onFinish() else page++ },
                )
                if (page > 0) FinneyButton(text = "Назад", onClick = { page-- })
                if (last && !isReplay) {
                    FinneyButton(text = "Режим проверки (демо)", onClick = onStartDemo)
                }
            }
        },
    ) {
        // Где ты — точками, а не «2 из 3»: число здесь ничего не значит.
        StepDots(done = page, total = PAGES, current = page)
        when (page) {
            0 -> MeetPage()
            1 -> DecisionsPage()
            else -> PeriodPage(isReplay)
        }
    }
}

@Composable
private fun Title(text: String) {
    OutlinedText(
        text,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * Текст страницы — кеглем крупнее основного: это первое, что ребёнок читает,
 * и на плейтесте он показался мелким. Абзацы — отдельными строками с отступом.
 */
@Composable
private fun Body(vararg paragraphs: String) {
    // На всю ширину: иначе короткая строка вставала по центру, а длинные — влево.
    Column(verticalArrangement = Arrangement.spacedBy(GapBlock), modifier = Modifier.fillMaxWidth()) {
        paragraphs.forEach { Text(it, style = MaterialTheme.typography.titleMedium, color = FinneyInk) }
    }
}

@Composable
private fun MeetPage() {
    Title("Знакомься: Финни")
    val animation = rememberPetAnimation()
    PetView(
        character = PetCharacter.PUSHISTIK,
        mood = PetMood.HAPPY,
        pose = rememberPoseProvider(animation),
        modifier = Modifier.size(200.dp),
    )
    Body(
        "Это твой питомец. Ему нужны еда, чистота, сон и радость.",
        "Спит он бесплатно, а за остальное платят финками — это игровые монетки. Как их тратить, решаешь ты.",
    )
}

@Composable
private fun DecisionsPage() {
    Title("Три решения")
    // Решения различаются значком, а не заливкой: зелёный у нас — «получилось»,
    // и зелёная карточка «Нужное» читалась как оценка, а не как название.
    Column(verticalArrangement = Arrangement.spacedBy(GapBlock)) {
        Decision(categoryIcon(Category.NEEDS), "Нужное", "Еда и мытьё. Это сначала.")
        Decision(categoryIcon(Category.WANTS), "Хочется", "Игрушки и сладости. Можно подождать.")
        Decision(SavingsIcon, "Отложить", "В копилку — на большую цель.")
    }
}

@Composable
private fun Decision(icon: FinneyIcons, title: String, text: String) {
    FinneyCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FinneyIcon(icon, size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            }
        }
    }
}

@Composable
private fun PeriodPage(isReplay: Boolean) {
    Title("Как идёт игра")
    // Четыре шага уровня — значком и парой слов, а не абзацем. Условия уровня
    // подробно показывает сама кнопка «Завершить уровень».
    Column(verticalArrangement = Arrangement.spacedBy(GapBlock)) {
        Step(1, FinneyIcons.Piggy, "Приходят монетки")
        Step(2, FinneyIcons.Plan, "Составь план")
        Step(3, FinneyIcons.Food, "Заботься, играй, копи")
        Step(4, FinneyIcons.Trophy, "Заверши уровень — питомец подрастёт")
    }
    if (isReplay) {
        Body("Ошибаться не страшно.")
    } else {
        Body("Ошибаться не страшно.", "Для взрослых: в демо-режиме все игры открыты сразу.")
    }
}

/** Размер кружка с номером шага: не меньше пальца, как все круглые кнопки. */
private val StepCircle = 48.dp

/**
 * Шаг уровня: номер в кружке, значок и подпись.
 *
 * Номер — в кружке с обводкой, как иконки-кружки UI-кита. Раньше цифра стояла голой
 * у самого края, её контур срезало полем экрана, а разная ширина цифр сдвигала
 * подписи вразнобой.
 */
@Composable
private fun Step(number: Int, icon: FinneyIcons, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Шаг $number: $text" },
    ) {
        Box(
            modifier = Modifier
                .size(StepCircle)
                .clip(CircleShape)
                .background(FinneyYellow)
                .border(strokeFor(StepCircle), FinneyInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Кегль кнопок, а не заголовков: при крупном шрифте системы цифра остаётся в кружке.
            OutlinedText("$number", style = MaterialTheme.typography.titleLarge)
        }
        FinneyIcon(icon, size = 40.dp)
        Text(text, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingPreview() {
    FinneyTheme { OnboardingScreen(isReplay = false, onFinish = {}, onStartDemo = {}) }
}
