package com.edu.unal.tictactoe.data.online

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * The only production wiring point of the online service and the real nickname store.
 *
 * [gameService] is the Firebase implementation over the default [FirebaseAuth] and
 * [FirebaseDatabase] instances, created the first time it is used.
 */
object OnlineServices {
    val gameService: OnlineGameService by lazy {
        FirebaseOnlineGameService(FirebaseAuth.getInstance(), FirebaseDatabase.getInstance())
    }

    fun nicknameStore(context: Context): NicknameStore = SharedPreferencesNicknameStore(context)
}
