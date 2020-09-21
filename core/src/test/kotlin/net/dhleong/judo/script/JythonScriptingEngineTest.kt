package net.dhleong.judo.script

import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

/**
 * @author dhleong
 */
class JythonScriptingEngineTest {
    @Test fun test() {
        val duration = measureTimeMillis {
            val engine = JythonScriptingEngine()

            engine.execute("print('hi')")
        }
        println("${duration}ms")
    }
}