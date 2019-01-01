package net.dhleong.judo.jline

import net.dhleong.judo.*
import net.dhleong.judo.render.IJudoTabpage2
import org.jline.terminal.Size
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.Display
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author dhleong
 */
class JLineRenderer2(
    override var settings: StateMap
) : JudoRenderer2 {

    private val terminal = TerminalBuilder.terminal()!!
    private val window = Display(terminal, true)
    private val windowSize = Size(0, 0)

    override val terminalType: String
        get() = terminal.type

    override val capabilities: EnumSet<JudoRendererInfo.Capabilities>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val windowWidth: Int
        get() = terminal.width
    override val windowHeight: Int
        get() = terminal.height
    override var onResized: OnResizedEvent?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var currentTabpage: IJudoTabpage2?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun validate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setCursorType(type: CursorType) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateInputLine(line: String, cursor: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun redraw() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setLoading(isLoading: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun render() {
        val display = JLineDisplay(windowWidth, windowHeight) // reuse?
        (currentTabpage as? JLineTabpage)?.render(display)

        // TODO
        val cursor = windowSize.cursorPos(0, 0)
        window.update(display.toAttributedStrings(), cursor)
    }
}