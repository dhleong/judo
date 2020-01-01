package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initColors() = with(engine) {
    registerFn<Unit>(
        "recolor",
        doc {
            usage {  }
            usage {
                arg("kind", "String")
                arg("name", "String/Int")
                arg("replacement", "String/Int/Fn")
            }
            body { """
                If invoked with no arguments, prints a demo of the current
                color theme to the current Buffer.
                
                Otherwise, update the current color theme to transform a specific color.
                    kind - For either the "fg" (foreground), "bg" (background),
                           or "all" (any time the color appears)
                    name - The color name or ansi code. The 16 basic colors
                           ("black," "bright-black," "red," "bright-red," etc)
                           may use a String or their ANSI number. The "default"
                           color must be specified by name. The extended 256-color
                           set must be specified by ANSI number. RGB colors must be
                           specified by "#RRGGBB" hex.
                           Alternatively, the following special category names may be used:
                           
                               "simple" - matches all 16 basic colors
                               "256" - matches all extended 256-color set colors
                               "rgb" - matches all RGB colors
                               
                    replacement - How to replace the matched color. If a String or Int,
                                  it is treated as a literal `name` (see above). If a
                                  function, it must be either of the form:
                                  
                                      (name: String/Int) -> String/Int
                                  
                                  or:
                                  
                                      (kind: String, name: String/Int) -> String/Int
                                  
                                  `kind` and `name` or formatted as above, but `name` will
                                  never be one of the special category names.
            """.trimIndent() }
        }
    ) { args: Array<Any> ->
        if (args.isEmpty()) {
            TODO("print colors")
            return@registerFn
        }

        val (kindRaw, name, replacement) = args
        TODO("replace")
    }

    registerFn<Unit>(
        "unrecolor",
        doc {
            usage {
                arg("kind", "String")
                arg("name", "String/Int")
            }
            body { """
                Revert a `recolor` setting; see `recolor` for arg meanings.
            """.trimIndent() }
        }
    ) { kind: String, name: String ->
        TODO()
    }
}
