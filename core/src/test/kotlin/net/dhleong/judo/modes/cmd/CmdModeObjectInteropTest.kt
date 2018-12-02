package net.dhleong.judo.modes.cmd

import assertk.all
import assertk.assert
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotSameAs
import net.dhleong.judo.render.hasHeight
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeObjectInteropTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `hsplit() returns an object supporting resize()`() {
        mode.execute("""
            newWin = hsplit(20)
            echo(newWin.id)
            echo(newWin.buffer)
            newWin.buffer.append("test")
            newWin.resize(newWin.width, 4)
            """.trimIndent())

        assert(judo.echos).isNotEmpty()
        val window = judo.tabpage.findWindowById(judo.echos[0] as Int)!!

        assert(window).hasHeight(4)

        val primary = judo.tabpage.currentWindow
        assert(primary).all {
            isNotSameAs(window)
            hasHeight(judo.tabpage.height - 4 - 1) // -1 for the separator!
        }
    }

}
