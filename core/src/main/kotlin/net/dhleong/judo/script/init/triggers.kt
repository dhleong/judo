package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.Flags
import net.dhleong.judo.script.FnObject
import net.dhleong.judo.script.PatternObject
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.SupportsDecorator
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initTriggers() = sequenceOf(
    TriggerScripting(this)
)

@Suppress("unused")
class TriggerScripting(
    private val context: ScriptInitContext
) : ScriptingObject {
    @Doc("""
        Declare a trigger. See :help alias for more about inputSpec.
        `options` is an optional, space-separated string that may contain any of:
             color - Keep color codes in the values passed to the handler
    """)
    @SupportsDecorator
    fun trigger(
        @PatternObject inputSpec: Any,
        @Flags(PatternProcessingFlags::class) options: String,
        @FnObject handler: Any
    ) = with(context) {
        mode.defineTrigger(compilePatternSpec(inputSpec, options), handler)
    }

    @Doc("Delete the trigger with the specified inputSpec")
    fun untrigger(inputSpec: String) = with(context) {
        judo.triggers.undefine(inputSpec)
    }
}
