package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.MultiTriggerType
import net.dhleong.judo.util.Json
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initMultiTriggers() = with(mode) {
    registerFn<Unit>(
        "multitrigger",
        doc {
            usage(decorator = true) {
                arg("id", "String")
                arg("type", "String")
                arg("config", Map::class.java, isOptional = true)
                arg("inputSpecs", "List<String/Pattern>")
                arg("options", "String", flags = PatternProcessingFlags::class.java)
                arg("handler", "Fn")
            }

            body { """
                Declare a multitrigger. See :help alias for more about inputSpec.
                
                `type` must be one of:
                    "range" - expects two values for inputSpecs: a *start* and an *end*.
                              The multitrigger will start collecting lines when *start*
                              matches and stop when *end* matches, and all matched lines
                              will be sent to the handler in a list
                `config` is an optional map with keys:
                    maxLines - the most lines that should be captured by this multitrigger;
                               if more get captured we will stop processing and print an error
                `options` is an optional, space-separated string that may contain any of:
                     color - Keep color codes in the values passed to the handler
                     delete - Delete matching lines instead of keeping them in the buffer
                     
                In general, you can omit `config` and use `options` like you're used to for
                other functions. The flags for `options` can also be used as keys for `config`
                (with boolean values) if you prefer, however.
            """.trimIndent() }
        }
    ) { args: Array<Any> ->
        val id = args[0] as String
        val rawType = args[1] as String
        val type = MultiTriggerType.valueOf(rawType.toUpperCase())

        val rawSpecs: List<*>
        var options: MultiTriggerOptions
        var i = 2
        if (args[i] is List<*>) {
            options = MultiTriggerOptions()
            rawSpecs = args[i++] as List<*>
        } else {
            val optionsMap = args[i++] as Map<*, *>
            options = Json.adapter<MultiTriggerOptions>()
                .fromJsonValue(optionsMap)
                ?: MultiTriggerOptions()
            rawSpecs = args[i++] as List<*>
        }

        val flags = if (args[i] !is String) ""
            else {
                (args[i] as String).also { flags ->
                    options = options.copy(
                        color = "color" in flags || options.color,
                        delete = "delete" in flags || options.delete
                    )
                }
            }

        val patterns = rawSpecs.map { rawSpec ->
            compilePatternSpec(rawSpec as Any, flags)
        }

        val rawFn = args.last()
        val fn = engine.callableToFunction1(rawFn)

        queueMultiTrigger(id)
        judo.multiTriggers.define(
            id = id,
            type = type,
            options = options,
            patterns = patterns,
            processor = { fn(it) }
        )
    }

    registerFn<Unit>(
        "unmultitrigger",
        doc {
            usage { arg("id", "String") }
            body { "Delete the multitrigger with the specified id" }
        }
    ) { id: String ->
        judo.multiTriggers.undefine(id)
    }
}
