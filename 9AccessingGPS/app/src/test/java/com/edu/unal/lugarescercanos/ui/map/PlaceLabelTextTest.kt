package com.edu.unal.lugarescercanos.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceLabelTextTest {
    private val name28 = "A".repeat(28)
    private val name40 = "Hospital Universitario Nacional de Colombia".take(40)

    @Test
    fun testNameHasFortyCharacters() {
        assertEquals(40, name40.length)
    }

    @Test
    fun twentyEightCharactersAreNotCut() {
        assertEquals(name28, labelText(name28))
    }

    @Test
    fun fortyCharactersAreCutAndEndWithEllipsis() {
        val text = labelText(name40)

        assertEquals(name40.take(LABEL_MAX_CHARS) + "…", text)
    }

    @Test
    fun aSurrogatePairIsNotSplit() {
        // 28 puntos de código, cada uno de dos caracteres UTF-16, más uno extra.
        val emoji = "😀"
        val name = emoji.repeat(29)

        assertEquals(emoji.repeat(28) + "…", labelText(name))
    }
}
