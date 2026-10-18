package com.edu.unal.directorio.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Draws the current screen. The ViewModel is Activity-scoped.
 *
 * PROVISIONAL: the content of each screen is a placeholder that groups 5 to 7 replace.
 * Back is intercepted only on detail and form; on the list the system handles it.
 */
@Composable
fun AppNavigation(navigation: NavigationViewModel = viewModel()) {
    val screen by navigation.screen.collectAsState()

    when (screen) {
        Screen.List -> Unit
        is Screen.Detail, is Screen.Form -> BackHandler { navigation.back() }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            when (val current = screen) {
                Screen.List -> {
                    Text("PROVISIONAL list")
                    Button(onClick = { navigation.openDetail(1) }) { Text("PROVISIONAL detail") }
                    Button(onClick = { navigation.openCreate() }) { Text("PROVISIONAL create") }
                }
                is Screen.Detail -> {
                    Text("PROVISIONAL detail ${current.id}")
                    Button(onClick = { navigation.openEdit(current.id) }) { Text("PROVISIONAL edit") }
                    Button(onClick = { navigation.onDeleted() }) { Text("PROVISIONAL delete") }
                }
                is Screen.Form -> {
                    Text("PROVISIONAL form ${current.id ?: "new"}")
                    Button(onClick = { navigation.onSaved() }) { Text("PROVISIONAL save") }
                    Button(onClick = { navigation.back() }) { Text("PROVISIONAL cancel") }
                }
            }
        }
    }
}
