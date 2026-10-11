package com.edu.unal.tictactoe

import android.content.Context
import android.view.View
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.SharedPreferencesNicknameStore

private const val KEYBOARD_TIMEOUT_MILLIS = 5_000L
private const val KEYBOARD_OPENING_MILLIS = 2_000L

/**
 * Stores [nickname] in the real nickname preferences, so that a real `MainActivity` shows the
 * lobby without asking for it. Pair it with [clearStoredNickname] in the test's cleanup.
 */
internal fun seedStoredNickname(context: Context, nickname: String = "Ana") {
    check(nicknamePreferences(context).edit().putString(SharedPreferencesNicknameStore.KEY_NICKNAME, nickname).commit()) {
        "Could not seed the nickname SharedPreferences"
    }
}

/** Removes any stored nickname, leaving the nickname preferences as on a fresh install. */
internal fun clearStoredNickname(context: Context) {
    check(nicknamePreferences(context).edit().clear().commit()) {
        "Could not clear the nickname SharedPreferences"
    }
}

private fun nicknamePreferences(context: Context) = context.applicationContext.getSharedPreferences(
    SharedPreferencesNicknameStore.FILE_NAME,
    Context.MODE_PRIVATE,
)

private fun softKeyboardVisible(host: View): Boolean {
    var visible = false
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        visible = ViewCompat.getRootWindowInsets(host)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }
    return visible
}

/**
 * On a phone the soft keyboard opens by itself with the nickname dialog, and then the first Back
 * only closes it. Waits briefly for it to open, because it appears after the dialog does, and
 * closes it with a real Back if it did. [host] is any view of the Activity window.
 */
internal fun ComposeTestRule.closeSoftKeyboardWithBack(host: View) {
    waitForIdle()
    val opened = try {
        waitUntil(timeoutMillis = KEYBOARD_OPENING_MILLIS) { softKeyboardVisible(host) }
        true
    } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
        false
    }
    if (!opened) return
    pressBackAsUser()
    waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) { !softKeyboardVisible(host) }
    waitForIdle()
}
