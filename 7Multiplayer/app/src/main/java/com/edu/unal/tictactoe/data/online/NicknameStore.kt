package com.edu.unal.tictactoe.data.online

/** Durable storage for the player's online nickname. */
interface NicknameStore {
    /** Returns the stored nickname, or null when none has been saved. */
    fun readNickname(): String?

    fun writeNickname(nickname: String)
}
