package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun findMotion(step: Int) =
    createMotion { readKey, buffer, start ->
        val target = readKey()
        val end: Int
        if (step > 0) {
            end = buffer.indexOf(target.keyChar, start)
        } else {
            end = buffer.lastIndexOf(target.keyChar, start)
        }

        if (end == -1) {
            start..start
        } else {
            start..end
        }
    }
