package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow

class CountingStackSearch(
    var current: IJLineWindow?,
    var count: Int
)

interface StackWindowCommandHandler {
    /**
     * If [current] is null, that means start from the *bottom-most*
     */
    fun focusUp(search: CountingStackSearch)
    fun focusDown(search: CountingStackSearch)
}
