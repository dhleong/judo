package net.dhleong.judo.jline

import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer2

/**
 * @author dhleong
 */
class JLineBuffer2(
    private val renderer: JLineRenderer2,
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : JudoBuffer2(
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

    override fun replaceLastLine(result: FlavorableCharSequence) = renderer.inTransaction {
        super.replaceLastLine(result)
    }

    override fun set(newContents: List<FlavorableCharSequence>) = renderer.inTransaction {
        super.set(newContents)
    }
}