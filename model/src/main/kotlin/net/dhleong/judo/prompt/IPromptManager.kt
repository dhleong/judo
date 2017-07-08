package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

/**
 * Prompts are like Aliases that act on output and
 * only ever return an empty string, instead passing
 * replacement value to the [net.dhleong.judo.JudoRenderer]
 *
 * @author dhleong
 */

interface IPromptManager : Clearable<String> {

    fun define(inputSpec: String, outputSpec: String)
    fun define(inputSpec: String, parser: AliasProcesser)

    fun define(inputSpec: PatternSpec, outputSpec: String)
    fun define(inputSpec: PatternSpec, parser: AliasProcesser)

    fun process(input: CharSequence, onPrompt: (index: Int, prompt: String) -> Unit): CharSequence

    val size: Int
}
