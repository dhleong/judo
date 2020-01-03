package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.script.registerFrom

/**
 * @author dhleong
 */
fun ScriptInitContext.initCore() = with(mode) {
    registerFrom(CoreScripting(this@initCore))

    registerFn<Unit>(
        "echo",
        doc {
            usage { withVarArgs() }
            body { "Echo some transient text to the screen locally." }
        }
    ) { args: Array<Any> -> judo.echo(*args) }

    registerFn<Unit>(
        "print",
        doc {
            usage { withVarArgs() }
            body { "Print some output into the current buffer locally." }
        }
    ) { args: Array<Any> -> judo.print(*args) }

    registerFn(
        "redraw",
        doc {
            usage { }
            body { """
                    Force a redraw of the screen; clears any echo()'d output
                """.trimMargin() }
        },
        judo::redraw
    )

    registerFn(
        "quit",
        doc {
            usage { }
            body { "Exit Judo." }
        },
        judo::quit
    )

    registerFn<Unit>(
        "reload",
        doc {
            usage { }
            body { "Reload the last-loaded, non-MYJUDORC script file." }
        }
    ) { reload() }

    registerFn<Unit>(
        "send",
        doc {
            usage { arg("text", "String") }
            body { "Send some text to the connected server." }
        }
    ) { text: String -> judo.send(text, true) }
}

