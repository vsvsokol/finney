package ru.finney.pet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.finney.pet.ui.adult.AdultScreen
import ru.finney.pet.ui.adult.ResetProgressScreen
import ru.finney.pet.ui.budget.BudgetScreen
import ru.finney.pet.ui.goals.GoalsScreen
import ru.finney.pet.ui.home.HomeScreen
import ru.finney.pet.ui.onboarding.OnboardingScreen
import ru.finney.pet.ui.onboarding.PetSetupScreen
import ru.finney.pet.ui.period.PeriodResultScreen
import ru.finney.pet.ui.progress.GlossaryScreen
import ru.finney.pet.ui.progress.ProgressScreen
import ru.finney.pet.ui.settings.SettingsScreen
import ru.finney.pet.ui.shop.ShopScreen
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Music
import ru.finney.pet.ui.wardrobe.WardrobeScreen
import ru.finney.pet.ui.tasks.TaskScreen
import ru.finney.pet.ui.tasks.TasksScreen

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
    BackgroundMusic(navController)
    NavHost(navController = navController, startDestination = start, modifier = modifier) {

        // ---------- Первый запуск ----------

        composable<OnboardingRoute> { entry ->
            val route = entry.toRoute<OnboardingRoute>()
            OnboardingScreen(
                isReplay = route.isReplay,
                onFinish = {
                    if (route.isReplay) navController.popBackStack() else navController.navigate(PetSetupRoute())
                },
                onStartDemo = { navController.navigate(PetSetupRoute(isDemo = true)) },
            )
        }

        composable<PetSetupRoute> { entry ->
            val route = entry.toRoute<PetSetupRoute>()
            PetSetupScreen(
                isEditing = route.isEditing,
                isDemo = route.isDemo,
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
                onOpenWardrobe = { navController.navigate(WardrobeRoute) },
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenHelp = { navController.navigate(OnboardingRoute(isReplay = true)) },
                onPeriodClosed = { number -> navController.navigate(PeriodResultRoute(number)) },
            )
        }

        composable<BudgetRoute> {
            BudgetScreen(onBack = { navController.popBackStack() })
        }

        composable<ShopRoute> {
            ShopScreen(
                onBack = { navController.popBackStack() },
                onOpenBudget = { navController.navigate(BudgetRoute) },
                onOpenHistory = { navController.navigate(ProgressRoute) },
            )
        }

        composable<WardrobeRoute> {
            WardrobeScreen(
                onBack = { navController.popBackStack() },
                onOpenGoals = { navController.navigate(GoalsRoute) },
            )
        }

        composable<GoalsRoute> {
            GoalsScreen(onBack = { navController.popBackStack() })
        }

        composable<TasksRoute> {
            TasksScreen(
                onOpenTask = { taskId -> navController.navigate(TaskRoute(taskId)) },
                onOpenBudget = { navController.navigate(BudgetRoute) },
                onBack = { navController.popBackStack() },
            )
        }

        // Задание и мини-игра — один экран: какую игру показать, решает движок задания.
        composable<TaskRoute> { entry ->
            val route = entry.toRoute<TaskRoute>()
            TaskScreen(
                taskId = route.taskId,
                onBack = { navController.popBackStack() },
                onOpenBudget = { navController.navigate(BudgetRoute) },
            )
        }

        composable<PeriodResultRoute> { entry ->
            val route = entry.toRoute<PeriodResultRoute>()
            PeriodResultScreen(
                periodNumber = route.periodNumber,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ProgressRoute> {
            ProgressScreen(
                onBack = { navController.popBackStack() },
                onOpenGlossary = { navController.navigate(GlossaryRoute) },
                onOpenPeriodResult = { number -> navController.navigate(PeriodResultRoute(number)) },
            )
        }

        composable<GlossaryRoute> {
            GlossaryScreen(onBack = { navController.popBackStack() })
        }

        // ---------- Взрослый ----------

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAdult = { navController.navigate(AdultRoute) },
            )
        }

        composable<AdultRoute> {
            AdultScreen(
                onBack = { navController.popBackStack() },
                onEditPet = { navController.navigate(PetSetupRoute(isEditing = true)) },
                onResetProgress = { navController.navigate(ResetProgressRoute) },
                onProfileDeleted = { hasProfile -> navController.restart(hasProfile) },
            )
        }

        composable<ResetProgressRoute> {
            ResetProgressScreen(
                onDone = { navController.openGame() },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/** Музыка по текущему экрану: в играх — своя, в знакомстве — своя, иначе — тема комнаты. */
@Composable
private fun BackgroundMusic(navController: NavController) {
    val sounds = LocalSounds.current
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val track = when {
        destination == null -> null
        destination.hasRoute<TaskRoute>() -> Music.Games
        destination.hasRoute<OnboardingRoute>() || destination.hasRoute<PetSetupRoute>() -> Music.Setup
        else -> Music.Room
    }
    LaunchedEffect(track) { sounds.music(track) }
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
