package com.example.sunote1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sunote1.data.AppDatabase
import com.example.sunote1.data.NoteRepository
import com.example.sunote1.ui.NoteViewModel
import com.example.sunote1.ui.SuNoteApp

class MainActivity : ComponentActivity() {

    // 반드시 androidx.activity.viewModels 를 임포트해야 합니다.
    private val viewModel: NoteViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(applicationContext)
                val repo = NoteRepository(db.noteDao())
                return NoteViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuNoteApp(viewModel = viewModel)
        }
    }
}