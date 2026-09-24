package ru.finney.pet.content

import kotlinx.serialization.json.Json
import ru.finney.pet.domain.model.EconomyConfig
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.Glossary
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.TaskDefinition

/** Разбор JSON из assets/content. Не зависит от Android — проверяется unit-тестами. */
object ContentParser {

    /** Неизвестное поле — ошибка: опечатка в JSON не должна молча превращаться в значение по умолчанию. */
    private val json = Json { ignoreUnknownKeys = false }

    const val ECONOMY = "economy.json"
    const val SHOP = "shop.json"
    const val GOALS = "goals.json"
    /** Задания — это мини-игры: у каждой свой движок, docs/minigames.md. */
    const val TASKS = "tasks.json"
    const val GLOSSARY = "glossary.json"

    /** @param read возвращает текст файла по имени из [ECONOMY], [SHOP], [GOALS], [TASKS], [GLOSSARY]. */
    fun parse(read: (String) -> String): GameContent = GameContent(
        economy = decode<EconomyConfig>(ECONOMY, read),
        shop = decode<List<ShopItem>>(SHOP, read),
        goals = decode<List<Goal>>(GOALS, read),
        tasks = decode<List<TaskDefinition>>(TASKS, read),
        glossary = decode<Glossary>(GLOSSARY, read).terms,
    )

    private inline fun <reified T> decode(file: String, read: (String) -> String): T =
        try {
            json.decodeFromString<T>(read(file))
        } catch (e: Exception) {
            throw ContentException(listOf("$file: ${e.message}"), e)
        }
}

class ContentException(val errors: List<String>, cause: Throwable? = null) :
    IllegalStateException("Ошибки в контенте:\n" + errors.joinToString("\n"), cause)
