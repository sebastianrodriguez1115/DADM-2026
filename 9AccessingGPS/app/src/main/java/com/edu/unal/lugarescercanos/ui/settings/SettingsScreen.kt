package com.edu.unal.lugarescercanos.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.lugarescercanos.R
import com.edu.unal.lugarescercanos.domain.RadiusRules
import kotlin.math.roundToInt

/** Pantalla de Ajustes del radio de búsqueda (P8). Sin estado propio: recibe el estado y los callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRadiusChange: (Double) -> Unit,
    onRadiusChangeFinished: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Entre 0,5 y 10 km en pasos de 0,5 km hay 19 intervalos y 18 puntos intermedios.
    val steps = ((RadiusRules.MAX_KM - RadiusRules.MIN_KM) / RadiusRules.STEP_KM).roundToInt() - 1

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_description),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_radius_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.radiusText,
                style = MaterialTheme.typography.headlineMedium,
            )
            Slider(
                value = state.radiusKm.toFloat(),
                onValueChange = { onRadiusChange(it.toDouble()) },
                onValueChangeFinished = onRadiusChangeFinished,
                valueRange = RadiusRules.MIN_KM.toFloat()..RadiusRules.MAX_KM.toFloat(),
                steps = steps,
            )
        }
    }
}
