package net.dhleong.judo

import net.dhleong.judo.render.IJudoTabpage2
import java.io.Closeable

/**
 * @author dhleong
 */
interface JudoRenderer2 : JudoRendererInfo, Closeable {

    val windowHeight: Int

    /**
     * Fired if the [windowWidth] or [windowHeight]
     *  values changed; run inside a transaction
     */
    var onResized: OnResizedEvent?

    var settings: StateMap

    var currentTabpage: IJudoTabpage2?

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

inline fun JudoRenderer2.inTransaction(block: () -> Unit) {
    try {
        beginUpdate()

        block()

    } finally {
        finishUpdate()
    }
}
