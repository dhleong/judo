package net.dhleong.judo

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import java.io.Closeable
import java.util.EnumSet

typealias OnRendererEventListener = (event: JudoRendererEvent) -> Unit

@Suppress("unused")
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
interface JudoRenderer : JudoRendererInfo, WindowCommandHandler, Closeable {

    val windowHeight: Int

    var onEvent: OnRendererEventListener?

    var settings: StateMap

    var currentTabpage: IJudoTabpage

    /**
     * The Renderer is responsible for allocating Buffers,
     * since writing to a Buffer may create work for the
     * Renderer in order to present the new lines
     */
    fun createBuffer(): IJudoBuffer

    /**
     * Make sure this renderer can be used
     */
    fun validate()

    /**
     * Attempt to set the current cursor type
     */
    fun setCursorType(type: CursorType)

    /**
     * Write text (possibly multiple lines) to the "echo" buffer.
     * In a transaction, multiple echoes may be combined into a
     * single write, but in general subsequent [echo] calls will
     * replace earlier calls.
     *
     * A blocking echo may be cleared with [clearEcho]
     */
    fun echo(text: FlavorableCharSequence)
    fun clearEcho()

    fun updateInputLine(line: FlavorableCharSequence, cursor: Int)

    fun redraw()
    fun setLoading(isLoading: Boolean)

    /**
     * Begin a series of updates that should eventually
     * result in a [redraw]
     */
    fun beginUpdate()

    /**
     * Finish an update initiated by [beginUpdate]
     */
    fun finishUpdate()
}

inline fun <R> JudoRenderer.inTransaction(
    block: () -> R
): R = try {
    beginUpdate()

    block()

} finally {
    finishUpdate()
}

