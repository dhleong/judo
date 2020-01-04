package net.dhleong.judo.render

import net.dhleong.judo.DelegateStateMap
import net.dhleong.judo.IStateMap
import net.dhleong.judo.util.CircularArrayList

open class JudoBuffer(
    override val id: Int,
    settings: IStateMap,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : IJudoBuffer {

    constructor(
        ids: IdManager,
        settings: IStateMap,
        scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
    ) : this(ids.newBuffer(), settings, scrollbackSize)

    private val contents = CircularArrayList<FlavorableCharSequence>(scrollbackSize)

    override val settings: IStateMap = DelegateStateMap(settings)

    override val size: Int
        get() = contents.size
    override val lastIndex: Int
        get() = contents.lastIndex

    override fun get(index: Int): FlavorableCharSequence = contents[index]

    @Synchronized
    override fun append(text: FlavorableCharSequence) {
        text.splitAtNewlines(contents, continueIncompleteLines = true)
    }

    @Synchronized
    override fun appendLine(line: FlavorableCharSequence) {
        if (!line.endsWith('\n')) {
            line += '\n'
        }
        line.splitAtNewlines(contents, continueIncompleteLines = false)
    }

    @Synchronized
    override fun clear() {
        contents.clear()
    }

    @Synchronized
    override fun deleteLast() =
        contents.removeLast()

    @Synchronized
    override fun replaceLastLine(result: FlavorableCharSequence) {
        contents[contents.lastIndex] = result
    }

    @Synchronized
    override fun set(newContents: List<FlavorableCharSequence>) {
        clear()
        newContents.forEach(this::appendLine)
    }

    @Synchronized
    override fun set(index: Int, line: FlavorableCharSequence) {
        val newLine = line.indexOf('\n')
        require(newLine == -1 || newLine == line.lastIndex) {
            "Line must not have any newline characters in it"
        }
        if (!line.endsWith("\n")) {
            line += '\n'
        }
        contents[index] = line
    }

    companion object {
        const val DEFAULT_SCROLLBACK_SIZE = 20_000
    }
}