package com.edu.unal.lugarescercanos.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MapStyleTest {
    @Test
    fun mapStyleHidesPointsOfInterest() {
        val json = File("src/main/res/raw/map_style.json").readText().replace("\\s".toRegex(), "")

        assertEquals("[{\"featureType\":\"poi\",\"stylers\":[{\"visibility\":\"off\"}]}]", json)
    }
}
