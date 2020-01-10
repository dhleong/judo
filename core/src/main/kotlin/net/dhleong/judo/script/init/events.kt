package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.FnObject
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.SupportsDecorator

/**
 * @author dhleong
 */
fun ScriptInitContext.initEvents() = sequenceOf(
    EventsScripting(this)
)

@Suppress("unused")
class EventsScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @SupportsDecorator
    @Doc("""
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
    """)
    fun event(eventName: String, @FnObject handler: Any) {
        context.mode.defineEvent(eventName, handler)
    }
}
