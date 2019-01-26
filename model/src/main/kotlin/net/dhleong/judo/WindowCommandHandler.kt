package net.dhleong.judo

/**
 * @author dhleong
 */
interface WindowCommandHandler {
    fun focusLeft(count: Int = 1)
    fun focusRight(count: Int = 1)
    fun focusUp(count: Int = 1)
    fun focusDown(count: Int = 1)

    // other potential ones:
    // top-left-most
    // bottom-right-most
    // previous (last accessed)
}