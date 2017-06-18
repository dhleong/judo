package net.dhleong.judo.input

import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class CountReadingBuffer {

    private var runningCount = 0

    /**
     * @return The actual, usable repeat count. This means that
     *  if no count was input, we'll return 1 anyway
     */
    fun toRepeatCount(): Int = maxOf(1, runningCount)

    fun tryPush(stroke: KeyStroke): Boolean {
        if (stroke.keyChar !in '0'..'9') return false

        val keyNumericValue = (stroke.keyChar - '0')
        if (keyNumericValue == 0 && runningCount == 0) {
            // no previous counts read, and this is just 0;
            // let that be a regular keypress
            return false
        }

        runningCount *= 10
        runningCount += keyNumericValue

        return true
    }

    fun clear() {
        runningCount = 0
    }
}
