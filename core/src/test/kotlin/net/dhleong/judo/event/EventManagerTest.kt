package net.dhleong.judo.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * @author dhleong
 */
class EventManagerTest {
    @Test fun enforceEventThread() {
        val events = EventManager()
        events.raise("event", "arg") // no problem

        val latch = CountDownLatch(1)
        var failed = true
        thread {
            try {
                assertThatThrownBy {
                    events.raise("wrongThread", "arg")
                }.hasMessageContaining("wrongThread")
                    .hasMessageContaining("non-event thread")

                failed = false
            } finally {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(failed)
            .overridingErrorMessage("Failed to catch wrong thread usage")
            .isFalse()
    }
}