package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.IAlias
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.asFlavorableBuilder
import net.dhleong.judo.util.PatternSpec
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author dhleong
 */
class PromptManager : IPromptManager {

    override val size: Int
        get() = delegate.aliases.size

    private val delegate = AliasManager()
    private val groupsForAlias = mutableMapOf<String, Int>()
    private val nextUniqueGroupId = AtomicInteger(-1)

    override fun clear() {
        delegate.clear()
        groupsForAlias.clear()
    }

    override fun clear(entry: String) {
        delegate.undefine(entry)
        groupsForAlias.remove(entry)
        nextUniqueGroupId.set(-1)
    }

    override fun define(inputSpec: String, outputSpec: String, group: Int) {
        define(group, delegate.define(inputSpec, outputSpec))
    }

    override fun define(inputSpec: String, parser: AliasProcesser, group: Int) {
        define(group, delegate.define(inputSpec, parser))
    }

    override fun define(inputSpec: PatternSpec, outputSpec: String, group: Int) {
        define(group, delegate.define(inputSpec, outputSpec))
    }

    override fun define(
        inputSpec: PatternSpec,
        parser: AliasProcesser,
        group: Int
    ) {
        define(group, delegate.define(inputSpec, parser))
    }

    private fun define(group: Int, alias: IAlias) {
        groupsForAlias[alias.original] = when (group) {
            AUTO_UNIQUE_GROUP_ID -> nextUniqueGroupId.getAndDecrement()
            else -> group
        }
    }

    override fun process(
        input: FlavorableCharSequence,
        onPrompt: (index: Int, prompt: String) -> Unit
    ): FlavorableCharSequence {
        val toProcess = input.asFlavorableBuilder()

        // in general, it *should* have a newline
        val hadNewline = input.removeTrailingNewline()
        val result = delegate.process(toProcess) { alias, prompt ->
            // FIXME: determine index within the group
            val group = groupsForAlias[alias.original] ?: 0
            onPrompt(0, prompt)
            "" // always replace with empty string
        }

        if (hadNewline) {
            // restore the newline
            toProcess += '\n'

            if (result.isNotEmpty() && result !== toProcess) {
                result += '\n'
            }
        }

        return result
    }

}


