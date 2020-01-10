package net.dhleong.judo.script.init

import net.dhleong.judo.net.createURI
import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.registerFrom

/**
 * @author dhleong
 */
fun ScriptInitContext.initConnection() =
    sequenceOf(ConnectionScripting(this))

@Suppress("unused")
class ConnectionScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("Connect to a server.")
    fun connect(uri: String) = context.judo.connect(createURI(uri))
    fun connect(host: String, port: Int) =
        context.judo.connect(createURI("$host:$port"))

    @Doc("Disconnect from the server.")
    fun disconnect() {
        context.judo.disconnect()
    }

    @Doc("Check if connected.")
    fun isConnected() = context.judo.isConnected()

    @Doc("Repeat the last connect()")
    fun reconnect() = context.judo.reconnect()

}
