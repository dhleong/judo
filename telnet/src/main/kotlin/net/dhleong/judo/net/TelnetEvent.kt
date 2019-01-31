package net.dhleong.judo.net

import java.io.ByteArrayInputStream

/**
 * @author dhleong
 */
class TelnetEvent(
    private val data: ByteArray,

    /**
     * Start must point to the first byte *after* IAC
     */
    private val start: Int = 0,
    private val end: Int = data.size
) : Sequence<Byte> {

    val length: Int = end - start

    /**
     * Deep copy; [TelnetEvent] normally shares its ByteArray, so if you need
     * to keep an event around outside the handler, you MUST make a [copy].
     */
    fun copy(): TelnetEvent {
        val bytes = ByteArray(length)
        System.arraycopy(data, start, bytes, 0, length)
        return TelnetEvent(bytes, 0, length)
    }

    operator fun get(index: Int): Byte {
        val idx = start + index
        if (idx < 0 || idx >= length) throw ArrayIndexOutOfBoundsException(index)

        return data[idx]
    }

    override fun iterator(): Iterator<Byte> =
        data.asSequence()
            .drop(start)
            .take(length)
            .iterator()

    fun toInputStream(start: Int, length: Int) = ByteArrayInputStream(
        data,
        this.start + start,
        length
    )

    fun toString(start: Int, length: Int) = String(
        data,
        this.start + start,
        length
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TelnetEvent

        if (length != other.length) return false

        for (i in 0 until length) {
            if (this[i] != other[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (i in start until end) {
            result = 31 * result + data[i]
        }

        return result
    }

    override fun toString(): String = StringBuilder("TelnetEvent{").apply {
        for (i in 0 until this@TelnetEvent.length) {
            val byte = this@TelnetEvent[i]
            if (Character.isLetterOrDigit(byte.toChar())) {
                append(byte.toChar())
            } else {
                append("\\x")
                append((byte.toInt() and 0xff).toString(16))
            }
        }
        append("}")
    }.toString()

}

inline fun TelnetEvent.indexOfFirst(
    startIndex: Int = 0,
    predicate: (Byte) -> Boolean
): Int {
    for (index in startIndex until length) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

fun TelnetEvent.indexOf(
    byte: Byte,
    startIndex: Int = 0
): Int = indexOfFirst(startIndex) { it == byte }

