package net.dhleong.judo.script.init

import net.dhleong.judo.prompt.AUTO_UNIQUE_GROUP_ID
import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.Flags
import net.dhleong.judo.script.FnObject
import net.dhleong.judo.script.Optional
import net.dhleong.judo.script.PatternObject
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.SupportsDecorator
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initPrompts() = sequenceOf(
    PromptScripting(this)
)

@Suppress("unused")
class PromptScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
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
    """)
    fun prompt(
        @Optional group: Int?,
        @PatternObject inputSpec: Any,
        @Flags(PatternProcessingFlags::class) options: String,
        outputSpec: String
    ) = prompt(group, inputSpec, options, outputSpec as Any)

    @SupportsDecorator
    fun prompt(
        @Optional group: Int?,
        @PatternObject inputSpec: Any,
        @Flags(PatternProcessingFlags::class) options: String,
        @FnObject handler: Any
    ) = with(context) {
        if (group == null) {
            mode.definePrompt(
                AUTO_UNIQUE_GROUP_ID,
                compilePatternSpec(inputSpec, options),
                handler
            )
        } else {
            mode.tryDefinePrompt(
                group,
                compilePatternSpec(inputSpec, options),
                handler
            )
        }
    }
}
