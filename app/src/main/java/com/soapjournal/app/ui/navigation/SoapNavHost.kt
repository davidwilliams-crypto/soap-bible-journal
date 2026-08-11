package com.soapjournal.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soapjournal.app.AppContainer
import com.soapjournal.app.ui.bible.BibleScreen
import com.soapjournal.app.ui.editor.EntryEditorScreen
import com.soapjournal.app.ui.editor.EntryEditorViewModel
import com.soapjournal.app.ui.history.HistoryScreen
import com.soapjournal.app.ui.history.HistoryViewModel
import com.soapjournal.app.ui.home.HomeScreen
import com.soapjournal.app.ui.home.HomeViewModel
import com.soapjournal.app.ui.memory.MemoryScreen
import com.soapjournal.app.ui.plan.ReadingPlanScreen
import com.soapjournal.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EDITOR = "editor/{entryId}"
    const val BIBLE = "bible"
    const val PLAN = "plan"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"

    fun editor(entryId: Long) = "editor/$entryId"
}

@Composable
fun SoapNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
            HomeScreen(
                viewModel = vm,
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenBible = { navController.navigate(Routes.BIBLE) },
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                onOpenMemory = { navController.navigate(Routes.MEMORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(container.repository)
            )
            HistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            val factory = remember(entryId) {
                EntryEditorViewModel.Factory(container.repository, entryId)
            }
            val vm: EntryEditorViewModel = viewModel(factory = factory)
            EntryEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BIBLE) {
            BibleScreen(
                container = container,
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
                onOpenEntry = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(Routes.MEMORY) {
            MemoryScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
