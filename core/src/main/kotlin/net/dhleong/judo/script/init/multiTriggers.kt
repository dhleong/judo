package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.Flags
import net.dhleong.judo.script.FnObject
import net.dhleong.judo.script.Optional
import net.dhleong.judo.script.PatternObject
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.SupportsDecorator
import net.dhleong.judo.script.compilePatternSpec
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.MultiTriggerType
import net.dhleong.judo.util.Json
import net.dhleong.judo.util.PatternProcessingFlags

/**
 * @author dhleong
 */
fun ScriptInitContext.initMultiTriggers() = sequenceOf(
    MultiTriggerScripting(this)
)

@Suppress("unused")
class MultiTriggerScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
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
    """)
    @SupportsDecorator
    fun multitrigger(
        id: String,
        type: String,
        @Optional config: Map<String, Any>?,
        @PatternObject inputSpecs: List<Any>,
        @Flags(PatternProcessingFlags::class) options: String,
        @FnObject handler: Any
    ) = with(context) {

        val rawSpecs: List<*> = inputSpecs
        var typedOptions: MultiTriggerOptions = config?.let { optionsMap ->
            Json.adapter<MultiTriggerOptions>()
                    .fromJsonValue(optionsMap)
        } ?: MultiTriggerOptions()

        if (options.isNotEmpty()) {
            typedOptions = typedOptions.copy(
                color = "color" in options || typedOptions.color,
                delete = "delete" in options || typedOptions.delete
            )
        }

        val patterns = rawSpecs.map { rawSpec ->
            compilePatternSpec(rawSpec as Any, options)
        }

        val fn = engine.callableToFunction1(handler)

        mode.queueMultiTrigger(id)
        judo.multiTriggers.define(
            id = id,
            type = MultiTriggerType.valueOf(type.toUpperCase()),
            options = typedOptions,
            patterns = patterns,
            processor = { fn(it) }
        )
    }

    @Doc("""
        Delete the multitrigger with the specified id       
    """)
    fun unmultitrigger(id: String) = with(context) {
        judo.multiTriggers.undefine(id)
    }
}
