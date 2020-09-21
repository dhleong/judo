package net.dhleong.judo.util

import java.io.File

/**
 * @author dhleong
 */
inline class Profiling(
    private val startTime: Long = System.nanoTime()
) {
    fun stop(action: String) {
        if (isEnabled) {
            val durationNanos = System.nanoTime() - startTime
            val millis = (durationNanos / 1_000_000).toInt()
            File("timing.txt").appendText("[$action]: ${millis}ms\n")
        }
    }

    companion object {
        var isEnabled = false
    }
}

inline fun <R> withProfiling(label: String, op: () -> R): R {
    val profiling = Profiling()
    return op().also {
        profiling.stop(label)
    }
}