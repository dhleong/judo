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

    var onDisconnect: ((JudoConnection, reason: IOException?) -> Unit)?
    var onEchoStateChanged: ((Boolean) -> Unit)?

    suspend fun send(line: String)

    fun setWindowSize(width: Int, height: Int)

    /**
     * @param async If `false`, this will run as a blocking operation in the
     * current thread; this is mostly useful for unit tests. Defaults to `true`
     */
    fun forEachLine(
        async: Boolean = true,
        onNewLine: (FlavorableCharSequence) -> Unit
    )

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 20_000
    }
}
