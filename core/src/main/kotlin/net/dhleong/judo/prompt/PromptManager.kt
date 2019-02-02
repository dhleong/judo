package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.IAlias
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.asFlavorableBuilder
import net.dhleong.judo.util.PatternSpec
import java.util.concurrent.atomic.AtomicInteger

private class PromptGroup(
    val id: Int,
    private val entries: MutableList<IAlias> = mutableListOf()
) {

    val size: Int
        get() = entries.size

    fun add(alias: IAlias) {
        entries += alias
    }

    fun indexOf(alias: IAlias) = entries.indexOf(alias)
    fun removeEntry(entry: String) {
        entries.removeIf { it.original == entry }
    }
}

/**
 * @author dhleong
 */
class PromptManager : IPromptManager {

    override val size: Int
        get() = delegate.aliases.size

    private val delegate = AliasManager()
    private val groupsById = mutableMapOf<Int, PromptGroup>()
    private val groupsForAlias = mutableMapOf<String, PromptGroup>()
    private val nextUniqueGroupId = AtomicInteger(-1)

    override fun clear() {
        delegate.clear()
        groupsById.clear()
        groupsForAlias.clear()
    }

    override fun clear(entry: String) {
        delegate.undefine(entry)
        groupsForAlias.remove(entry)?.let { group ->
            group.removeEntry(entry)
            if (group.size == 0) {
                groupsById.remove(group.id)
            }
        }
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

    override fun define(inputSpec: PatternSpec, parser: AliasProcesser, group: Int) {
        define(group, delegate.define(inputSpec, parser))
    }

    private fun define(group: Int, alias: IAlias) {
        groupsForAlias[alias.original] = when (group) {
            AUTO_UNIQUE_GROUP_ID -> createGroup(nextUniqueGroupId.getAndDecrement())
            else -> groupsById[group] ?: createGroup(group)
        }.apply {
            add(alias)
        }
    }

    private fun createGroup(id: Int): PromptGroup = PromptGroup(id).also {
        groupsById[id] = it
    }

    override fun process(
        input: FlavorableCharSequence,
        onPrompt: (group: Int, prompt: String, index: Int) -> Unit
    ): FlavorableCharSequence {
        val toProcess = input.asFlavorableBuilder()

        // in general, it *should* have a newline
        val hadNewline = input.removeTrailingNewline()
        val result = delegate.process(toProcess) { alias, prompt ->
            val group = groupsForAlias[alias.original]
                ?: throw IllegalStateException("No group for prompt")

            val index = group.indexOf(alias)
            onPrompt(group.id, prompt, index)
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


