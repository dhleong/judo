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
 * Scripting unfortunately needs to execute on its own thread instead of the
 * main thread. However, since the [net.dhleong.judo.script.ScriptingEngine]
 * has no knowledge of coroutines it cannot easily participate in the suspension,
 * and so we have cases where a script is calling into suspending code like
 * [net.dhleong.judo.IJudoCore.feedKeys] from the Scripting Dispatcher, but
 * can't suspend and needs to block the thread.
 *
 * In such cases, it is safe to access the [net.dhleong.judo.script.ScriptingEngine]
 * from a new thread, since the other thread "accessing" it is blocked, and
 * cannot run. It is important for this to be true, since suspending code
 * can easily call into the engine again, for example when feeding keys via
 * `normal()` into a lambda-based mapping.
 *
 * Single-threaded access to the engine is enforced using a [Mutex]. When we have
 * to exit the scripting context and run suspending code, we temporarily unlock
 * the [Mutex] so suspending code can then call back into the engine.
 *
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

    fun <R> withLockBlocking(block: suspend () -> R): R =
        runBlocking {
            lock.withLockReentrant(this@JudoScriptDispatcher) {
                block()
            }
        }

    /**
     * When running suspending code from within the scripting context,
     * we need to release the lock in case a script is triggering
     * code that triggers feeding keys (eg: `normal()` from a mapping)
     * so a new Script thread can execute if necessary
     */
    suspend fun <R> withoutLock(block: suspend () -> R): R {
        require(lock.holdsLock(this))
        lock.unlock()
        return try {
            block()
        } finally {
            // we *were* locked, so if we aren't yet locked again,
            // we need to regain the lock
            if (!lock.holdsLock(this)) {
                lock.lock(this)
            }
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
