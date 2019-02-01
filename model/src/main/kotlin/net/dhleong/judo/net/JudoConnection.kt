package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.render.FlavorableCharSequence
import java.io.Closeable
import java.io.IOException
import java.net.URI

/**
 * @author dhleong
 */
interface JudoConnection : Closeable {
    interface Factory {
        fun create(judo: IJudoCore, uri: URI): JudoConnection?
    }

    val isMsdpEnabled: Boolean
    val isGmcpEnabled: Boolean

    var onError: ((IOException) -> Unit)?
    var onDisconnect: ((JudoConnection) -> Unit)?
    var onEchoStateChanged: ((Boolean) -> Unit)?

    suspend fun send(line: String)

    fun setWindowSize(width: Int, height: Int)

    fun forEachLine(onNewLine: (FlavorableCharSequence) -> Unit)

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 20_000
    }
}
