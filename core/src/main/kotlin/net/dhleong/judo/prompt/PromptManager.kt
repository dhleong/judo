package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.util.ReplaceableAttributedStringBuilder

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
        if (delegate.aliases.isNotEmpty()) {
            throw IllegalStateException("Only a single prompt is supported right now")
        }

        delegate.define(inputSpec, outputSpec)
    }

    override fun define(inputSpec: String, parser: AliasProcesser) {
        if (delegate.aliases.isNotEmpty()) {
            throw IllegalStateException("Only a single prompt is supported right now")
        }

        delegate.define(inputSpec, parser)
    }

    override fun process(input: CharSequence, onPrompt: (index: Int, prompt: String) -> Unit): CharSequence {

        // NOTE: I don't love the dependency on JLine in the core
        //  here, but it seems to be the best way to process output
        //  for prompts without losing the ansi stuff....
        val withAnsi = ReplaceableAttributedStringBuilder(input.length).apply {
            appendAnsi(input.toString())
        }

        return delegate.process(withAnsi) { index, prompt ->
            onPrompt(index, prompt)
            "" // always replace with empty string
        }
    }

}
