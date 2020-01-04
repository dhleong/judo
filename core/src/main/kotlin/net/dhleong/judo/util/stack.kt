package net.dhleong.judo.util

import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.render.toFlavorable

/**
 * Stack trace cleanup
 *
 * @author dhleong
 */
fun Throwable.formatStack(prefix: String = "") = sequence {

    // NOTE: Kotlin barfs on a recursive implementation, so we break it up into
    // this method and a helper method
    val root = this@formatStack
    var thisPrefix = prefix
    var cause: Throwable? = root
    while (cause != null) {
        yieldAll(cause.formatSingleStack(thisPrefix))

        thisPrefix = if (
            cause === root
            && cause is ScriptExecutionException
            && "\n" in cause.message ?: ""
            && cause.cause is IllegalArgumentException
        ) {
            // NOTE: for a script exception caused by an IllegalArgumentException
            // that has a multi-line message, we want a blank prefix for that cause,
            // because "Caused by:" doesn't really make sense
            ""
        } else {
            "Caused by: "
        }

        cause = cause.cause
    }

}

private fun Throwable.formatSingleStack(
    prefix: String = "",
    maxLines: Int = 7
) = sequence {
    val root = this@formatSingleStack
    if (root is ScriptExecutionException) {
        yield("${prefix}ScriptExecutionException")

        val parts = root.message?.split("\n")
        if (parts != null) {
            val causeLine = root.cause?.let { e ->
                "${e.javaClass.name}: ${e.message}"
            }

            // NOTE: some script engines embed a string stack into
            // ScriptExecutionException that may also include a
            // dup of the cause exception
            yieldAll(parts.asSequence()
                .takeWhile { it != causeLine }
                .filterRelevantStackLines())
            return@sequence
        }
    } else {
        yield("${prefix}${root.javaClass.name}: ${root.message}")
    }

    val linesToTake = when {
        root is IllegalArgumentException && root.cause != null -> 1
        else -> maxLines
    }

    yieldAll(
        stackTrace.asSequence()
            .map { "\tat $it" }
            .filterRelevantStackLines()
            .take(linesToTake + 1)
            .mapIndexed { index, line ->
                if (index < linesToTake) line
                else "\t... more"
            }
    )

}.map { it.toFlavorable() }

private fun Sequence<String>.filterRelevantStackLines(): Sequence<String> =
    sequence {
        var lastWasCoroutine = false
        loop@ for (element in this@filterRelevantStackLines) {
            val isCoroutineLib = element.matches(Regex(
                """^kotlin[x]?\.coroutines.*"""
            ))
            when {
                isCoroutineLib && !lastWasCoroutine -> {
                    lastWasCoroutine = true
                    yield("\t... coroutines ...")
                    continue@loop
                }

                // ignore all the noisy coroutine libs
                isCoroutineLib -> continue@loop

                // not useful line:
                (
                    element.matches(Regex("\\.invoke(Suspend)?"))
                ) -> continue@loop

                // also unnecessary
                "BlockingKeySourceChannelAdapter" in element -> continue@loop

                lastWasCoroutine -> lastWasCoroutine = false
            }

            // default to the original element
            yield(element)
        }
    }
