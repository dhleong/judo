package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initAliases() = with(engine) {
    // aliasing
    registerFn<Unit>(
        "alias",
        doc {
            usage {
                arg("inputSpec", "String/Pattern")
                arg("outputSpec", "String")
            }
            usage(decorator = true) {
                arg("inputSpec", "String/Pattern")
                arg("handler", "Fn")
            }

            body { """
                    Create a text alias. You may use Regex objects to match against
                    a regular expression. In Python, for example:
                        import re
                        alias(re.compile("^study (.*)"), "say I'd like to study '$1'")
                """.trimIndent() }
        }
    ) { spec: Any, alias: Any ->
        mode.defineAlias(compilePatternSpec(spec, ""), alias)
    }

    registerFn<Unit>(
        "unalias",
        doc {
            usage { arg("inputSpec", "String") }
            body { "Delete the alias with the specified inputSpec" }
        }
    ) { inputSpec: String ->
        judo.aliases.undefine(inputSpec)
    }
}
