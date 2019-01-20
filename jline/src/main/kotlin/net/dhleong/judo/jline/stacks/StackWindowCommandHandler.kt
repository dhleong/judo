package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow

class CountingStackSearch(
    var current: IJLineWindow?,
    var count: Int
)

interface StackWindowCommandHandler {
    fun focusUp(search: CountingStackSearch)
    fun focusDown(search: CountingStackSearch)

    fun focusLeft(search: CountingStackSearch)
    fun focusRight(search: CountingStackSearch)
}
