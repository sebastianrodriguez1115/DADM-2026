package com.edu.unal.directorio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.edu.unal.directorio.ui.navigation.AppNavigation
import com.edu.unal.directorio.ui.theme.DirectorioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DirectorioTheme {
                AppNavigation()
            }
        }
    }
}
