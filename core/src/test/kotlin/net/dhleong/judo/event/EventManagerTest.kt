package net.dhleong.judo.event

import assertk.all
import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.messageContains
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * @author dhleong
 */
class EventManagerTest {
    @Test fun enforceEventThread() {
        val events = EventManager()

        runBlocking {
            events.raise("event", "arg") // no problem
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