package ru.finney.pet.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Session

/** Стартовый экран: есть профиль — главный, нет — знакомство с игрой. */
class StartViewModel(private val session: Session) : ViewModel() {

    private val _startDestination = MutableStateFlow<Any?>(null)

    /** null — пока идёт проверка профиля. */
    val startDestination: StateFlow<Any?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value = if (session.restore()) HomeRoute else OnboardingRoute()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { StartViewModel(appContainer().session) }
        }
    }
}
