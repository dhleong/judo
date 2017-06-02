package net.dhleong.judo.motions

/**
 * @author dhleong
 */

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

    return createMotion { buffer, start ->
        var end = start
        for (i in 1..count) {
            end = motion.calculate(buffer, end).endInclusive
        }

        start..end
    }
}

