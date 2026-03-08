package com.astute.notes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.astute.notes.ui.noteeditor.NoteEditorScreen
import com.astute.notes.ui.notelist.NoteListScreen
import com.astute.notes.ui.settings.SettingsScreen

object Routes {
    const val NOTE_LIST = "noteList"
    const val NOTE_EDITOR = "noteEditor?noteId={noteId}"
    const val SETTINGS = "settings"

    fun noteEditor(noteId: String? = null): String {
        return if (noteId != null) "noteEditor?noteId=$noteId" else "noteEditor"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.NOTE_LIST) {

        composable(Routes.NOTE_LIST) {
            NoteListScreen(
                onCreateNote = { navController.navigate(Routes.noteEditor()) },
                onEditNote = { noteId -> navController.navigate(Routes.noteEditor(noteId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteEditorScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
