package ru.finney.pet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme

// Обратная связь после действия — ТЗ п. 2.5.9: ребёнок видит, что изменилось
// (деньги, копилка, шкала питомца), почему и что делать дальше. Отказ устроен
// так же: что не так и какой следующий шаг. Тексты отказов раньше жили в трёх
// экранах по-разному; здесь они в одном месте.

/** Что не получилось и что делать дальше. */
data class RejectionMessage(val problem: String, val next: String)

/** Отказ домена словами для ребёнка. Каждый ответ заканчивается следующим шагом. */
fun rejectionMessage(rejection: Rejection): RejectionMessage = when (rejection) {
    is Rejection.InsufficientFunds -> RejectionMessage(
        "Не хватает ${rejection.shortage} финок.",
        "Сыграй в мини-игру или подожди новый период.",
    )
    is Rejection.InsufficientSavings -> RejectionMessage(
        "В копилке пока только ${rejection.saved}.",
        "Сними меньше.",
    )
    Rejection.PlanNotConfirmed -> RejectionMessage(
        "План периода ещё не готов.",
        "Сначала составь план.",
    )
    Rejection.PlanAlreadyConfirmed -> RejectionMessage(
        "План этого периода уже подтверждён.",
        "Новый план — в следующем периоде.",
    )
    is Rejection.PlanExceedsBudget -> RejectionMessage(
        "В плане ${rejection.planned}, а есть только ${rejection.budget}.",
        "Убери лишнее.",
    )
    Rejection.NoActiveGoal -> RejectionMessage(
        "Цель не выбрана.",
        "Выбери, на что копить.",
    )
    Rejection.GoalAlreadyCompleted -> RejectionMessage(
        "Эту цель ты уже достиг.",
        "Выбери новую цель.",
    )
    is Rejection.GoalNotReached -> RejectionMessage(
        "Накоплено ${rejection.saved} из ${rejection.price}.",
        "Откладывай понемногу — и получится.",
    )
    Rejection.AlreadyOwned -> RejectionMessage(
        "Это у тебя уже есть.",
        "Выбери что-нибудь другое.",
    )
    is Rejection.NotOwned -> RejectionMessage(
        "Этой вещи у тебя пока нет.",
        "Её можно купить в магазине.",
    )
    is Rejection.BonusLimitExceeded -> RejectionMessage(
        "В этом периоде можно добавить ещё ${rejection.left}.",
        "Остальное — в следующем периоде.",
    )
    is Rejection.BonusNotOnStep -> RejectionMessage(
        "Бонус добавляется по ${rejection.step}.",
        "Выбери сумму, кратную ${rejection.step}.",
    )
    Rejection.TaskLocked -> RejectionMessage(
        "Это задание ещё закрыто.",
        "Откроется на следующих уровнях.",
    )
    Rejection.InvalidAmount,
    is Rejection.UnknownGoal,
    is Rejection.UnknownItem,
    is Rejection.NotWearable,
    is Rejection.UnknownTask,
    is Rejection.InvalidTaskInput,
    -> RejectionMessage("Так не получится.", "Попробуй по-другому.")
}

/** Плашка отказа: розовая, но смысл несёт текст, а не цвет (ТЗ п. 3.6). */
@Composable
fun RejectionNote(rejection: Rejection, modifier: Modifier = Modifier) {
    val message = rejectionMessage(rejection)
    FeedbackBox(color = FinneyPink, modifier = modifier) {
        Text(message.problem, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        Text(message.next, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    }
}

/** Одна строка «было → стало». */
data class ChangeLine(val label: String, val before: Int, val after: Int) {
    val delta: Int get() = after - before
}

/** Что произошло после действия: заголовок, изменения, причина и следующий шаг. */
data class ActionFeedback(
    val title: String,
    val lines: List<ChangeLine>,
    val why: String,
    val next: String,
)

/**
 * Изменения между двумя состояниями игры: деньги, копилка, шкалы питомца.
 * Строки без изменений не попадают — ребёнку показывается только то, что сдвинулось.
 */
fun changesBetween(before: GameState, after: GameState): List<ChangeLine> = listOf(
    ChangeLine("Деньги", before.balance, after.balance),
    ChangeLine("Копилка", before.totalSavings, after.totalSavings),
    ChangeLine("Сытость", before.pet.satiety, after.pet.satiety),
    ChangeLine("Чистота", before.pet.hygiene, after.pet.hygiene),
    ChangeLine("Радость", before.pet.mood, after.pet.mood),
).filter { it.delta != 0 }

@Composable
fun ActionFeedbackCard(feedback: ActionFeedback, modifier: Modifier = Modifier) {
    FeedbackBox(color = FinneyGreen, modifier = modifier) {
        Text(feedback.title, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        feedback.lines.forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = "${line.label}: было ${line.before}, стало ${line.after}"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(line.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
                Text(
                    text = "${line.before} → ${line.after}  (${if (line.delta > 0) "+" else ""}${line.delta})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
            }
        }
        Text(feedback.why, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
        Text(feedback.next, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    }
}

/**
 * Итог действия поверх экрана: успех карточкой «было → стало», отказ — что не так
 * и что делать. Экран под окном может быть длинным, и надпись в его начале ребёнок,
 * нажавший кнопку внизу, не увидел бы.
 */
@Composable
fun FeedbackDialog(
    feedback: ActionFeedback?,
    rejection: Rejection?,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    if (feedback == null && rejection == null) return
    Dialog(onDismissRequest = onDismiss) {
        FinneyPanel(title = if (feedback != null) "Готово" else "Не вышло") {
            feedback?.let { ActionFeedbackCard(it) }
            rejection?.let { RejectionNote(it) }
            if (actionLabel != null && onAction != null) {
                FinneyButton(text = actionLabel, onClick = { onDismiss(); onAction() })
            }
            FinneyButton(text = "Понятно", onClick = onDismiss)
        }
    }
}

@Composable
private fun FeedbackBox(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun FeedbackPreview() {
    FinneyTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            ActionFeedbackCard(
                ActionFeedback(
                    title = "Купили мячик",
                    lines = listOf(ChangeLine("Деньги", 40, 25), ChangeLine("Радость", 50, 80)),
                    why = "Это «хочется»: радует, но можно и без него.",
                    next = "Хватит ли на нужное?",
                ),
            )
            RejectionNote(Rejection.InsufficientFunds(needed = 25, balance = 10))
        }
    }
}
