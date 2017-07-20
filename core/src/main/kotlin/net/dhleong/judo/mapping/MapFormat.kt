package net.dhleong.judo.mapping

import java.io.InputStream
import java.io.OutputStream

/**
 * @author dhleong
 */
interface MapFormat {
    val name: String
    fun read(map: IJudoMap, input: InputStream)
    fun write(map: IJudoMap, out: OutputStream)
}