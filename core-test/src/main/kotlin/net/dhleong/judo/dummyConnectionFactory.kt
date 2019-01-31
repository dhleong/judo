package net.dhleong.judo

import net.dhleong.judo.net.JudoConnection
import java.net.URI

/**
 * @author dhleong
 */
object DummyConnectionFactory : JudoConnection.Factory {
    override fun create(judo: IJudoCore, uri: URI): JudoConnection {
        TODO("not implemented")
    }
}
