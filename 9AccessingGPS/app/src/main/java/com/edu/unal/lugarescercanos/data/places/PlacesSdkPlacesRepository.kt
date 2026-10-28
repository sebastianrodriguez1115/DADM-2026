package com.edu.unal.lugarescercanos.data.places

import android.content.Context
import com.edu.unal.lugarescercanos.domain.NearbySearchParams
import com.edu.unal.lugarescercanos.domain.PlaceSearchRules
import com.edu.unal.lugarescercanos.domain.PlacesError
import com.edu.unal.lugarescercanos.domain.PlacesErrorMapper
import com.edu.unal.lugarescercanos.domain.RawPlaceCandidate
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place as SdkPlace
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Búsqueda Nearby con el Places SDK (New) (D5). Requiere que [Places] ya esté inicializado en
 * `LugaresApplication`; el cliente se crea en la primera búsqueda.
 */
class PlacesSdkPlacesRepository(context: Context) : PlacesRepository {
    private val appContext = context.applicationContext
    private val client: PlacesClient by lazy { Places.createClient(appContext) }

    override suspend fun searchNearby(params: NearbySearchParams): PlacesSearchResult {
        return try {
            val cancellation = CancellationTokenSource()
            val response = client
                .searchNearby(buildRequest(params, cancellation.token))
                .awaitCancellable(cancellation)
            val candidates = response.places.map { it.toCandidate() }
            PlacesSearchResult.Success(
                PlaceSearchRules.process(candidates, params.center, params.radiusMeters, params.maxResultCount),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            PlacesSearchResult.Failure(PlacesErrorMapper.map(e.statusCode))
        } catch (_: IOException) {
            PlacesSearchResult.Failure(PlacesError.NoConnection)
        } catch (_: Exception) {
            PlacesSearchResult.Failure(PlacesError.ServiceUnavailable)
        }
    }

    private fun buildRequest(
        params: NearbySearchParams,
        token: CancellationToken,
    ): SearchNearbyRequest {
        val bounds = CircularBounds.newInstance(
            LatLng(params.center.latitude, params.center.longitude),
            params.radiusMeters,
        )
        return SearchNearbyRequest.builder(bounds, FIELDS)
            .setIncludedTypes(params.includedTypes)
            .setMaxResultCount(params.maxResultCount)
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
            .setCancellationToken(token)
            .build()
    }

    private fun SdkPlace.toCandidate(): RawPlaceCandidate {
        val sdkLocation: LatLng? = location
        return RawPlaceCandidate(
            id = id.orEmpty(),
            name = displayName,
            latitude = sdkLocation?.latitude,
            longitude = sdkLocation?.longitude,
        )
    }

    private companion object {
        /** Solo los campos Pro (D5). Ningún campo Enterprise. */
        val FIELDS: List<SdkPlace.Field> = listOf(
            SdkPlace.Field.ID,
            SdkPlace.Field.DISPLAY_NAME,
            SdkPlace.Field.LOCATION,
        )
    }
}

/**
 * Espera el [Task] sin bloquear. Si la corrutina se cancela, cancela también el [cancellation]
 * de la petición (el SDK admite `CancellationToken`, así que no hace falta descartar el resultado).
 */
private suspend fun <T> Task<T>.awaitCancellable(cancellation: CancellationTokenSource): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            when {
                task.isCanceled -> continuation.cancel()
                task.isSuccessful -> continuation.resume(task.result)
                else -> continuation.resumeWithException(
                    task.exception ?: IllegalStateException("La tarea de Places falló sin excepción"),
                )
            }
        }
        continuation.invokeOnCancellation { cancellation.cancel() }
    }
