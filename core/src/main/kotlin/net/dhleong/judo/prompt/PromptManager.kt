package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.asFlavorableBuilder
import net.dhleong.judo.util.PatternSpec

/**
 * @author dhleong
 */
class PromptManager : IPromptManager {

    override val size: Int
        get() = delegate.aliases.size

    private val delegate = AliasManager()

    override fun clear() {
        delegate.clear()
    }

    override fun clear(entry: String) = delegate.undefine(entry)

    override fun define(inputSpec: String, outputSpec: String) {
        // TODO support multiple prompts?
        delegate.aliases.clear()
        delegate.define(inputSpec, outputSpec)
    }

    override fun define(inputSpec: String, parser: AliasProcesser) {
        // TODO support multiple prompts?
        delegate.aliases.clear()
        delegate.define(inputSpec, parser)
    }

    override fun define(inputSpec: PatternSpec, outputSpec: String) {
        // TODO support multiple prompts?
        delegate.aliases.clear()
        delegate.define(inputSpec, outputSpec)
    }

    override fun define(inputSpec: PatternSpec, parser: AliasProcesser) {
        // TODO support multiple prompts?
        delegate.aliases.clear()
        delegate.define(inputSpec, parser)
    }

    override fun process(
        input: FlavorableCharSequence,
        onPrompt: (index: Int, prompt: String) -> Unit
    ): FlavorableCharSequence {
        val toProcess = input.asFlavorableBuilder()

        // in general, it *should* have a newline
        val hadNewline = input.endsWith('\n')
        if (hadNewline) {
            // don't process with trailing newlines
            toProcess.setLength(input.length - 1)
        }
        val result = delegate.process(toProcess) { index, prompt ->
            onPrompt(index, prompt)
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


