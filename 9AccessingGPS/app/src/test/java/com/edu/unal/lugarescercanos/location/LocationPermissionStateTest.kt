package com.edu.unal.lugarescercanos.location

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPermissionStateTest {
    @Test
    fun preciseWhenFineIsGranted() {
        val state = locationPermissionStateOf(
            fineGranted = true, coarseGranted = true, alreadyRequested = true, shouldShowRationale = false,
        )
        assertEquals(LocationPermissionState.Precise, state)
    }

    @Test
    fun approximateWhenOnlyCoarseIsGranted() {
        val state = locationPermissionStateOf(
            fineGranted = false, coarseGranted = true, alreadyRequested = true, shouldShowRationale = true,
        )
        assertEquals(LocationPermissionState.Approximate, state)
    }

    @Test
    fun deniedWhenSystemWouldShowRationale() {
        val state = locationPermissionStateOf(
            fineGranted = false, coarseGranted = false, alreadyRequested = true, shouldShowRationale = true,
        )
        assertEquals(LocationPermissionState.Denied, state)
    }

    @Test
    fun permanentlyDeniedWhenRequestedAndNoRationale() {
        val state = locationPermissionStateOf(
            fineGranted = false, coarseGranted = false, alreadyRequested = true, shouldShowRationale = false,
        )
        assertEquals(LocationPermissionState.PermanentlyDenied, state)
    }

    @Test
    fun notYetRequestedIsDeniedNotPermanent() {
        val state = locationPermissionStateOf(
            fineGranted = false, coarseGranted = false, alreadyRequested = false, shouldShowRationale = false,
        )
        assertEquals(LocationPermissionState.Denied, state)
    }
}
