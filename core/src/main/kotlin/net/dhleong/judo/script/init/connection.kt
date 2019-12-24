package net.dhleong.judo.script.init

import net.dhleong.judo.net.createURI
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initConnection() {
    registerFn<Unit>(
        "connect",
        doc {
            usage {
                arg("uri", "String")
            }

            usage {
                arg("host", "String")
                arg("port", "Int")
            }

            body { "Connect to a server." }
        }
    ) { args: Array<Any> -> when (args.size) {
        1 -> judo.connect(createURI(args[0] as String))
        2 -> judo.connect(createURI("${args[0]}:${args[1]}"))
    } }

    registerFn(
        "disconnect",
        doc {
            usage { }
            body { "Disconnect from the server." }
        },
        judo::disconnect
    )

    registerFn(
        "isConnected",
        doc {
            usage { returns("Boolean") }
            body { "Check if connected." }
        },
        judo::isConnected
    )

    registerFn(
        "reconnect",
        doc {
            usage { }
            body { "Repeat the last connect()" }
        },
        judo::reconnect
    )
}
