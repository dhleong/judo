package net.dhleong.judo.trigger

import net.dhleong.judo.alias.AliasManager

/**
 * @author dhleong
 */
class TriggerManager : ITriggerManager {

    val aliases = AliasManager()

    override fun clear() =
        aliases.clear()

    override fun define(inputSpec: String, parser: TriggerProcessor) {
        aliases.define(inputSpec, { args ->
            parser.invoke(args)
            "" // ignore result
        })
    }

    override fun process(input: CharSequence) {
        aliases.process(input)
    }

    override fun undefine(inputSpec: String) {
        aliases.undefine(inputSpec)
    }

    fun hasTriggerFor(inputSpec: String): Boolean =
        aliases.hasAliasFor(inputSpec)

    override fun toString(): String =
        StringBuilder(1024).apply {
            appendln("Triggers:")
            appendln("=========")
            aliases.describeContentsTo(this)
        }.toString()
}
