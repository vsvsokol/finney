package ru.finney.pet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.finney.pet.ui.budget.BudgetScreen
import ru.finney.pet.ui.home.HomeScreen
import ru.finney.pet.ui.onboarding.PetSetupScreen
import ru.finney.pet.ui.period.PeriodResultScreen
import ru.finney.pet.ui.pet.PetLabScreen

/**
 * Граф экранов. Экраны не знают про NavController: получают лямбды `onOpenX`,
 * а куда ведёт каждая — решается только здесь.
 */
@Composable
fun FinneyNavHost(
    modifier: Modifier = Modifier,
    startViewModel: StartViewModel = viewModel(factory = StartViewModel.Factory),
) {
    val startDestination by startViewModel.startDestination.collectAsStateWithLifecycle()
    val start = startDestination
    if (start == null) {
        // Проверка профиля — доли секунды, отдельный сплэш не нужен.
        Box(modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = start, modifier = modifier) {

        // ---------- Первый запуск ----------

        composable<OnboardingRoute> { entry ->
            val route = entry.toRoute<OnboardingRoute>()
            StubScreen(
                "Знакомство с игрой",
                if (route.isReplay) {
                    "Понятно" to { navController.popBackStack() }
                } else {
                    "Создать питомца" to { navController.navigate(PetSetupRoute()) }
                },
            )
        }

        composable<PetSetupRoute> { entry ->
            val route = entry.toRoute<PetSetupRoute>()
            PetSetupScreen(
                isEditing = route.isEditing,
                onSaved = {
                    if (route.isEditing) navController.popBackStack() else navController.openGame()
                },
            )
        }

        // ---------- Игра ----------

        composable<HomeRoute> {
            HomeScreen(
                onOpenBudget = { navController.navigate(BudgetRoute) },
                onOpenShop = { navController.navigate(ShopRoute) },
                onOpenGoals = { navController.navigate(GoalsRoute) },
                onOpenTasks = { navController.navigate(TasksRoute) },
                onOpenTask = { taskId -> navController.navigate(TaskRoute(taskId)) },
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenAdult = { navController.navigate(AdultRoute) },
                onOpenHelp = { navController.navigate(OnboardingRoute(isReplay = true)) },
                onPeriodClosed = { number -> navController.navigate(PeriodResultRoute(number)) },
                onOpenPetLab = { navController.navigate(PetLabRoute) },
            )
        }

        // Черновой экран анимаций. Удалить вместе с PetLabRoute, когда анимации
        // переедут на главный экран.
        composable<PetLabRoute> { PetLabScreen() }

        composable<BudgetRoute> {
            BudgetScreen(onBack = { navController.popBackStack() })
        }

        composable<ShopRoute> {
            StubScreen("Магазин", "Назад" to { navController.popBackStack() })
        }

        composable<GoalsRoute> {
            StubScreen("Цели и копилка", "Назад" to { navController.popBackStack() })
        }

        composable<TasksRoute> {
            StubScreen("Задания", "Назад" to { navController.popBackStack() })
        }

        composable<TaskRoute> { entry ->
            val route = entry.toRoute<TaskRoute>()
            StubScreen("Задание ${route.taskId}", "Назад" to { navController.popBackStack() })
        }

        composable<PeriodResultRoute> { entry ->
            val route = entry.toRoute<PeriodResultRoute>()
            PeriodResultScreen(
                periodNumber = route.periodNumber,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ProgressRoute> {
            StubScreen(
                "Прогресс",
                "Справочник" to { navController.navigate(GlossaryRoute) },
                "Назад" to { navController.popBackStack() },
            )
        }

        composable<GlossaryRoute> {
            StubScreen("Справочник", "Назад" to { navController.popBackStack() })
        }

        // ---------- Взрослый ----------

        composable<AdultRoute> {
            StubScreen(
                "Раздел взрослого",
                "Имя и внешность питомца" to { navController.navigate(PetSetupRoute(isEditing = true)) },
                "Назад" to { navController.popBackStack() },
            )
        }
    }
}

/** Вход в игру после создания профиля: знакомство и настройка уходят из стека, «назад» закрывает приложение. */
fun NavController.openGame() = navigate(HomeRoute) {
    popUpTo(graph.id) { inclusive = true }
}

/**
 * После удаления профиля из раздела взрослого: [hasProfile] — `Session.restore()`.
 * Остался другой профиль — главный, не осталось — первый запуск.
 */
fun NavController.restart(hasProfile: Boolean) = navigate(if (hasProfile) HomeRoute else OnboardingRoute()) {
    popUpTo(graph.id) { inclusive = true }
}
