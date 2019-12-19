package net.dhleong.judo

import net.dhleong.judo.net.JudoConnection
import java.net.URI

/**
 * @author dhleong
 */
object DummyConnectionFactory : JudoConnection.Factory {
    override suspend fun create(judo: IJudoCore, uri: URI): JudoConnection {
        TODO("not implemented")
    }
}
