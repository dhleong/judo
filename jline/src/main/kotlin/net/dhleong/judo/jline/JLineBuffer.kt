package net.dhleong.judo.jline

import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer

/**
 * @author dhleong
 */
class JLineBuffer(
    private val renderer: JLineRenderer,
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : JudoBuffer(
    ids,
    scrollbackSize
) {
    override fun append(text: FlavorableCharSequence) = renderer.inTransaction {
        super.append(text)
    }

    override fun appendLine(line: FlavorableCharSequence) = renderer.inTransaction {
        super.appendLine(line)
    }

    override fun clear() = renderer.inTransaction {
        super.clear()
    }

    override fun deleteLast(): FlavorableCharSequence  = renderer.inTransaction {
        return super.deleteLast()
    }

    override fun replaceLastLine(result: FlavorableCharSequence) = renderer.inTransaction {
        super.replaceLastLine(result)
    }

    override fun set(newContents: List<FlavorableCharSequence>) = renderer.inTransaction {
        super.set(newContents)
    }

    override fun set(index: Int, line: FlavorableCharSequence) = renderer.inTransaction {
        super.set(index, line)
    }
}