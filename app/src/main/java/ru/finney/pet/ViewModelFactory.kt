package ru.finney.pet

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Доступ к [AppContainer] из фабрики ViewModel:
 *
 * ```
 * companion object {
 *     val Factory = viewModelFactory {
 *         initializer { HomeViewModel(appContainer().session, appContainer().game) }
 *     }
 * }
 * ```
 * На экране: `viewModel(factory = HomeViewModel.Factory)`.
 */
fun CreationExtras.appContainer(): AppContainer = (checkNotNull(this[APPLICATION_KEY]) as FinneyApplication).container
