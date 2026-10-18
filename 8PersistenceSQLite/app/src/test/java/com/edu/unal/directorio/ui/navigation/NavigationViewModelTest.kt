package com.edu.unal.directorio.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationViewModelTest {
    private val vm = NavigationViewModel()

    @Test
    fun startsOnList() {
        assertEquals(Screen.List, vm.screen.value)
    }

    @Test
    fun listToDetailToEditToDetailToList() {
        vm.openDetail(7)
        assertEquals(Screen.Detail(7), vm.screen.value)
        vm.openEdit(7)
        assertEquals(Screen.Form(7), vm.screen.value)
        vm.back()
        assertEquals(Screen.Detail(7), vm.screen.value)
        vm.back()
        assertEquals(Screen.List, vm.screen.value)
    }

    @Test
    fun createGoesBackToList() {
        vm.openCreate()
        assertEquals(Screen.Form(null), vm.screen.value)
        vm.back()
        assertEquals(Screen.List, vm.screen.value)
    }

    @Test
    fun savingCreateGoesToList() {
        vm.openCreate()
        vm.onSaved()
        assertEquals(Screen.List, vm.screen.value)
    }

    @Test
    fun savingEditGoesToThatDetail() {
        vm.openDetail(3)
        vm.openEdit(3)
        vm.onSaved()
        assertEquals(Screen.Detail(3), vm.screen.value)
    }

    @Test
    fun deletingGoesToList() {
        vm.openDetail(3)
        vm.onDeleted()
        assertEquals(Screen.List, vm.screen.value)
    }

    @Test
    fun backOnListStaysOnList() {
        vm.back()
        assertEquals(Screen.List, vm.screen.value)
    }
}
