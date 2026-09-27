package ru.finney.pet.notifications

import ru.finney.pet.domain.model.PetRule
import ru.finney.pet.domain.model.PetStats

/** Чего питомцу хочется сейчас — от этого зависит текст напоминания. */
enum class PetWish { HUNGRY, DIRTY, TIRED, BORED, FINE }

/** Заголовок и строка уведомления. */
data class NotificationText(val title: String, val text: String)

/**
 * Тексты уведомлений. Детские и дружелюбные: питомец зовёт в гости, а не жалуется.
 * Без давления, без «ты плохой хозяин» и без восклицаний о беде — худшее, что с ним
 * бывает, это «проголодался» (docs/economy.md, раздел 5).
 */
object ReminderTexts {

    /**
     * Чего хочется больше всего. Порядок — как у эмоции (PetRules.emotion): сначала еда,
     * потом чистота, сон и радость. Порог — «нужное обеспечено»: ниже него шкала уже видна
     * на главном как просевшая.
     */
    fun wish(stats: PetStats, rule: PetRule): PetWish = when {
        stats.satiety < rule.needsThreshold -> PetWish.HUNGRY
        stats.hygiene < rule.needsThreshold -> PetWish.DIRTY
        stats.energy < rule.needsThreshold -> PetWish.TIRED
        stats.mood < rule.needsThreshold -> PetWish.BORED
        else -> PetWish.FINE
    }

    fun reminder(petName: String, wish: PetWish): NotificationText = when (wish) {
        PetWish.HUNGRY -> NotificationText("$petName проголодался", "Загляни на кухню — вместе выберете, чем перекусить.")
        PetWish.DIRTY -> NotificationText("$petName хочет искупаться", "В ванной его ждут пена и мыльные пузыри.")
        PetWish.TIRED -> NotificationText("$petName зевает", "Уложи его в капсулу — пусть отдохнёт.")
        PetWish.BORED -> NotificationText("$petName скучает по тебе", "Поиграйте вместе — у него в зале есть игрушки.")
        PetWish.FINE -> NotificationText("$petName машет тебе", "У него всё хорошо. Загляни, когда захочешь!")
    }

    fun woke(petName: String): NotificationText =
        NotificationText("$petName проснулся", "Выспался, полон сил и ждёт тебя.")
}
