package net.dhleong.judo.script.init

import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initWindows() {
    val splitHelp = """
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

    registerFn<Any>(
        "hsplit",
        doc {
            usage {
                arg("rows", "Int")
                returns("Window")
            }
            usage {
                arg("perc", "Float")
                returns("Window")
            }

            body { """
                    Create a new window by splitting the active window. The new window
                    will be `rows` tall, or `perc` % of the active window's height.
                """.trimIndent() + "\n" + splitHelp }
        }
    ) { arg: Any -> dispatchSplit { newBuffer ->
        when (arg) {
            is Int -> judo.tabpage.hsplit(arg, newBuffer)
            is Float -> judo.tabpage.hsplit(arg, newBuffer)
            is Double -> judo.tabpage.hsplit(arg.toFloat(), newBuffer)
            else -> throw IllegalArgumentException()
        }
    } }

    registerFn<Any>(
        "vsplit",
        doc {
            usage {
                arg("cols", "Int")
                returns("Window")
            }
            usage {
                arg("perc", "Float")
                returns("Window")
            }

            body { """
                    Create a new window by splitting the active window. The new window
                    will be `cols` wide, or `perc` % of the active window's width.
                """.trimIndent() + "\n" + splitHelp }
        }
    ) { arg: Any -> dispatchSplit { newBuffer ->
        when (arg) {
            is Int -> judo.tabpage.vsplit(arg, newBuffer)
            is Float -> judo.tabpage.vsplit(arg, newBuffer)
            is Double -> judo.tabpage.vsplit(arg.toFloat(), newBuffer)
            else -> throw IllegalArgumentException()
        }
    } }

    registerFn<Unit>(
        "unsplit",
        doc {
            usage {  }
            body { "Remove any split windows" }
        }
    ) { judo.tabpage.unsplit() }
}

private inline fun ScriptInitContext.dispatchSplit(
    split: IJudoTabpage.(buffer: IJudoBuffer) -> IJudoWindow
): Any {
    val newBuffer = judo.renderer.createBuffer()
    return engine.wrapWindow(
        judo.tabpage,
        judo.tabpage.split(newBuffer)
    )
}

