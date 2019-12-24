package net.dhleong.judo.util

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
class JudoMainDispatcher(
    override val executor: ExecutorService =
        Executors.newSingleThreadExecutor { Thread("judo:main") },
    private val delegate: ExecutorCoroutineDispatcher = executor.asCoroutineDispatcher()
) : ExecutorCoroutineDispatcher() {

    override fun close() {
        delegate.close()
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        context[ContinuationInterceptor] !== this
            && super.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, block)
    }

    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        delegate.dispatchYield(context, block)
    }

}