package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.FnObject
import net.dhleong.judo.script.PatternObject
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.SupportsDecorator
import net.dhleong.judo.script.compilePatternSpec

/**
 * @author dhleong
 */
fun ScriptInitContext.initAliases() =
    sequenceOf(AliasScripting(this))

@Suppress("unused")
class AliasScripting(
    private val context: ScriptInitContext
) : ScriptingObject {
    @Doc("""
        Create a text alias. You may use Regex objects to match against
        a regular expression. In Python, for example:
            import re
            alias(re.compile("^study (.*)"), "say I'd like to study '$1'")
    """)
    fun alias(@PatternObject spec: Any, outputSpec: String) = with(context) {
        mode.defineAlias(compilePatternSpec(spec, ""), outputSpec)
    }
    @SupportsDecorator
    fun alias(@PatternObject spec: Any, @FnObject handler: Any) = with(context) {
        mode.defineAlias(compilePatternSpec(spec, ""), handler)
    }

    @Doc("""
         Delete the alias with the specified inputSpec       
    """)
    fun unalias(inputSpec: String) = with(context) {
        judo.aliases.undefine(inputSpec)
    }
}
