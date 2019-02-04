package net.dhleong.judo

import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Keys
import java.util.concurrent.atomic.AtomicReference

/**
 * @author dhleong
 */
inline fun assertionsWhileTyping(
    judo: JudoCore,
    crossinline block: suspend SequenceScope<Key>.() -> Unit
) {
    // NOTE: we have to catch any exceptions (including from
    // assertions) and re-throw them later, since feedKeys
    // normally consumes exceptions and prints them to the buffer
    val error = AtomicReference<Throwable>(null)
    judo.feedKeys(sequence {
        try {
            block()
        } catch (e: Throwable) {
            error.set(e)
        }
    })
    error.get()?.let { throw it }
}

suspend fun SequenceScope<Key>.yieldKeys(keys: String) {
    yieldAll(Keys.parse(keys))
}
