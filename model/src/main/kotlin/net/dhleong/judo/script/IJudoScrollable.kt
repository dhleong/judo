package net.dhleong.judo.script

/**
 * @author dhleong
 */
interface IJudoScrollable {

    /**
     * @param count Number of lines to scroll, where a POSITIVE number
     *  moves backward in history, and a NEGATIVE number moves forward
     */
    fun scrollLines(count: Int)

    /**
     * @see scrollLines
     */
    fun scrollPages(count: Int)

    /**
     * Scroll based on the [net.dhleong.judo.SCROLL] setting
     */
    fun scrollBySetting(count: Int)

    fun scrollToBottom()

}
