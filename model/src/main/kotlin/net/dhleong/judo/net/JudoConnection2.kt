package net.dhleong.judo.net

import net.dhleong.judo.render.FlavorableCharSequence
import java.io.Closeable
import java.io.IOException

/**
 * @author dhleong
 */
interface JudoConnection2 : Closeable {
    interface Factory {
        fun create(address: String, port: Int): JudoConnection2
    }

    val isMsdpEnabled: Boolean
    val isGmcpEnabled: Boolean

    var onError: ((IOException) -> Unit)?
    var onDisconnect: ((JudoConnection) -> Unit)?
    var onEchoStateChanged: ((Boolean) -> Unit)?

    fun send(line: String)

    fun setWindowSize(width: Int, height: Int)

    fun forEachLine(onNewLine: (FlavorableCharSequence) -> Unit)
}
