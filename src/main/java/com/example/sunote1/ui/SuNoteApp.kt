package com.example.sunote1.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sunote1.ui.screens.EditNoteScreen
import com.example.sunote1.ui.screens.NoteListScreen

@Composable
fun SuNoteApp(viewModel: NoteViewModel) {
    val nav = rememberNavController()
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = nav, startDestination = "list") {
                composable("list") {
                    NoteListScreen(
                        viewModel = viewModel,
                        onAdd = { nav.navigate("edit/0") },
                        onOpen = { id -> nav.navigate("edit/$id") }
                    )
                }
                composable("edit/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    EditNoteScreen(
                        viewModel = viewModel,
                        noteId = id,
                        onClose = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
