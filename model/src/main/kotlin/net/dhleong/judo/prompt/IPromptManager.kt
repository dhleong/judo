package net.dhleong.judo.prompt

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

/**
 * If a prompt is declared with this `group` value,
 * it will be atomatically assigned a unique, negative
 * group ID
 */
const val AUTO_UNIQUE_GROUP_ID = 0

/**
 * Prompts are like Aliases that act on output and
 * only ever return an empty string, instead passing
 * replacement value to the [net.dhleong.judo.JudoRenderer]
 *
 * @author dhleong
 */

interface IPromptManager : Clearable<String> {

    fun define(inputSpec: String, outputSpec: String, group: Int = AUTO_UNIQUE_GROUP_ID)
    fun define(inputSpec: String, parser: AliasProcesser, group: Int = AUTO_UNIQUE_GROUP_ID)

    fun define(inputSpec: PatternSpec, outputSpec: String, group: Int = AUTO_UNIQUE_GROUP_ID)
    fun define(inputSpec: PatternSpec, parser: AliasProcesser, group: Int = AUTO_UNIQUE_GROUP_ID)

    fun process(
        input: FlavorableCharSequence,
        onPrompt: (group: Int, prompt: String, index: Int) -> Unit
    ): FlavorableCharSequence

    val size: Int
}
