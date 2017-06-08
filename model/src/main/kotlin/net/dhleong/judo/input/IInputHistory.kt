package net.dhleong.judo.input

/**
 * @author dhleong
 */

interface IInputHistory {

    fun clear()
    fun push(line: String)
    fun resetHistoryOffset()

    /**
     * Scroll the history by [dir], where positive numbers move to more recent
     * items and negative numbers move to older, updating the attached InputBuffer
     */
    fun scroll(dir: Int)

    /**
     * Search backwards from the current historyOffset position
     *  for a string matching the [match]. If a match was found,
     *  the buffer is set to that value; if not, nothing will change
     *
     * @return True if a match was found, else false.
     */
    fun search(match: String, forceNext: Boolean): Boolean
}
