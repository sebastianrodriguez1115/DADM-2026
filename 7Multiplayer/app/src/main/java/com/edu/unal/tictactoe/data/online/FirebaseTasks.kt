package com.edu.unal.tictactoe.data.online

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until this Firebase [Task] completes: returns its result, rethrows its failure, and
 * stops waiting when the calling coroutine is cancelled (the task itself cannot be cancelled and
 * keeps running). A task that was cancelled by its owner fails with [IllegalStateException]
 * instead of cancelling the caller.
 */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener(Runnable::run) { task ->
        val failure = task.exception
        when {
            failure != null -> continuation.resumeWithException(failure)
            task.isCanceled -> continuation.resumeWithException(IllegalStateException("The task was canceled"))
            else -> continuation.resume(task.result)
        }
    }
}
