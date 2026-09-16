package ru.finney.pet

import android.content.Context
import ru.finney.pet.content.AssetContentLoader
import ru.finney.pet.data.db.FinneyDatabase
import ru.finney.pet.data.repository.RoomGameStorage
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameStore
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

    /** Экраны работают только через него: команды игры с сохранением и наблюдение за профилями. */
    val gameStore: GameStore by lazy { GameStore(game, RoomGameStorage(database.gameDao())) }
}
