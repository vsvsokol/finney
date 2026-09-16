package ru.finney.pet.domain.model

/** Цвет тела. 3 цвета × 3 варианта глаз = 9 комбинаций внешности (ТЗ п. 2.6). */
enum class BodyColor { A, B, C }

enum class EyesVariant { ROUND, OVAL, SLY }

data class PetAppearance(val bodyColor: BodyColor, val eyes: EyesVariant)

/** Локальный игровой профиль: без реального имени, телефона и почты (ТЗ п. 2.5.1, 3.5). */
data class Profile(
    val id: Long,
    val petName: String,
    val appearance: PetAppearance,
    val isDemo: Boolean,
    val createdAt: Long,
)

/** Профиль вместе с игровым состоянием — то, что загружается при запуске. */
data class SavedGame(val profile: Profile, val state: GameState)
