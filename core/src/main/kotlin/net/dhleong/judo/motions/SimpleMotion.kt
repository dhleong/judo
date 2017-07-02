package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun charMotion(step: Int) =
    createMotion { _, start ->
        val end = maxOf(0, start + step)

        start..end
    }

// special purpose motion for the x/X delete actions
fun xCharMotion(step: Int) =
    createMotion { _, start ->
        if (step < 0) {
            val end = start + step
            end..(start - 1)
        } else {
            start..(start + step - 1)
        }
    }

fun toEndMotion(): Motion =
    createMotion(Motion.Flags.INCLUSIVE) { buffer, cursor ->
        cursor..buffer.lastIndex
    }

fun toStartMotion(): Motion =
    createMotion { _, cursor ->
        cursor..0
    }

