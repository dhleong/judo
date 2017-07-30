package net.dhleong.judo.net

import java.io.Closeable

/**
 * @author dhleong
 */
interface JudoConnection : Closeable {
    val isMsdpEnabled: Boolean
    val isGmcpEnabled: Boolean
}