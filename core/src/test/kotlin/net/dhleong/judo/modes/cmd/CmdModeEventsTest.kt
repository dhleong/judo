package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeEventsTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `event() with function0`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |def handleEvent(): print("awesome")
                |event('cool', handleEvent)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                event('cool', function handleEvent() {
                    print("awesome");
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", "cold")
        assert(judo.prints)
            .containsExactly("awesome")
    }

    @Test fun `event() with single arg`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                @event("cool")
                def handleEvent(ev):
                    print("awesome %s" % ev)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                event('cool', function(ev) {
                    print("awesome " + ev);
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", "colder")
        assert(judo.prints)
            .containsExactly("awesome colder")
    }

    @Test fun `event() with single null argument`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                @event("cool")
                def handleEvent(ev):
                    print("awesome %s" % ev)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                event('cool', function(ev) {
                    print("awesome " + ev);
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        val nullName = when (scriptType()) {
            SupportedScriptTypes.PY -> "None"
            else -> "null"
        }
        judo.events.raise("cool")
        assert(judo.prints)
            .containsExactly("awesome $nullName")
    }

    @Test fun `event() with destructuring`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                @event("cool")
                def handleEvent(key, val):
                    print("awesome %s:%s" % (key, val))
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                event('cool', function(key, val) {
                    print("awesome " + key + ":" + val);
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", arrayOf("cold", "colder"))
        assert(judo.prints)
            .containsExactly("awesome cold:colder")
    }

}