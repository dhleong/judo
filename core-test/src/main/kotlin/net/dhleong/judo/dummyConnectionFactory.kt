package net.dhleong.judo

import net.dhleong.judo.net.JudoConnection

/**
 * @author dhleong
 */
object DummyConnectionFactory : JudoConnection.Factory {
    override fun create(judo: IJudoCore, address: String, port: Int): JudoConnection {
        TODO("not implemented")
    }
}
