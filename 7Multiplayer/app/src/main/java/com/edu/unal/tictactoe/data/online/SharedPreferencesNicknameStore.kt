package com.edu.unal.tictactoe.data.online

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android persistence for the online nickname, in its own preferences file so it never mixes with
 * the local-game preferences. The application context is converted immediately to
 * [SharedPreferences]; no Activity or other Context is retained. Writes use the KTX
 * `edit` extension, which applies with `apply()` by default, like the game preferences: visible
 * in memory at once, flushed to disk asynchronously.
 */
class SharedPreferencesNicknameStore(context: Context) : NicknameStore {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE,
    )

    /** Returns null when the value is absent, blank or not a string. */
    override fun readNickname(): String? = try {
        preferences.getString(KEY_NICKNAME, null)?.takeIf { it.isNotBlank() }
    } catch (_: ClassCastException) {
        null
    }

    override fun writeNickname(nickname: String) {
        preferences.edit { putString(KEY_NICKNAME, nickname) }
    }

    companion object {
        const val FILE_NAME = "online_preferences"
        const val KEY_NICKNAME = "nickname"
    }
}
