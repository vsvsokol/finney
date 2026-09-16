package ru.finney.pet

import android.content.Context

/**
 * Ручной DI: единственное место, где создаются база, репозитории и загрузчик контента.
 * Экраны получают зависимости отсюда через ViewModel-фабрики и не создают их сами —
 * так `ui` не знает про `data` (см. docs/architecture.md).
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext
}
