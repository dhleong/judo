package net.dhleong.judo.motions

import net.dhleong.judo.DummyConnectionFactory
import net.dhleong.judo.JudoCore
import net.dhleong.judo.StateMap
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.assertThat
import org.junit.After
import org.junit.Before

/**
 * @author dhleong
 */
abstract class AbstractMotionIntegrationTest {
    val renderer = TestableJudoRenderer()
    lateinit var judo: JudoCore

    @Before fun setUp() {
        judo = JudoCore(
            renderer, renderer.mapRenderer, StateMap(),
            connections = DummyConnectionFactory
        )
    }

    @After fun tearDown() {
        // if not empty, it contained errors
        assertThat(renderer.outputLines).isEmpty()
    }
}