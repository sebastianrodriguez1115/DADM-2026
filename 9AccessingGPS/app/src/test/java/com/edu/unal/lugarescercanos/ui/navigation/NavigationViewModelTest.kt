package com.edu.unal.lugarescercanos.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationViewModelTest {
    private val vm = NavigationViewModel()

    @Test
    fun startsOnMap() {
        assertEquals(Screen.Map, vm.screen.value)
    }

    @Test
    fun settingsAndBackReturnToMap() {
        vm.openSettings()
        assertEquals(Screen.Settings, vm.screen.value)
        vm.back()
        assertEquals(Screen.Map, vm.screen.value)
    }

    @Test
    fun backOnMapStaysOnMap() {
        vm.back()
        assertEquals(Screen.Map, vm.screen.value)
    }
}
