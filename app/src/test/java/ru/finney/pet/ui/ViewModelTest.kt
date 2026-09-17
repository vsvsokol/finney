package ru.finney.pet.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import ru.finney.pet.domain.FakeActiveProfile
import ru.finney.pet.domain.FakeStorage
import ru.finney.pet.domain.Fixtures
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.Session

/** Основа тестов ViewModel: viewModelScope работает на тестовом диспетчере, игра — в памяти. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    protected val game = Fixtures.game()
    protected val storage = FakeStorage()
    protected val session = Session(GameStore(game, storage), FakeActiveProfile())

    @Before
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    protected fun test(body: suspend TestScope.() -> Unit) = runTest(dispatcher) { body() }

    /** Дать ViewModel доделать запущенные корутины. */
    protected fun TestScope.settle() = advanceUntilIdle()
}
