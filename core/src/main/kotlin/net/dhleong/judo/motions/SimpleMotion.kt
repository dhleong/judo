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
            end..end
        } else {
            start..start
        }
    }

fun toEndMotion(): Motion =
    createMotion { buffer, cursor ->
        cursor..buffer.length
    }

fun toStartMotion(): Motion =
    createMotion { _, cursor ->
        cursor..0
    }


/**
 * Repeat any motion N times
 */
fun repeat(motion: Motion, count: Int): Motion {
    if (count < 1) throw IllegalArgumentException("Invalid repeat count ($count)")

    return createMotion { readKey, buffer, start ->
        var end = start
        for (i in 1..count) {
            end = motion.calculate(readKey, buffer, end).endInclusive
        }

        start..end
    }
}

