package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initTriggers() = with(mode) {
    registerFn<Unit>(
        "trigger",
        doc {
            usage(decorator = true) {
                arg("inputSpec", "String/Pattern")
                arg("options", "String", flags = PatternProcessingFlags::class.java)
                arg("handler", "Fn")
            }

            body { """
                    Declare a trigger. See :help alias for more about inputSpec.
                    `options` is an optional, space-separated string that may contain any of:
                         color - Keep color codes in the values passed to the handler
                """.trimIndent() }
        }
    ) { args: Array<Any> ->
        if (args.size == 2) {
            defineTrigger(compilePatternSpec(args[0], ""), args[1])
        } else {
            defineTrigger(compilePatternSpec(args[0], args[1] as String), args[2])
        }
    }

    registerFn<Unit>(
        "untrigger",
        doc {
            usage { arg("inputSpec", "String") }
            body { "Delete the trigger with the specified inputSpec" }
        }
    ) { inputSpec: String ->
        judo.triggers.undefine(inputSpec)
    }
}

