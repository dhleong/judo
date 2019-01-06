package net.dhleong.judo

import net.dhleong.judo.net.JudoConnection2

/**
 * @author dhleong
 */
object DummyConnectionFactory : JudoConnection2.Factory {
    override fun create(address: String, port: Int): JudoConnection2 {
        TODO("not implemented")
    }
}
