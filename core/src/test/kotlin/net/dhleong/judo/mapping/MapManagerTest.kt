package net.dhleong.judo.mapping

import assertk.all
import assertk.assert
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.StateMap
import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.bufferOf
import net.dhleong.judo.doesNotHaveLine
import net.dhleong.judo.hasSize
import net.dhleong.judo.mapping.renderer.SimpleBufferMapRenderer
import net.dhleong.judo.render.toFlavorable
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class MapManagerTest {

    private lateinit var judo: IJudoCore
    private lateinit var mapper: IMapManager
    private lateinit var map: IJudoMap

    @Before fun setUp() {
        judo = TestableJudoCore(TestableJudoRenderer())
        mapper = MapManager(judo, StateMap(), SimpleBufferMapRenderer())

        mapper.createEmpty()
        map = mapper.current as JudoMap
        map.inRoom = 0
        map.add(JudoRoom(0, "0"))
    }

    @Test fun `Render to primary window by default`() {
        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        assert(buffer).hasSize(0)

        mapper.render()
        assert(buffer).hasSize(judo.renderer.windowHeight)
    }

    @Test fun `Don't clear the primary window`() {
        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        buffer.appendLine("Test".toFlavorable())
        assert(buffer).hasSize(1)

        mapper.render()
        assert(buffer).doesNotHaveLine("Test".toFlavorable())
    }

    @Test fun `Render to and control provided window`() {
        val primaryWindow = judo.renderer.currentTabpage.currentWindow
        val primary = primaryWindow.currentBuffer
        assert(primary).hasSize(0)

        val buffer = bufferOf("""
            You can't take
            The skies from me
        """.trimIndent())
        val window = judo.tabpage.hsplit(10, buffer)
        assert(buffer).hasSize(2)

        // swap back to primary window
        judo.renderer.currentTabpage.currentWindow = primaryWindow

        mapper.window = window
        mapper.render()
        assert(buffer).all {
            // non-primary window's buffer gets cleared
            hasSize(9)
            doesNotHaveLine("You can't take".toFlavorable())
        }

        // still nothing in the primary buffer
        assert(primary).hasSize(0)
    }
}

