package com.soapjournal.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.prefs.UserPreferences
import com.soapjournal.app.ui.bible.BibleScreen
import com.soapjournal.app.ui.editor.EntryEditorScreen
import com.soapjournal.app.ui.editor.EntryEditorViewModel
import com.soapjournal.app.ui.history.HistoryScreen
import com.soapjournal.app.ui.history.HistoryViewModel
import com.soapjournal.app.ui.home.HomeScreen
import com.soapjournal.app.ui.home.HomeViewModel
import com.soapjournal.app.ui.insights.InsightsScreen
import com.soapjournal.app.ui.insights.InsightsViewModel
import com.soapjournal.app.ui.memory.MemoryScreen
import com.soapjournal.app.ui.plan.ReadingPlanScreen
import com.soapjournal.app.ui.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EDITOR = "editor/{entryId}"
    const val BIBLE = "bible"
    const val PLAN = "plan"
    const val MEMORY = "memory"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"

    fun editor(entryId: Long) = "editor/$entryId"
}

private val enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 30 }
private val exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 40 }
private val popEnter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 40 }
private val popExit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 30 }

@Composable
fun SoapNavHost(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var prefsSnapshot by remember { mutableStateOf(UserPreferences()) }

    LaunchedEffect(Unit) {
        val prefs = container.prefs.preferences.first()
        prefsSnapshot = prefs
        startDestination = resolveStartDestination(prefs)
    }

    val start = startDestination
    if (start == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        SoapNavGraph(
            container = container,
            resumeDestination = start,
            prefsSnapshot = prefsSnapshot,
            scope = scope
        )
    }
}

@Composable
private fun SoapNavGraph(
    container: AppContainer,
    resumeDestination: String,
    prefsSnapshot: UserPreferences,
    scope: CoroutineScope
) {
    // Editor can be the graph root so writing restores without a home flash.
    // Every other screen starts at home so Back always has somewhere to go.
    val graphStart = if (resumeDestination.startsWith("editor/")) {
        resumeDestination
    } else {
        Routes.HOME
    }
    val navController = rememberNavController()
    var trackedRoute by rememberSaveable { mutableStateOf(resumeDestination) }

    DisposableEffect(navController) {
        val listener =
            androidx.navigation.NavController.OnDestinationChangedListener { _, destination, args ->
                val route = destination.route ?: return@OnDestinationChangedListener
                val entryId = args?.getLong("entryId") ?: -1L
                val normalized = when {
                    route.startsWith("editor/") || route == Routes.EDITOR -> {
                        if (entryId > 0L) Routes.editor(entryId) else trackedRoute
                    }
                    else -> route
                }
                trackedRoute = normalized
                scope.launch {
                    container.prefs.setResumeSession(
                        route = when {
                            normalized.startsWith("editor/") -> "editor"
                            else -> normalized
                        },
                        entryId = entryId
                    )
                }
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    LaunchedEffect(resumeDestination) {
        if (graphStart == Routes.HOME && resumeDestination != Routes.HOME) {
            navController.navigate(resumeDestination) {
                launchSingleTop = true
            }
        }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val stuckOnInnerRoot = currentRoute != null &&
        currentRoute != Routes.HOME &&
        navController.previousBackStackEntry == null
    BackHandler(enabled = stuckOnInnerRoot) {
        navController.popOrHome()
    }

    NavHost(
        navController = navController,
        startDestination = graphStart,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { popEnter },
        popExitTransition = { popExit }
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
            val livePrefs by container.prefs.preferences.collectAsStateWithLifecycle(
                initialValue = prefsSnapshot
            )
            HomeScreen(
                viewModel = vm,
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenBible = { navController.navigate(Routes.BIBLE) },
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                onOpenMemory = { navController.navigate(Routes.MEMORY) },
                onOpenInsights = { navController.navigate(Routes.INSIGHTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                resumeEntryId = livePrefs.resumeEntryId.takeIf {
                    livePrefs.resumeRoute == "editor" && it > 0L
                }
            )
        }

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(container.repository)
            )
            HistoryScreen(
                viewModel = vm,
                onBack = { navController.popOrHome() },
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            val initialSection = prefsSnapshot.resumeSection.takeIf {
                prefsSnapshot.resumeEntryId == entryId && it.isNotBlank()
            }
            val factory = remember(entryId, initialSection) {
                EntryEditorViewModel.Factory(
                    repository = container.repository,
                    entryId = entryId,
                    initialSectionKey = initialSection
                )
            }
            val vm: EntryEditorViewModel = viewModel(factory = factory)
            LaunchedEffect(vm.selectedSection) {
                container.prefs.setResumeSession(
                    route = "editor",
                    entryId = entryId,
                    section = vm.selectedSection.key
                )
            }
            EntryEditorScreen(
                viewModel = vm,
                onBack = { navController.popOrHome() }
            )
        }

        composable(Routes.BIBLE) {
            BibleScreen(
                container = container,
                onBack = { navController.popOrHome() },
                onJournalPassage = { reference, text ->
                    scope.launch {
                        val entry = container.repository.getOrCreateTodayEntry(
                            scriptureReference = reference,
                            scriptureText = text
                        )
                        navController.navigate(Routes.editor(entry.id))
                    }
                }
            )
        }

        composable(Routes.PLAN) {
            ReadingPlanScreen(
                container = container,
                onBack = { navController.popOrHome() },
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(Routes.MEMORY) {
            MemoryScreen(
                container = container,
                onBack = { navController.popOrHome() }
            )
        }

        composable(Routes.INSIGHTS) {
            val vm: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory(container))
            InsightsScreen(
                viewModel = vm,
                onBack = { navController.popOrHome() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popOrHome() }
            )
        }
    }
}

internal fun NavController.popOrHome() {
    if (!popBackStack()) {
        navigate(Routes.HOME) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

/**
 * Cold-start route after process death / app switch.
 * Settings is never a start destination — Back would have nowhere to pop.
 */
internal fun resolveStartDestination(prefs: UserPreferences): String {
    return when {
        prefs.resumeRoute == "editor" && prefs.resumeEntryId > 0L ->
            Routes.editor(prefs.resumeEntryId)
        prefs.resumeRoute == "history" -> Routes.HISTORY
        prefs.resumeRoute == "bible" -> Routes.BIBLE
        prefs.resumeRoute == "plan" -> Routes.PLAN
        prefs.resumeRoute == "memory" -> Routes.MEMORY
        prefs.resumeRoute == "insights" -> Routes.INSIGHTS
        else -> Routes.HOME
    }
}
