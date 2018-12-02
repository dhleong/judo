package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
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

    @Test fun `event() with function0`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |def handleEvent(): echo("awesome")
                |event('cool', handleEvent)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                event('cool', function handleEvent() {
                    echo("awesome");
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", "cold")
        assert(judo.echos)
            .containsExactly("awesome")
    }

    @Test fun `event() with single arg`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                @event("cool")
                def handleEvent(ev):
                    echo("awesome %s" % ev)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                event('cool', function(ev) {
                    echo("awesome " + ev);
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", "colder")
        assert(judo.echos)
            .containsExactly("awesome colder")
    }

    @Test fun `event() with destructuring`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                @event("cool")
                def handleEvent(key, val):
                    echo("awesome %s:%s" % (key, val))
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                event('cool', function(key, val) {
                    echo("awesome " + key + ":" + val);
                });
            """.trimIndent()
        })

        assert(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", arrayOf("cold", "colder"))
        assert(judo.echos)
            .containsExactly("awesome cold:colder")
    }

}