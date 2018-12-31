package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import java.io.Closeable
import java.io.IOException

/**
 * @author dhleong
 */
interface JudoConnection : Closeable {
    interface Factory {
        fun create(judo: IJudoCore, address: String, port: Int): JudoConnection
    }

    val isMsdpEnabled: Boolean
    val isGmcpEnabled: Boolean

    var onError: ((IOException) -> Unit)?
    var onDisconnect: ((JudoConnection) -> Unit)?
    var onEchoStateChanged: ((Boolean) -> Unit)?

    fun send(line: String)

    fun setWindowSize(width: Int, height: Int)

    fun forEachLine(onNewLine: (CharArray, Int) -> Unit)
}