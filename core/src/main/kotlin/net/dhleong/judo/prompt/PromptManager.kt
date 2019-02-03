package net.dhleong.judo.prompt

import net.dhleong.judo.alias.Alias
import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.IAlias
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.util.PatternSpec
import java.util.concurrent.atomic.AtomicInteger

private class PromptGroup(
    val id: Int,
    val entries: MutableList<Prompt> = mutableListOf()
) {

    val size: Int
        get() = entries.size

    fun add(prompt: Prompt) {
        entries += prompt
    }

    inline fun forEachOutputter(
        except: Prompt,
        block: (Int, Prompt, FlavorableCharSequence) -> Unit
    ) {
        var index = 0

        for (entry in entries) {
            if (entry !== except) {
                entry.lastOutputProducingInput?.let { input ->
                    block(index, entry, input)

                    ++index
                }
            } else if (entry.lastOutputProducingInput != null) {
                // the entry to skip also produced output
                ++index
            }
        }
    }

    fun indexOf(alias: IAlias): Int {
        var index = 0

        for (entry in entries) {
            if (entry.alias === alias) {
                return index
            }

            // if entry hasn't produced any output,
            // do not count it toward the index of `alias`
            if (entry.lastOutputProducingInput != null) {
                ++index
            }
        }

        return -1
    }
    fun removeEntry(entry: String) {
        entries.removeIf { it.alias.original == entry }
    }
}

private class Prompt(
    val alias: IAlias,
    val group: PromptGroup,
    var lastOutputProducingInput: FlavorableCharSequence? = null
)

/**
 * @author dhleong
 */
class PromptManager : IPromptManager {

    override val size: Int
        get() = delegate.aliases.size

    private val delegate = AliasManager()
    private val groupsById = mutableMapOf<Int, PromptGroup>()
    private val aliasToPrompt = mutableMapOf<String, Prompt>()
    private val nextUniqueGroupId = AtomicInteger(-1)

    override fun clear() {
        delegate.clear()
        groupsById.clear()
        aliasToPrompt.clear()
    }

    override fun clear(entry: String) {
        delegate.undefine(entry)
        aliasToPrompt.remove(entry)?.let { prompt ->
            val group = prompt.group
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
        val groupObj = when (group) {
            AUTO_UNIQUE_GROUP_ID -> createGroup(nextUniqueGroupId.getAndDecrement())
            else -> groupsById[group] ?: createGroup(group)
        }
        val prompt = Prompt(alias, groupObj)
        groupObj.add(prompt)
        aliasToPrompt[alias.original] = prompt
    }

    private fun createGroup(id: Int): PromptGroup = PromptGroup(id).also {
        groupsById[id] = it
    }

    override fun process(
        input: FlavorableCharSequence,
        onPrompt: (group: Int, prompt: String, index: Int) -> Unit
    ): FlavorableCharSequence {

        // copy; we might want to keep the original
        val toProcess = FlavorableStringBuilder(input)

        // in general, it *should* have a newline
        val hadNewline = toProcess.removeTrailingNewline()
        val result = delegate.process(toProcess) { alias, prompt ->
            val promptObj = aliasToPrompt[alias.original]
                ?: throw IllegalStateException("Alias not registered as Prompt")
            val group = promptObj.group

            val index = group.indexOf(alias)
            if (prompt.isNullOrEmpty()) {

                // re-run any other prompts in the group that have produced output
                triggerOtherPrompts(promptObj, onPrompt)

            } else {
                promptObj.lastOutputProducingInput = input
                onPrompt(group.id, prompt, index)
            }

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

    private fun triggerOtherPrompts(
        except: Prompt,
        onPrompt: (group: Int, prompt: String, index: Int) -> Unit
    ) {
        val group = except.group
        group.forEachOutputter(except) { index, prompt, input ->
            // this cast is HAX!
            val parseInput = FlavorableStringBuilder(input)
            parseInput.removeTrailingNewline()
            (prompt.alias as Alias).parse(parseInput) { output ->
                onPrompt(group.id, output ?: "", index)
                ""
            }
        }
    }

}


