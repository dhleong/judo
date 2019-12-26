package net.dhleong.judo.event

import assertk.all
import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.messageContains
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.util.JudoMainDispatcher
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * @author dhleong
 */
class EventManagerTest {
    @Test fun enforceEventThread() {
        val dispatcher = JudoMainDispatcher()
        val events = EventManager()

        runBlocking(dispatcher) {
            events.raise("event", "arg") // no problem
        }

        runBlocking(Dispatchers.IO) {
            assertThat {
                events.raise("wrongThread", "arg")
            }.isFailure().all {
                messageContains("wrongThread")
                messageContains("non-event thread")
            }

            @Suppress("EXPERIMENTAL_API_USAGE")
            async(dispatcher, start = CoroutineStart.UNDISPATCHED) {
                assertThat {
                    events.raise("wrongThread", "arg")
                }.isFailure().all {
                    messageContains("wrongThread")
                    messageContains("non-event thread")
                }
            }.await()
        }

        val latch = CountDownLatch(1)
        var failed = true
        thread {
            try {
                assertThat {
                    runBlocking {
                        events.raise("wrongThread", "arg")
                    }
                }.isFailure().all {
                    messageContains("wrongThread")
                    messageContains("non-event thread")

                    failed = false
                }
            } finally {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(failed, "Failed to catch wrong thread usage").isFalse()
    }
}