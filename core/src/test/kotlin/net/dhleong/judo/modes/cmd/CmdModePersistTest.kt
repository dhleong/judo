package net.dhleong.judo.modes.cmd

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.hasSize
import net.dhleong.judo.matchesLinesExactly
import net.dhleong.judo.render.DiskBackedList
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModePersistTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `Persist output`() = runBlocking {
        val file = File(".CmdModePersistTest.persist.judo")
        file.delete()
        file.deleteOnExit()

        mode.execute("persistOutput('${file.absolutePath}')")
        val buffer = judo.primaryWindow.currentBuffer
        assertThat(buffer).hasSize(0)

        buffer.append("Persisted".toFlavorable())
        buffer.setNotPersistent()

        assertThat(DiskBackedList(file)).all {
            hasSize(1)
            transform("nth(0)") { it[0].toString().trim() }
                .isEqualTo("Persisted")
        }
    }

    @Test fun `Restore output`() = runBlocking {
        val file = File(".CmdModePersistTest.restore.judo")
        file.writeText("""
            Take my love
            Take my land
        """.trimIndent())
        file.deleteOnExit()

        mode.execute("persistOutput('${file.absolutePath}')")
        val buffer = judo.primaryWindow.currentBuffer
        assertThat(buffer).matchesLinesExactly(
            "Take my love\n",
            "Take my land\n",
            Regex("""^\^\^\^ Loaded 2 lines at .*\n"""),
            "\n" // blank line
        )
    }

}
