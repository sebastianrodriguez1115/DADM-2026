package com.edu.unal.tictactoe

import com.edu.unal.tictactoe.data.online.NicknameStore

/** In-memory test double for the nickname storage boundary. */
class FakeNicknameStore(private var nickname: String? = null) : NicknameStore {
    override fun readNickname(): String? = nickname

    override fun writeNickname(nickname: String) {
        this.nickname = nickname
    }
}
