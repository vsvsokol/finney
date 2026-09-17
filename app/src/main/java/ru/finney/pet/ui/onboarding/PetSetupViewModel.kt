package ru.finney.pet.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.ProfileResult
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.profile.PetNameError
import ru.finney.pet.domain.profile.ProfileRules

data class PetSetupUiState(
    /** true — повторная настройка: поля заполнены из профиля, прогресс не меняется. */
    val isEditing: Boolean,
    /** При редактировании — пока профиль не загружен. */
    val isLoading: Boolean = isEditing,
    val name: String = "",
    val appearance: PetAppearance = PetAppearance(BodyColor.A, EyesVariant.ROUND),
    /** Показывается после попытки сохранить, сбрасывается при вводе. */
    val nameError: PetNameError? = null,
    val isSaving: Boolean = false,
) {
    val maxNameLength: Int get() = ProfileRules.PET_NAME_MAX_LENGTH
}

sealed interface PetSetupEvent {
    /** Профиль создан и открыт или изменения сохранены. */
    data object Saved : PetSetupEvent
}

class PetSetupViewModel(
    private val session: Session,
    isEditing: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetSetupUiState(isEditing = isEditing))
    val uiState: StateFlow<PetSetupUiState> = _uiState.asStateFlow()

    private val _events = Channel<PetSetupEvent>(Channel.BUFFERED)
    val events: Flow<PetSetupEvent> = _events.receiveAsFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                val profile = session.activeGame.first()?.profile
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = profile?.petName ?: it.name,
                        appearance = profile?.appearance ?: it.appearance,
                    )
                }
            }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name, nameError = null) }

    fun setBodyColor(color: BodyColor) = _uiState.update { it.copy(appearance = it.appearance.copy(bodyColor = color)) }

    fun setEyes(eyes: EyesVariant) = _uiState.update { it.copy(appearance = it.appearance.copy(eyes = eyes)) }

    fun save() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (current.isEditing) {
                session.updateProfile(current.name, current.appearance)
            } else {
                session.createProfile(current.name, current.appearance)
            }
            when (result) {
                is ProfileResult.Saved -> _events.send(PetSetupEvent.Saved)
                is ProfileResult.InvalidName -> _uiState.update { it.copy(nameError = result.error) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    companion object {
        fun factory(isEditing: Boolean) = viewModelFactory {
            initializer { PetSetupViewModel(appContainer().session, isEditing) }
        }
    }
}
