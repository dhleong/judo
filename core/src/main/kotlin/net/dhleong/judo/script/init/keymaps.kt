package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initKeymaps() {
    // mapping functions
    sequenceOf(
        "" to "",
        "c" to "cmd",
        "i" to "insert",
        "n" to "normal"
    ).forEach { (letter, modeName) ->

        registerFn<Unit>(
            "${letter}map",
            doc {
                usage {
                    arg("inputKeys", "String")
                    arg("output", "String/Fn")
                }
                usage { /* no args to list */ }

                body { "Create a mapping in a specific mode from inputKeys to outputKeys" }
            }
        ) { args: Array<Any> ->
            when (args.size) {
                0 -> judo.printMappings(modeName)
                1 -> {
                    // special case; map(mode)
                    judo.printMappings(args[0] as String)
                }
                else -> {
                    mode.createMap(modeName, args[0] as String, args[1], true)
                }
            }
        }

        registerFn<Unit>(
            "${letter}noremap",
            doc {
                usage {
                    arg("inputKeys", "String")
                    arg("output", "String/Fn")
                }
                usage { /* no args to list */ }

                body { "Create a mapping in a specific mode from inputKeys to outputKeys" }
            }
        ) { inputKeys: String, output: Any ->
            mode.createMap(modeName, inputKeys, output, false)
        }

        registerFn<Unit>(
            "${letter}unmap",
            doc {
                usage {
                    arg("inputKeys", "String")
                }

                body { "Delete a mapping in the specific mode with inputKeys" }
            }
        ) { inputKeys: String ->
            judo.unmap(modeName, inputKeys)
        }
    }

    registerFn<Unit>(
        "createMap",
        doc {
            usage {
                arg("modeName", "String")
                arg("inputKeys", "String")
                arg("outputKeys", "String")
                arg("remap", "Boolean", isOptional = true)
            }
            body {
                """
                    Create a mapping in a specific mode from inputKeys to outputKeys.
                    If remap is provided and True, the outputKeys can trigger other mappings.
                    Otherwise, they will be sent as-is.
                """.trimIndent()
            }
        }
    ) { args: Array<Any> ->
        val remap =
            if (args.size == 4) args[3] as Boolean
            else false
        mode.createMap(args[0] as String, args[1] as String, args[2], remap)
    }

    registerFn<Unit>(
        "deleteMap",
        doc {
            usage {
                arg("modeName", "String")
                arg("inputKeys", "String")
            }
            body { "Delete a mapping in the specific mode with inputKeys" }
        }
    ) { modeName: String, inputKeys: String ->
        judo.unmap(modeName, inputKeys)
    }
}

