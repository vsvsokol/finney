package ru.finney.pet.navigation

import kotlinx.serialization.Serializable

// Маршруты типобезопасной навигации. Новый экран = новый объект здесь + composable в FinneyNavHost.
// profileId в маршрутах нет: экраны игры берут открытый профиль из Session.

// ---------- Первый запуск ----------

/** Цель игры и три типа решений (ТЗ п. 2.5.1). [isReplay] — открыто повторно с главного, в конце назад. */
@Serializable
data class OnboardingRoute(val isReplay: Boolean = false)

/**
 * Имя и внешность питомца. [isEditing] — повторная настройка существующего профиля (ТЗ п. 2.5.2).
 * [isDemo] — тестовый профиль для проверки: все задания открыты сразу (ТЗ п. 2.5.13).
 */
@Serializable
data class PetSetupRoute(val isEditing: Boolean = false, val isDemo: Boolean = false)

// ---------- Игра ----------

@Serializable
data object HomeRoute

// Черновой экран анимаций питомца. Уедет, когда анимации переедут на главный экран.
@Serializable
data object PetLabRoute

/** План бюджета периода: до подтверждения — распределение, после — план против факта. */
@Serializable
data object BudgetRoute

@Serializable
data object ShopRoute

/** Цели и копилка. */
@Serializable
data object GoalsRoute

/** Все задания и мини-игры по темам. */
@Serializable
data object TasksRoute

/** Одно задание или мини-игра: вступление, игра, итог. */
@Serializable
data class TaskRoute(val taskId: String)

/** Итоги закрытого периода: факт, очки, изменение уровня и стадии. */
@Serializable
data class PeriodResultRoute(val periodNumber: Int)

/** История операций, завершённые задания, итоги периодов (ТЗ п. 2.5.4, 2.5.11). */
@Serializable
data object ProgressRoute

/** Справочник терминов (ТЗ п. 2.5.11). */
@Serializable
data object GlossaryRoute

// ---------- Взрослый ----------

/** Раздел взрослого. Барьер (удержание кнопки или пример) — часть этого экрана (ТЗ п. 2.5.12). */
@Serializable
data object AdultRoute

/** Подтверждение сброса прогресса из раздела взрослого. */
@Serializable
data object ResetProgressRoute
