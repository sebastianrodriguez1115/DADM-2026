package com.edu.unal.lugarescercanos.ui.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.edu.unal.lugarescercanos.location.locationPermissionStateOf

/** Permisos que pide la pantalla del mapa (`current-location`). */
internal val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Lanzador de los permisos de ubicación. Al abrir el mapa pide los permisos si todavía no se
 * consultaron; al volver de Ajustes con permiso concedido actualiza el estado.
 */
@Composable
internal fun rememberLocationPermissionLauncher(
    viewModel: MapViewModel,
): ActivityResultLauncher<Array<String>> {
    val context = LocalContext.current
    val activity = context.findActivity()

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { results ->
        val rationale = activity != null &&
            LOCATION_PERMISSIONS.any { activity.shouldShowRequestPermissionRationale(it) }
        viewModel.onPermissionResult(
            locationPermissionStateOf(
                fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true,
                coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
                alreadyRequested = true,
                shouldShowRationale = rationale,
            ),
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.state.value.permission == null) {
            if (context.hasAnyLocationPermission()) {
                viewModel.onPermissionResult(context.currentPermissionState())
            } else {
                launcher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    DisposableEffect(context) {
        val lifecycle = (context.findActivity() as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && context.hasAnyLocationPermission()) {
                val current = context.currentPermissionState()
                if (current != viewModel.state.value.permission) viewModel.onPermissionResult(current)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    return launcher
}

/** Abre los ajustes de la app en el sistema (`package:<id>`). */
internal fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}

private fun Context.hasAnyLocationPermission(): Boolean =
    isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
        isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Estado según los permisos ya concedidos, sin pedirlos. */
private fun Context.currentPermissionState() = locationPermissionStateOf(
    fineGranted = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION),
    coarseGranted = isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION),
    alreadyRequested = false,
    shouldShowRationale = false,
)

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
