package net.dhleong.judo.script

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author dhleong
 */
class ScriptExecutionSandbox {

    private var threads = mutableSetOf<Thread>()
    private var executor = Executors.newCachedThreadPool { task ->
        Thread(task, "sandbox:${threads.size}").also {
            threads.add(it)
        }
    }
    private val running = AtomicInteger(0)

    fun interrupt() {
        if (running.getAndSet(0) == 0) {
            // fast path: nothing to interrupt
            return
        }

        executor.shutdownNow()
        val oldThread = threads
        threads = mutableSetOf()
        executor = Executors.newCachedThreadPool()

        // give the old threads ~1s to clean up gracefully...
        executor.execute {
            Thread.sleep(1000)

            // ... then kill them if they don't
            oldThread.forEach {
                if (it.isAlive) {
                    @Suppress("DEPRECATION")
                    it.stop()
                }
            }
        }
    }

    fun execute(block: () -> Unit) {
        running.incrementAndGet()
        try {
            executor.submit(block).get()
        } catch (e: ExecutionException) {
            if (e.cause !is ThreadDeath) {
                e.cause?.let { throw it }
                    ?: throw e
            }
        } finally {
            running.decrementAndGet()
        }
//        try {
//            val latch = CountDownLatch(1)
//            executor.execute {
//                try {
//                    block()
//                } catch (e: Throwable)
//                latch.countDown()
//            }
//            latch.await()
//        } finally {
//            running.decrementAndGet()
//        }
    }

}