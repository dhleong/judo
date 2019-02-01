package net.dhleong.judo.net

import java.net.URI

/**
 * @author dhleong
 */
fun createURI(raw: String): URI {
    val colons = raw.count { it == ':' }
    if (colons == 1) {
        return URI("telnet://$raw")
    }

    return URI(raw)
}