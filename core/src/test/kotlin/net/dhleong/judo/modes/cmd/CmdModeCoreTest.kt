package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.message
import net.dhleong.judo.modes.USER_CONFIG_FILE
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeCoreTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assert(judo.echos).containsExactly("test", 2)
    }

    @Test fun globals() {
        mode.execute("echo(MYJUDORC)")

        assert(judo.echos).containsExactly(USER_CONFIG_FILE.absolutePath)
    }

    @Test fun `Complain with unexpected number of args`() {
        // works:
        mode.execute(fnCall("send", "mreynolds"))
        assert(judo.sends).containsExactly("mreynolds")

        assert {
            mode.execute(fnCall("send"))
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }

        assert {
            mode.execute(fnCall("send", "niska", 2))
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }
    }


}