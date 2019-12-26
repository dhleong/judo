package net.dhleong.judo.util

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private const val UNSET_THREAD_ID = Long.MIN_VALUE

/**
 * @author dhleong
 */
open class SingleThreadDispatcher(
    private val threadName: String
) : ExecutorCoroutineDispatcher() {

    private var threadId: Long = UNSET_THREAD_ID

    private val myExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, threadName).apply {
            isDaemon = true
            threadId = id
        }
    }
    private val delegate: ExecutorCoroutineDispatcher = myExecutor.asCoroutineDispatcher()

    override val executor: ExecutorService = myExecutor

    override fun close() {
        delegate.close()
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        Thread.currentThread().id != threadId

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, block)
    }

    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        delegate.dispatchYield(context, block)
    }

}