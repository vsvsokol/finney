package ru.finney.pet.ui

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.Fixtures
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.home.HomeEvent
import ru.finney.pet.ui.home.HomeUiState
import ru.finney.pet.ui.home.HomeViewModel

class HomeViewModelTest : ViewModelTest() {

    @Test
    fun `главный показывает открытый профиль и обновляется после команд`() = test {
        session.createProfile("Финни", PetAppearance(PetCharacter.PUSHISTIK, BodyColor.B, EyesVariant.SLY))
        val viewModel = HomeViewModel(session, game, Fixtures.content)
        backgroundScope.launch { viewModel.uiState.collect {} }
        settle()

        var state = viewModel.uiState.value as HomeUiState.Ready
        assertEquals("Финни", state.petName)
        assertEquals(50, state.balance)
        assertEquals(PeriodPhase.PLANNING, state.phase)
        assertFalse(state.canClosePeriod)
        assertEquals("t1", state.nextTask?.id)

        session.submitTask("t1", Fixtures.success)
        session.execute { confirmPlan(it, needs = 0, wants = 0, savings = 0) }
        settle()

        state = viewModel.uiState.value as HomeUiState.Ready
        assertEquals(65, state.balance)
        assertEquals("t2", state.nextTask?.id)
        assertTrue(state.canClosePeriod)
    }

    @Test
    fun `закрытие периода сообщает номер закрытого периода`() = test {
        session.createProfile("Финни", PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND))
        session.execute { confirmPlan(it, needs = 0, wants = 0, savings = 0) }
        val viewModel = HomeViewModel(session, game, Fixtures.content)

        viewModel.closePeriod()

        assertEquals(HomeEvent.PeriodClosed(1), viewModel.events.first())
        assertEquals(2, session.activeGame.first()!!.state.currentPeriod.number)
    }
}
