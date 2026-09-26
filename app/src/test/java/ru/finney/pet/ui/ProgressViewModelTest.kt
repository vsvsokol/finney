package ru.finney.pet.ui

import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.finney.pet.domain.Fixtures
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.progress.ProgressUiState
import ru.finney.pet.ui.progress.ProgressViewModel

class ProgressViewModelTest : ViewModelTest() {

    private suspend fun viewModel(): ProgressViewModel {
        session.createProfile("Финни", PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND))
        return ProgressViewModel(session, game, Fixtures.content)
    }

    @Test
    fun `у каждой операции в истории есть источник и сумма, новые сверху`() = test {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        session.execute { selectGoal(it, "bike") }
        session.execute { confirmPlan(it, needs = 20, wants = 10, savings = 10) }
        session.execute { buy(it, "candy") }
        settle()

        val state = viewModel.uiState.value as ProgressUiState.Ready
        assertNull("закрытых периодов ещё нет", state.lastClosedPeriod)
        val rows = state.history.single { it.number == 1 }.rows
        assertEquals(
            listOf("Покупка: Конфета (хочется)", "В копилку на «Велосипед»", "Доход периода"),
            rows.map { it.label },
        )
        assertEquals(listOf(-5, -10, 50), rows.map { it.balanceDelta })
        assertEquals(10, rows[1].savingsDelta)
    }

    @Test
    fun `после закрытия периода есть ссылка на его итоги`() = test {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        session.execute { confirmPlan(it, needs = 0, wants = 0, savings = 0) }
        session.execute { closePeriod(it) }
        settle()

        val state = viewModel.uiState.value as ProgressUiState.Ready
        assertEquals(1, state.lastClosedPeriod)
        assertEquals(listOf(2, 1), state.history.map { it.number })
    }
}
