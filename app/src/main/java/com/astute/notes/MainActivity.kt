package com.astute.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.astute.notes.ui.navigation.AppNavigation
import com.astute.notes.ui.theme.AstuteNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AstuteNotesTheme {
                AppNavigation()
            }
        }
    }
}
