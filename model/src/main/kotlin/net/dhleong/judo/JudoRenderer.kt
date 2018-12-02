package net.dhleong.judo

import net.dhleong.judo.render.IJudoTabpage
import java.io.Closeable
import java.util.EnumSet

typealias OnResizedEvent = () -> Unit

enum class CursorType(ansiCode: Int) {

    BLOCK_BLINK(1),
    BLOCK(2),
    UNDERSCORE_BLINK(3),
    UNDERSCORE(4),
    PIPE_BLINK(5),
    PIPE(6);

    val ansiString: String = StringBuilder(5)
        .append(27.toChar())
        .append('[')
        .append(ansiCode)
        .append(" q")
        .toString()
}

interface JudoRendererInfo {
    enum class Capabilities {
        UTF8,
        VT100,
        COLOR_256
    }

    /**
     * The type of terminal this is rendering to
     * (or is emulating), eg VT100
     */
    val terminalType: String

    val capabilities: EnumSet<Capabilities>

    val windowWidth: Int
}

/**
 * @author dhleong
 */
interface JudoRenderer : JudoRendererInfo, Closeable {

    val windowHeight: Int

    /**
     * Fired if the [windowWidth] or [windowHeight]
     *  values changed; run inside a transaction
     */
    var onResized: OnResizedEvent?

    var settings: StateMap

    var currentTabpage: IJudoTabpage?

    fun inTransaction(block: () -> Unit)

    /**
     * Make sure this renderer can be used
     */
    fun validate()

    /**
     * Attempt to set the current cursor type
     */
    fun setCursorType(type: CursorType)

    fun updateInputLine(line: String, cursor: Int)

    fun redraw()
    fun setLoading(isLoading: Boolean)
}