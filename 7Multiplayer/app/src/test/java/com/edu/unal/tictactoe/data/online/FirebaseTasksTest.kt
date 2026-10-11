package com.edu.unal.tictactoe.data.online

import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseTasksTest {
    @Test
    fun returnsTheResultOfASuccessfulTask() = runTest {
        val source = TaskCompletionSource<String>()
        val deferred = async(start = CoroutineStart.UNDISPATCHED) { source.task.awaitTask() }

        source.setResult("done")

        assertEquals("done", deferred.await())
    }

    @Test
    fun returnsTheResultOfAnAlreadyCompletedTask() = runTest {
        val source = TaskCompletionSource<Int>()
        source.setResult(7)

        assertEquals(7, source.task.awaitTask())
    }

    @Test
    fun propagatesTheFailureOfTheTask() = runTest {
        val source = TaskCompletionSource<String>()
        val deferred = async(start = CoroutineStart.UNDISPATCHED) { runCatching { source.task.awaitTask() } }

        source.setException(IllegalStateException("boom"))

        val thrown = deferred.await().exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
        assertEquals("boom", thrown?.message)
    }

    @Test
    fun cancellingTheCoroutineStopsWaitingAndIgnoresALateResult() = runTest {
        val source = TaskCompletionSource<String>()
        val deferred = async(start = CoroutineStart.UNDISPATCHED) { source.task.awaitTask() }

        deferred.cancelAndJoin()
        source.setResult("late")

        assertTrue(deferred.isCancelled)
    }
}
