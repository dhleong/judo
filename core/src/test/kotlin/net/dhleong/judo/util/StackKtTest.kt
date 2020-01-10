package net.dhleong.judo.util

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.doesNotContain
import assertk.assertions.isFailure
import net.dhleong.judo.script.JudoScriptingEntity
import net.dhleong.judo.script.JythonScriptingEngine
import net.dhleong.judo.script.doc
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class StackKtTest {

    lateinit var jython: JythonScriptingEngine

    @Before fun setUp() {
        jython = JythonScriptingEngine().apply {
            register(JudoScriptingEntity.Function<String>("takeoff", doc {
                usage { arg("maneuver", "String") }
                body { "Leave port" }
            }, forceDispatchAsMultiArity = false) { maneuver: String ->
                maneuver
            })
        }
    }

    @Test fun `Clean up stack from Jython`() {
        assertThat {
            jython.execute("takeoff(42)")
        }.isFailure().transform { e ->
            e.formatStackTestable()
        }.all {
            doesNotContain(
                "java.lang.IllegalArgumentException: java.lang.IllegalArgumentException: Incorrect arguments to takeoff()"
            )

            containsAll(
                "java.lang.IllegalArgumentException: Incorrect arguments to takeoff()",
                "at net.dhleong.judo.script.Fn1.invoke(functional.kt)",
                "... more",
                "Caused by: java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String",
                "at net.dhleong.judo.util.StackKtTest\$setUp$1$2.invoke(StackKtTest.kt)",
                "at net.dhleong.judo.script.Fn1.invoke(functional.kt)",
                "... functional internals ...",
                "... python internals ..."
            )

            contains("... more")
        }
    }
}

private fun Throwable.formatStackTestable() = formatStack()
    .map {  l ->
        l.toString()
            .replace(Regex("(?<=\\.kt)(:\\d+)"), "")
            .trim()
    }
    .toList()
    .also { for (l in it) println(l) }
