package net.dhleong.judo.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
class JudoMainDispatcher(
    awaitEnabled: Boolean = false
) : SingleThreadDispatcher("judo:main") {

    private val dispatch = whenTrue(awaitEnabled) {
        Channel<Any>(1)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        super.isDispatchNeeded(context).also { isNeeded ->
            if (!isNeeded) {
                dispatch?.offer(Unit)
            }
        }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        super.dispatch(context, block)

        dispatch?.offer(Unit)
    }

    /**
     * Wait for a [dispatch] to occur; this does not work unless this dispatcher was
     * created with `awaitEnabled` set to True, and is entirely for testing
     */
    @VisibleForTesting
    suspend fun awaitDispatch() {
        val ch = dispatch ?: throw UnsupportedOperationException("awaitDispatch not enabled")

        withTimeoutOrNull(500) {
            ch.receive()

            // NOTE: this *might* be received from isDispatchNeeded, in which case
            // the code in question will probably run *after* we receive; we delay
            // here to give it some time to do that first
            delay(1)
        }

        awaitIdle()
    }
}