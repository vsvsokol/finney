package ru.finney.pet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Session

/**
 * Демо-режим открытого профиля. Звук и музыка — не здесь: они общие для приложения
 * и живут в LocalSounds.
 */
class SettingsViewModel(private val session: Session) : ViewModel() {

    /** null — профиль ещё не загружен или его нет. */
    val isDemo: StateFlow<Boolean?> = session.activeGame
        .map { it?.profile?.isDemo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDemo(enabled: Boolean) {
        viewModelScope.launch { session.setDemo(enabled) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer().session) }
        }
    }
}
