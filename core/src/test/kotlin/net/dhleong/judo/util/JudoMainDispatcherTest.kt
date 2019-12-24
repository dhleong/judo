package net.dhleong.judo.util

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JudoMainDispatcherTest {
    lateinit var dispatcher: JudoMainDispatcher

    @Before fun setUp() {
        dispatcher = JudoMainDispatcher()
    }

    @After fun tearDown() {
        dispatcher.close()
    }

    @Test(timeout = 1000) fun `runBlocking nested`() {
        runBlocking(dispatcher) {
            inner()
        }
    }

    private fun inner() {
        runBlocking(dispatcher) {
            // inner nop
        }
    }
}