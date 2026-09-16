package ru.finney.pet.content

import android.content.Context
import ru.finney.pet.domain.model.GameContent

/** Загружает и проверяет контент из assets/content. Ошибка контента — падение при старте, а не тихий баг. */
class AssetContentLoader(private val context: Context) {

    fun load(): GameContent {
        val content = ContentParser.parse { file ->
            context.assets.open("$DIRECTORY/$file").bufferedReader().use { it.readText() }
        }
        val errors = ContentValidator.validate(content)
        if (errors.isNotEmpty()) throw ContentException(errors)
        return content
    }

    private companion object {
        const val DIRECTORY = "content"
    }
}
