package com.edu.unal.tictactoe

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseInitializationTest {
    @Test
    fun defaultFirebaseAppIsInitializedWithRequiredOptions() {
        val app = FirebaseApp.getInstance()
        assertNotNull("FirebaseApp default instance", app)

        val options = app.options
        assertFalse("applicationId must not be blank", options.applicationId.isBlank())
        assertFalse("apiKey must not be blank", options.apiKey.isBlank())
        assertFalse("databaseUrl must not be blank", options.databaseUrl.isNullOrBlank())
    }
}
