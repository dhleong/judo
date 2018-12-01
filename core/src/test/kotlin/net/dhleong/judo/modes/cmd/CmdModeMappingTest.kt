package net.dhleong.judo.modes.cmd

import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeMappingTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun deleteMap() {
        mode.execute(fnCall("deleteMap", "gf", "to"))
    }

}
