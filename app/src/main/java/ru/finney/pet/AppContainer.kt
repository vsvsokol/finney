package ru.finney.pet

import android.content.Context
import ru.finney.pet.content.AssetContentLoader
import ru.finney.pet.data.db.FinneyDatabase
import ru.finney.pet.data.prefs.DataStoreActiveProfileStorage
import ru.finney.pet.data.prefs.settingsDataStore
import ru.finney.pet.data.repository.RoomGameStorage
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent

/**
 * Ручной DI: единственное место, где создаются база, репозитории и загрузчик контента.
 * Экраны получают зависимости отсюда через ViewModel-фабрики и не создают их сами —
 * так `ui` не знает про `data` (см. docs/architecture.md).
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val content: GameContent by lazy { AssetContentLoader(appContext).load() }

    val game: Game by lazy { Game(content) }

    private val database: FinneyDatabase by lazy { FinneyDatabase.create(appContext) }

    /** Команды игры с сохранением для любого профиля. Экранам игры удобнее [session]. */
    val gameStore: GameStore by lazy { GameStore(game, RoomGameStorage(database.gameDao())) }

    /** Открытый профиль: его состояние и команды без profileId. */
    val session: Session by lazy {
        Session(gameStore, DataStoreActiveProfileStorage(appContext.settingsDataStore))
    }
}
