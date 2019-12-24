package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.adaptSuspend
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initCore() = with(mode) {
    registerFn<Unit>(
        "config",
        doc {
            usage {
                arg("setting", "String")
                arg("value", "Any")
            }
            usage { arg("setting", "String") }
            usage { /* no-arg */ }

            body { "Set or get the value of a setting, or list all settings" }
        }
    ) { args: Array<Any> -> config(args) }

    registerFn(
        "complete",
        doc {
            usage { arg("text", "String") }
            body { """
                    Feed some text into the text completion system.
                    NOTE: This does not yet guarantee that the provided words will
                    be suggested in the sequence provided, but it may in the future.
                """.trimIndent() }
        },
        judo::seedCompletion
    )

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

    registerFn<String?>(
        "input",
        doc {
            usage { returns("String") }
            usage {
                arg("prompt", "String")
                returns("String")
            }
            body {
                """Request a string from the user, returning whatever they typed.
          |NOTE: Unlike the equivalent function in Vim, input() DOES NOT currently
          |consume pending input from mappings.
        """.trimMargin()
            }
        }
    ) { args: Array<Any> -> adaptSuspend {
        if (args.isNotEmpty()) {
            readInput(args[0] as String)
        } else {
            readInput("")
        }
    } }

    registerFn<Unit>(
        "normal",
        doc {
            usage { arg("keys", "String") }
            usage {
                arg("keys", "String")
                arg("remap", "Boolean")
            }
            body { """
                    Process [keys] as though they were typed by the user in normal mode.
                    To perform this operation with remaps disabled (as in nnoremap), pass
                    False for the second parameter.
                """.trimMargin() }
        }
    ) { args: Array<Any> -> adaptSuspend { feedKeys(args, mode = "normal") } }

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

