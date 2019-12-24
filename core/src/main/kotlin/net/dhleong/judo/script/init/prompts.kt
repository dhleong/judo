package net.dhleong.judo.script.init

import net.dhleong.judo.prompt.AUTO_UNIQUE_GROUP_ID
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initPrompts() = with(mode) {
    registerFn<Unit>(
        "prompt",
        doc {
            usage {
                arg("group", "Int", isOptional = true)
                arg("inputSpec", "Pattern/String")
                arg("options", "String", flags = PatternProcessingFlags::class.java)
                arg("outputSpec", "String")
            }

            usage(decorator = true) {
                arg("group", Integer::class.java, isOptional = true)
                arg("inputSpec", "Pattern/String")
                arg("options", "String", flags = PatternProcessingFlags::class.java)
                arg("handler", "Fn")
            }
            body { """
                    Prepare a prompt to be displayed in the status area.
                    See :help alias for more about `inputSpec`, and :help trigger for more
                    about the optional `options`.

                    You may optionally assign your prompt to a `group` in order to
                    display multiple prompts at once. The number must be greater than
                    zero (0) but is otherwise arbitrary, as long as you are consistent.
                    If `group` is not specified, the prompt will be added to its own group.

                    As long as the matched prompts belong to the same group they will
                    all be displayed, in the order you declared them, in the prompt
                    area. If a prompt from a different group is matched, only prompts
                    from *that* group will be displayed at that point, and so on.
                """.trimIndent() }
        }
    ) { args: Array<Any> -> when (args.size) {
        2 -> definePrompt(
            AUTO_UNIQUE_GROUP_ID,
            compilePatternSpec(args[0], ""), args[1]
        )
        3 -> {
            if (args[0] is Int) {
                // provided a group
                tryDefinePrompt(args[0] as Int, compilePatternSpec(args[1], ""), args[2])
            } else {
                // provided flags
                definePrompt(
                    AUTO_UNIQUE_GROUP_ID,
                    compilePatternSpec(args[0], args[1] as String), args[2]
                )
            }
        }
        4 -> tryDefinePrompt(args[0] as Int, compilePatternSpec(args[1], args[2] as String), args[3])
    } }
}

