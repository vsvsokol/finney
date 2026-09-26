package ru.finney.pet.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Text(
            text = "${page + 1} из $PAGES",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )
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

@Composable
private fun Body(text: String) {
    // На всю ширину: иначе короткая строка вставала по центру, а длинные — влево.
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.fillMaxWidth())
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
    Body("Это твой питомец. Ему нужны еда, чистота и радость.")
    Body("За них платят финками — это игровые монетки. Как их тратить, решаешь ты.")
}

@Composable
private fun DecisionsPage() {
    Title("Три решения")
    Decision(FinneyIcons.Food, FinneyGreen, "Нужное", "Еда и мытьё. Это сначала.")
    Decision(FinneyIcons.Star, FinneyPeach, "Хочется", "Игрушки и сладости. Можно подождать.")
    Decision(FinneyIcons.Piggy, FinneyYellow, "Отложить", "В копилку — на большую цель.")
}

@Composable
private fun Decision(icon: FinneyIcons, color: Color, title: String, text: String) {
    FinneyCard(accent = color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FinneyIcon(icon, size = 36.dp)
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
    Body("1. Приходят монетки.")
    Body("2. Составь план.")
    Body("3. Покупай, играй, копи.")
    Body("4. Закончи период — Финни подрастёт.")
    Body("Ошибаться не страшно.")
    if (!isReplay) {
        Body("Для взрослых: в демо-режиме все игры открыты сразу.")
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingPreview() {
    FinneyTheme { OnboardingScreen(isReplay = false, onFinish = {}, onStartDemo = {}) }
}
