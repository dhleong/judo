package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.MultiTriggerType
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
                arg("inputSpecs", "List<String/Pattern>")
                arg("options", "String", flags = PatternProcessingFlags::class.java)
                arg("handler", "Fn")
            }

            body { """
                Declare a multitrigger. See :help alias for more about inputSpec.
                `options` is an optional, space-separated string that may contain any of:
                     color - Keep color codes in the values passed to the handler
            """.trimIndent() }
        }
    ) { args: Array<Any> ->
        val id = args[0] as String
        val flags = if (args.size == 5) {
            args[3] as String
        } else ""

        val rawType = args[1] as String
        val rawSpecs = engine.toJava(args[2]) as List<*>
        val patterns = rawSpecs.map { rawSpec ->
            compilePatternSpec(rawSpec as Any, flags)
        }

        val rawFn = args.last()
        val fn = engine.callableToFunction1(rawFn)

        queueMultiTrigger(id)
        judo.multiTriggers.define(
            id = id,
            type = MultiTriggerType.valueOf(rawType.toUpperCase()),
            options = MultiTriggerOptions(
                color = "color" in flags,
                delete = "delete" in flags
            ),
            patterns = patterns,
            processor = { fn(it) }
        )
    }
}
