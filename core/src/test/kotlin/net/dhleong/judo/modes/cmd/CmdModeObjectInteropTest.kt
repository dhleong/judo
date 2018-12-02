package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.all
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.exists
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.message
import assertk.assertions.support.expected
import net.dhleong.judo.render.hasHeight
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

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

    @Test fun `Prevent access to non-exposed methods`() {
        assert {
            // clearTestable is a public fn on TestableJudoCore, but not in IJudoCore
            mode.execute("""
                judo.clearTestable()
            """.trimIndent())
        }.thrownError {
            message().isNotNull {
                it.contains("clearTestable")
            }
        }
    }

    @Test fun `Mapper works`() {
        val file = File("test.map")
        if (file.exists()) file.delete()
        assert(file).doesNotExist()

        mode.execute("""
            judo.mapper.createEmpty()
            judo.mapper.saveAs("${file.absolutePath}")
            """.trimIndent())
        assert(file).exists()

        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                echo(judo.mapper.current is not None)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                echo(judo.mapper.current !== null)
            """.trimIndent()
        })

        assert(judo.echos).containsExactly(true)
        file.delete()
    }

}

private fun Assert<File>.doesNotExist() {
    if (!actual.exists()) return
    expected("to not exist")
}
