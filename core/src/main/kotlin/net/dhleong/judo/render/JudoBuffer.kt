package net.dhleong.judo.render

import net.dhleong.judo.util.CircularArrayList

open class JudoBuffer(
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : IJudoBuffer {

    override val id: Int = ids.newBuffer()

    private val contents = CircularArrayList<FlavorableCharSequence>(scrollbackSize)

    override val size: Int
        get() = contents.size
    override val lastIndex: Int
        get() = contents.lastIndex

    override fun get(index: Int): FlavorableCharSequence = contents[index]

    override fun append(text: FlavorableCharSequence) {
        text.splitAtNewlines(contents, continueIncompleteLines = true)
    }

    override fun appendLine(line: FlavorableCharSequence) {
        if (!line.endsWith('\n')) {
            line += '\n'
        }
        line.splitAtNewlines(contents, continueIncompleteLines = false)
    }

    override fun clear() {
        contents.clear()
    }

    override fun replaceLastLine(result: FlavorableCharSequence) {
        contents[contents.lastIndex] = result
    }

    override fun set(newContents: List<FlavorableCharSequence>) {
        clear()
        newContents.forEach(this::appendLine)
    }

    companion object {
        const val DEFAULT_SCROLLBACK_SIZE = 20_000
    }
}