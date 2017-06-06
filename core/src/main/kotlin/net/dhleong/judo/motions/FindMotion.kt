package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun findMotion(step: Int) =
    createMotion(listOf(Motion.Flags.INCLUSIVE)) { readKey, buffer, start ->
        val target = readKey()
        val end: Int
        if (step > 0) {
            end = buffer.indexOf(target.keyChar, start + 1)
        } else {
            end = buffer.lastIndexOf(target.keyChar, start - 1)
        }

        if (end == -1) {
            start..start
        } else {
            start..end
        }
    }
