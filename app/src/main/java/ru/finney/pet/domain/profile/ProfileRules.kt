package ru.finney.pet.domain.profile

enum class PetNameError { BLANK, TOO_LONG }

object ProfileRules {
    const val PET_NAME_MAX_LENGTH = 16

    /** Имя питомца после обрезки пробелов или ошибка. */
    fun normalizeName(raw: String): Result = raw.trim().let { name ->
        when {
            name.isEmpty() -> Result.Invalid(PetNameError.BLANK)
            name.length > PET_NAME_MAX_LENGTH -> Result.Invalid(PetNameError.TOO_LONG)
            else -> Result.Valid(name)
        }
    }

    sealed interface Result {
        data class Valid(val name: String) : Result
        data class Invalid(val error: PetNameError) : Result
    }
}
