package net.dhleong.judo.motions

/**
 * Repeat any motion N times
 */
fun repeat(motion: Motion, count: Int): Motion {
    if (count < 1) throw IllegalArgumentException("Invalid repeat count ($count)")
    if (count == 1) return motion

    return createMotion { readKey, buffer, originalStart ->
        var currentMotion = motion
        var start = originalStart
        var end = start
        for (i in 1..count) {
            val intermediate = currentMotion.calculate(readKey, buffer, end)
            start = if (intermediate.first < intermediate.last) {
                minOf(start, intermediate.first)
            } else {
                maxOf(start, intermediate.first)
            }

            end = intermediate.last
            currentMotion = currentMotion.toRepeatable()
        }

        start..end
    }
}