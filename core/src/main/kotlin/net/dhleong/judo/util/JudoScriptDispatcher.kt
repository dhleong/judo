package net.dhleong.judo.util

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
class JudoScriptDispatcher : ExecutorCoroutineDispatcher() {

    private val lock = Mutex()

    private val nextThreadId = AtomicLong(0)
    private val myExecutor: ExecutorService = Executors.newCachedThreadPool { task ->
        Thread(task, "judo:cmd:${nextThreadId.getAndIncrement()}").apply {
            isDaemon = true
        }
    }
    private val delegate: ExecutorCoroutineDispatcher = myExecutor.asCoroutineDispatcher()

    override val executor: ExecutorService = myExecutor

    override fun close() {
        delegate.close()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, Runnable {
            withLockBlocking {
                block.run()
            }
        })
    }

    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        delegate.dispatchYield(context, block)
    }

    fun <R> withLockBlocking(block: () -> R): R =
        runBlocking {
            println("acquire lock...")
            lock.withLockReentrant(this@JudoScriptDispatcher) {
                println("acquired lock!")
                block()
            }
        }

    suspend fun <R> withoutLock(block: suspend () -> R): R {
        require(lock.holdsLock(this))
        lock.unlock()
        return try {
            block()
        } finally {
            lock.lock(this)
        }
    }

    fun <R> wrapWithLock(fn: () -> R): () -> R =
        { withLockBlocking { fn() } }
}

suspend inline fun <T> Mutex.withLockReentrant(owner: Any, action: () -> T): T {
    val needLock = !holdsLock(owner)
    if (needLock) {
        lock(owner)
    }
    try {
        return action()
    } finally {
        if (needLock) {
            unlock(owner)
        }
    }
}
