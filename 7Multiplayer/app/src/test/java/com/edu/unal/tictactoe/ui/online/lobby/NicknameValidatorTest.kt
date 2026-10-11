package com.edu.unal.tictactoe.ui.online.lobby

import org.junit.Assert.assertEquals
import org.junit.Test

class NicknameValidatorTest {
    @Test
    fun emptyTextIsRejected() {
        assertEquals(NicknameValidation.Rejected, NicknameValidator.validate(""))
    }

    @Test
    fun whitespaceOnlyTextIsRejected() {
        assertEquals(NicknameValidation.Rejected, NicknameValidator.validate("     "))
    }

    @Test
    fun twentyOneCharactersAreRejected() {
        assertEquals(NicknameValidation.Rejected, NicknameValidator.validate("a".repeat(21)))
    }

    @Test
    fun surroundingSpacesAreTrimmed() {
        assertEquals(NicknameValidation.Valid("Ana"), NicknameValidator.validate("  Ana  "))
    }

    @Test
    fun exactlyTwentyCharactersAreAccepted() {
        val nickname = "a".repeat(20)

        assertEquals(NicknameValidation.Valid(nickname), NicknameValidator.validate(nickname))
    }

    @Test
    fun twentyOneCharactersThatTrimToTwentyAreAccepted() {
        val nickname = "b".repeat(20)

        assertEquals(NicknameValidation.Valid(nickname), NicknameValidator.validate(" $nickname"))
        assertEquals(NicknameValidation.Valid(nickname), NicknameValidator.validate("$nickname "))
    }

    @Test
    fun singleCharacterIsAccepted() {
        assertEquals(NicknameValidation.Valid("A"), NicknameValidator.validate("A"))
    }

    @Test
    fun innerSpacesAreKept() {
        assertEquals(NicknameValidation.Valid("Ana Maria"), NicknameValidator.validate(" Ana Maria "))
    }
}
