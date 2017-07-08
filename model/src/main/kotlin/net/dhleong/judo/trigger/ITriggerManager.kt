package net.dhleong.judo.trigger

import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

typealias TriggerProcessor = (args: Array<String>) -> Unit

/**
 * Triggers are a lot like Aliases, except they don't
 * return any value, and they run on text output by
 * the server, rather than input from the user.
 *
 * @author dhleong
 */
interface ITriggerManager : Clearable<String> {
    fun define(inputSpec: String, parser: TriggerProcessor)
    fun define(inputSpec: PatternSpec, parser: TriggerProcessor)

    fun process(input: CharSequence)

    fun undefine(inputSpec: String)
}
