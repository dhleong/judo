package net.dhleong.judo.render

import net.dhleong.judo.util.CircularArrayList

private const val DEFAULT_SCROLLBACK_SIZE = 20_000

open class JudoBuffer2(
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : IJudoBuffer2 {

    override val id: Int = ids.newBuffer()

    private val contents = CircularArrayList<FlavorableCharSequence>(scrollbackSize)

    override val size: Int
        get() = contents.size
    override val lastIndex: Int
        get() = contents.lastIndex

    override fun get(index: Int): FlavorableCharSequence = contents[index]

    override fun append(text: FlavorableCharSequence) {
        if (contents.isEmpty()) {
            appendLine(text)
        } else {
            contents.last() += text
        }
    }

    override fun appendLine(line: FlavorableCharSequence) {
        contents.add(line)
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

}