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
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.adult.AdultEvent
import ru.finney.pet.ui.adult.AdultUiState
import ru.finney.pet.ui.adult.AdultViewModel
import kotlin.random.Random

class AdultViewModelTest : ViewModelTest() {

    private suspend fun viewModel(): AdultViewModel {
        session.createProfile("Финни", PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND))
        return AdultViewModel(session, game, Fixtures.content, Random(42))
    }

    private fun AdultViewModel.unlock() {
        val gate = (uiState.value as AdultUiState.Locked).gate
        answer(gate.answer.toString())
    }

    @Test
    fun `раздел закрыт примером, неверный ответ даёт новый пример`() = test {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        settle()

        val first = (viewModel.uiState.value as AdultUiState.Locked).gate
        viewModel.answer((first.answer + 1).toString())
        settle()
        val second = (viewModel.uiState.value as AdultUiState.Locked).gate
        assertTrue(second.wrongAnswer)

        viewModel.answer(second.answer.toString())
        settle()
        assertTrue(viewModel.uiState.value is AdultUiState.Ready)
    }

    @Test
    fun `бонус взрослого начисляется шагом и не больше лимита за период`() = test {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        settle()
        viewModel.unlock()
        settle()

        var state = viewModel.uiState.value as AdultUiState.Ready
        val limit = Fixtures.content.economy.parentBonus.maxPerPeriod
        val step = Fixtures.content.economy.parentBonus.step
        assertEquals(limit, state.bonusLeft)

        repeat(limit / step) {
            viewModel.addBonus()
            settle()
        }
        state = viewModel.uiState.value as AdultUiState.Ready
        assertEquals(0, state.bonusLeft)
        assertEquals(50 + limit, session.activeGame.first()?.state?.balance)
    }

    @Test
    fun `темы и прогресс видны без оценок, удаление профиля ведёт к первому запуску`() = test {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val events = mutableListOf<AdultEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }
        settle()
        viewModel.unlock()
        settle()

        val state = viewModel.uiState.value as AdultUiState.Ready
        // Все три темы ТЗ видны, даже если в контенте по теме пока нет игр.
        assertEquals(3, state.topics.size)
        assertTrue("ничего ещё не пройдено", state.topics.all { it.passed == 0 })
        assertTrue(state.topics.single { it.title == "сбережения" }.total > 0)

        viewModel.deleteProfile()
        settle()
        assertEquals(listOf<AdultEvent>(AdultEvent.ProfileDeleted(hasProfile = false)), events)
        assertFalse(session.restore())
    }
}
