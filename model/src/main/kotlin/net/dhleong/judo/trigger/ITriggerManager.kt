package net.dhleong.judo.trigger

typealias TriggerProcessor = (args: Array<String>) -> Unit

/**
 * Triggers are a lot like Aliases, except they don't
 * return any value, and they run on text output by
 * the server, rather than input from the user.
 *
 * @author dhleong
 */
interface ITriggerManager {
    fun clear()

    fun define(inputSpec: String, parser: TriggerProcessor)

    fun process(input: CharSequence)
}
