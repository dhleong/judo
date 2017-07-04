package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import org.assertj.core.api.Assertions.assertThat
import org.jline.utils.AttributedString
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JudoTabpageTest {

    val ids = IdManager()

    lateinit var tabpage: JudoTabpage
    lateinit var primary: PrimaryJudoWindow

    @Before fun setUp() {
        val settings = StateMap()
        primary = PrimaryJudoWindow(ids, settings,
            JudoBuffer(ids),
            12,
            6
        )
        primary.isFocused = true
        tabpage = JudoTabpage(ids, settings, primary)
    }

    @Test fun hsplit() {
        primary.appendLine("Take my land", isPartialLine = false)
        primary.updateStatusLine("[status]")
        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "",
                "",
                "",
                "Take my land",
                "[status]"
            )

        val buffer = JudoBuffer(ids)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love", isPartialLine = false)

        assertThat(window.height).isEqualTo(2)
        assertThat(primary.height).isEqualTo(3)

        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "Take my love",
                "------------",
                "",
                "Take my land",
                "[status]"
            )
    }

    @Test fun unsplit() {
        primary.appendLine("Take my land", isPartialLine = false)
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(ids)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love", isPartialLine = false)

        assertThat(window.height).isEqualTo(2)
        assertThat(primary.height).isEqualTo(3)

        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "Take my love",
                "------------",
                "",
                "Take my land",
                "[status]"
            )

        tabpage.unsplit()
        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "",
                "",
                "",
                "Take my land",
                "[status]"
            )

        // do it again
        tabpage.unsplit()
        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "",
                "",
                "",
                "Take my land",
                "[status]"
            )
    }

    @Test fun resplit() {
        primary.appendLine("Take my land", isPartialLine = false)
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(ids)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love", isPartialLine = false)

        assertThat(window.height).isEqualTo(2)
        assertThat(primary.height).isEqualTo(3)

        // make sure we're good and unsplit
        tabpage.unsplit()
        tabpage.unsplit() // this should be idempotent

        // now, re-split without erroring
        tabpage.hsplit(2, buffer)
        assertThat(tabpage.getDisplayStrings())
            .containsExactly(
                "",
                "Take my love",
                "------------",
                "",
                "Take my land",
                "[status]"
            )

    }
}

fun IJudoTabpage.getDisplayStrings(): List<String> =
    with(mutableListOf<CharSequence>()) {
        getDisplayLines(this)
        map { (it as AttributedString).toAnsi() }
    }
