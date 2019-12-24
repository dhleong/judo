package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initEvents() {
    registerFn<Unit>(
        "event",
        doc {
            usage(decorator = true) {
                arg("eventName", "String")
                arg("handler", "Fn")
            }

            body { """
                    Subscribe to an event with the provided handler.
                    Available events:

                    "Name": (args) Description
                    --------------------------
                    "CONNECTED"      ():            Connected to the server
                    "DISCONNECTED"   ():            Disconnected to the server
                    "GMCP ENABLED    ():            The server declared support for GMCP
                    "GMCP            (name, value): A GMCP event was sent by the server
                    "GMCP:{pkgName}" (value):       The server sent the value of the GMCP
                                                    package {pkgName} (ex: "GMCP:room.info")
                    "MSDP ENABLED"   ():            The server declared support for MSDP
                    "MSDP"           (name, value): An MSDP variable was sent by the server.
                    "MSDP:{varName}" (value):       The server sent the value of the MSDP
                                                    variable {varName} (ex: "MSDP:COMMANDS")
                """.trimIndent() }
        }
    ) { eventName: String, handler: Any -> mode.defineEvent(eventName, handler) }
}
