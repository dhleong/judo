package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initModes() {
    registerFn(
        "createUserMode",
        doc {
            usage { arg("modeName", "String") }
            body { """
                    Create a new mode with the given name. Mappings can be added to it
                    using the createMap function
                """.trimIndent() }
        },
        judo::createUserMode
    )

    registerFn<Unit>(
        "enterMode",
        doc {
            usage { arg("modeName", "String") }
            body { "Enter the mode with the given name." }
        }
    ) { modeName: String -> judo.enterMode(modeName) }

    registerFn<Unit>(
        "exitMode",
        doc {
            usage { }
            body { "Exit the current mode." }
        }
    ) { modeName: String -> judo.enterMode(modeName) }

    registerFn<Unit>(
        "startInsert",
        doc {
            usage {  }
            body { "Enter insert mode as if by pressing `i`" }
        }
    ) { judo.enterMode("insert") }

    registerFn<Unit>(
        "stopInsert",
        doc {
            usage {  }
            body { "Exit insert mode as soon as possible." }
        }
    ) { judo.exitMode() }
}

