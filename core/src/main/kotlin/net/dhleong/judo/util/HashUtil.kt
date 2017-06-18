package net.dhleong.judo.util

import java.security.MessageDigest

/**
 * @author dhleong
 */

/**
 * Applies a SHA-1 hash (or, whatever you request) to the input
 * and returns the result as a string
 */
fun hash(text: String, algorithm: String = "SHA-1"): String {
    val bytes = text.toByteArray()
    val md = MessageDigest.getInstance(algorithm)
    val digest = md.digest(bytes)
    return digest.joinToString(separator = "") { "%02x".format(it) }
}
