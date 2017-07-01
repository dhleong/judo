package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.util.IStringBuilder
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

    override fun process(input: CharSequence, onPrompt: (index: Int, prompt: String) -> Unit): CharSequence {

        // NOTE: I don't love the dependency on JLine in the core
        //  here, but it seems to be the best way to process output
        //  for prompts without losing the ansi stuff....
        val withAnsi = IStringBuilder.from(input)

        return delegate.process(withAnsi) { index, prompt ->
            onPrompt(index, prompt)
            "" // always replace with empty string
        }
    }

}
