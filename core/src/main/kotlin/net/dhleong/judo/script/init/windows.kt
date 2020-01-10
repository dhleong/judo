package net.dhleong.judo.script.init

import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.IScriptWindow
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject

/**
 * @author dhleong
 */
fun ScriptInitContext.initWindows() = sequenceOf(
    WindowsScripting(this)
)

@Suppress("unused", "MemberVisibilityCanBePrivate")
class WindowsScripting(
    private val context: ScriptInitContext
) : ScriptingObject {
    override val docVariables: Map<String, String>? = mapOf(
        "splitHelp" to """
        The resulting Window object has the following attributes and methods:
            id - The window's unique, numeric ID
            buffer - The window's underlying Buffer object
            height - The height of the window in rows
            width - The width of the window in columns
            hidden - Whether or not to hide this Window
            close() - Close the window
            resize(width, height) - Resize the window

        A Buffer object has the following attributes and methods:
            id - The buffer's unique, numeric ID
            append(line: String) - Append a line to the buffer
            clear() - Remove all lines from the buffer
            deleteLast() - Remove the last (most-recently added) line 
                           in the buffer
            get(index: Int[, flags: String]) - Get a line from the buffer
                           by 0-based index. If `flags` includes "color"
                           the returned String will include ANSI color codes
            set(index: Int, line: String) - Replace the contents of a line
                           in the buffer by 0-based index
            set(lines: String[]) - Replace the buffer's entire contents
                                   with the given lines list
                                   
        Buffer also supports len() to get the number of lines
        """.trimIndent()
    )

    @Doc("""
        Create a new window by splitting the active window. The new window
        will be `rows` tall, or `perc` % of the active window's height.
        
        @{splitHelp}
    """)
    fun hsplit(rows: Int) = dispatchSplit { newBuffer -> hsplit(rows, newBuffer) }
    fun hsplit(perc: Float) = dispatchSplit { newBuffer -> hsplit(perc, newBuffer) }
    fun hsplit(perc: Double) = hsplit(perc.toFloat())

    @Doc("""
        Create a new window by splitting the active window. The new window
        will be `cols` wide, or `perc` % of the active window's width.
        
        @{splitHelp}
    """)
    fun vsplit(cols: Int) = dispatchSplit { newBuffer -> vsplit(cols, newBuffer) }
    fun vsplit(perc: Float) = dispatchSplit { newBuffer -> vsplit(perc, newBuffer) }
    fun vsplit(perc: Double) = vsplit(perc.toFloat())

    @Doc("""
        Remove any split windows
    """)
    fun unsplit() = with(context) {
        judo.tabpage.unsplit()
    }

    private inline fun dispatchSplit(
        split: IJudoTabpage.(buffer: IJudoBuffer) -> IJudoWindow
    ): IScriptWindow = with(context) {
        val newBuffer = judo.renderer.createBuffer()
        engine.wrapWindow(
            judo.tabpage,
            judo.tabpage.split(newBuffer)
        )
    }

}

