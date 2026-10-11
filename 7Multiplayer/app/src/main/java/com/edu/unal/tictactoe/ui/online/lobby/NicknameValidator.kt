package com.edu.unal.tictactoe.ui.online.lobby

/** Outcome of validating the text typed for the online nickname. */
sealed interface NicknameValidation {
    /** The text is acceptable; [nickname] is the trimmed value to store. */
    data class Valid(val nickname: String) : NicknameValidation

    /** The text is empty after trimming or longer than [NicknameValidator.MAX_LENGTH]. */
    data object Rejected : NicknameValidation
}

/** Pure nickname rule: trim surrounding whitespace, then require 1 to [MAX_LENGTH] characters. */
object NicknameValidator {
    const val MAX_LENGTH = 20

    fun validate(text: String): NicknameValidation {
        val trimmed = text.trim()
        return if (trimmed.length in 1..MAX_LENGTH) {
            NicknameValidation.Valid(trimmed)
        } else {
            NicknameValidation.Rejected
        }
    }
}
