package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.all
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.exists
import assertk.assertions.hasToString
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.assertions.support.expected
import net.dhleong.judo.hasHeight
import net.dhleong.judo.hasId
import net.dhleong.judo.hasSize
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.render.hasFlavor
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
            print(newWin.id)
            print(newWin.buffer)
            print(newWin.height)
            newWin.buffer.append("test")
            newWin.resize(newWin.width, 4)
            """.trimIndent())

        assert(judo.prints).isNotEmpty()

        val splitHeight = judo.prints[2] as Int
        assert(splitHeight, "split height").isEqualTo(20)

        val window = judo.tabpage.findWindowById(judo.prints[0] as Int)!!
        assert(window).hasHeight(4)

        // NOTE: the tests below are no longer useful since core is no longer
        // bundled with a functioning renderer:
//        val primary = judo.tabpage.currentWindow
//        assert(primary).all {
//            isNotSameAs(window)
//
//            hasHeight(judo.tabpage.height - 4 - 1) // -1 for the separator!
//        }
    }

    @Test fun `Set current window by scripting`() {
        mode.execute("""
            primary = judo.current.window
            print(primary.id)
            newWin = hsplit(20)
            print(newWin.id)
        """.trimIndent())

        assert(judo.prints).isNotEmpty()
        val primaryId = judo.prints[0] as Int
        val newWinId = judo.prints[1] as Int
        assert(judo.renderer.currentTabpage.currentWindow).hasId(newWinId)

        mode.execute("""
            judo.current.window = primary
        """.trimIndent())

        assert(judo.renderer.currentTabpage.currentWindow).hasId(primaryId)
    }

    @Test fun `Prevent access to non-exposed methods`() {
        assert {
            // clearTestable is a public fn on TestableJudoCore, but not in IScriptJudo
            mode.execute("""
                judo.clearTestable()
            """.trimIndent())
        }.thrownError {
            message().isNotNull {
                it.contains("clearTestable")
            }
        }

        assert {
            // clearTestable is a public fn on IJudoCore, but not in IScriptJudo
            mode.execute("""
                judo.print("hi")
            """.trimIndent())
        }.thrownError {
            message().isNotNull {
                it.contains("print")
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
                print(judo.mapper.current is not None)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                print(judo.mapper.current !== null)
            """.trimIndent()
        })

        assert(judo.prints).containsExactly(true)
        file.delete()
    }

    @Test fun `Buffer append handles ANSI`() {

        val esc = "\\u001b"
        var string = """
            "$esc[33mANSI"
        """.trimIndent()

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (scriptType()) {
            SupportedScriptTypes.PY -> string = "u$string"
        }

        mode.execute("""
            judo.current.buffer.append($string)
        """.trimIndent())

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        assert(buffer).hasSize(1)

        assert(buffer[0]).all {
            hasToString("ANSI\n")
            hasFlavor(SimpleFlavor(
                hasForeground = true,
                foreground = JudoColor.Simple.from(3)
            ))
        }
    }

}

private fun Assert<File>.doesNotExist() {
    if (!actual.exists()) return
    expected("to not exist")
}
