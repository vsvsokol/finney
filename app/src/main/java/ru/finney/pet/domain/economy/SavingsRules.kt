package ru.finney.pet.domain.economy

import ru.finney.pet.domain.model.Period

/** Темп накоплений и срок до цели. docs/economy.md, раздел 6. */
object SavingsRules {
    const val PACE_WINDOW = 3

    /** Сумма пополнений за последние закрытые периоды (не больше [PACE_WINDOW]) и их число. */
    data class Pace(val total: Int, val periods: Int) {
        /** Для текста «если откладывать по N монет». Округление вниз. */
        val average: Int get() = if (periods == 0) 0 else total / periods
    }

    fun pace(periods: List<Period>): Pace {
        val closed = periods.mapNotNull { it.result }.takeLast(PACE_WINDOW)
        return Pace(total = closed.sumOf { it.facts.savings }, periods = closed.size)
    }

    /** Через сколько периодов наберётся [remaining]. null — пока не посчитать: пополнений не было. */
    fun periodsToGoal(remaining: Int, pace: Pace): Int? = when {
        remaining <= 0 -> 0
        pace.total <= 0 -> null
        else -> ceilDiv(remaining * pace.periods, pace.total)
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
}
