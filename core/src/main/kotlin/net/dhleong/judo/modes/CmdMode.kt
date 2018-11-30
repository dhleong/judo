package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.script.doc
import java.io.InputStream

/**
 * @author dhleong
 */
class CmdMode(
    judo: IJudoCore,
    private val ids: IdManager,
    inputBuffer: InputBuffer,
    rendererInfo: JudoRendererInfo,
    history: IInputHistory,
    private var completions: CompletionSource,
    private val engineFactory: ScriptingEngine.Factory
) : BaseCmdMode(judo, inputBuffer, rendererInfo, history) {

    private val engine by lazy {
        engineFactory.create().apply {
            init()
        }
    }

    override fun execute(code: String) {
        engine.execute(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.readFile(fileName, stream)
    }

    private fun createMap(modeName: String, fromKeys: Any, mapTo: Any, remap: Boolean) {
        when (mapTo) {
            is String -> judo.map(
                modeName,
                fromKeys as String,
                mapTo,
                remap
            )

            // TODO ?
//            is PyFunction -> judo.map(
//                modeName,
//                fromKeys as String,
//                {
//                    wrapExceptions {
//                        mapTo.__call__()
//                    }
//                },
//                mapTo.toString()
//            )

            else -> throw IllegalArgumentException("Unexpected map-to value")
        }

        queueMap(modeName, fromKeys)
    }

    private fun ScriptingEngine.init() {
        registerConst(
            "MYJUDORC",
            doc {
                body {
                    "Path to the Judo config file"
                }
            },
            USER_CONFIG_FILE.absolutePath
        )

        // TODO mapping functions
        registerFn<Unit>(
            "createMap",
            doc {
                usage {
                    arg("modeName", "String")
                    arg("inputKeys", "String")
                    arg("outputKeys", "String")
                }
                body { "Create a mapping in a specific mode from inputKeys to outputKeys" }
            }
        ) { args: Array<Any> ->
            val remap =
                if (args.size == 4) args[3] as Boolean
                else false
            createMap(args[0] as String, args[1] as String, args[2], remap)
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

        registerFn<Unit>(
            "config",
            doc {
                usage {
                    arg("setting", "String")
                    arg("value", "Any")
                }
                usage { arg("setting", "String") }
                usage { /* no-arg */ }

                body { "Set or get the value of a setting, or list all settings" }
            }
        ) { args: Array<Any> -> config(args) }

        registerFn(
            "connect",
            doc {
                usage {
                    arg("host", "String")
                    arg("port", "Int")
                }

                body { "Connect to a server." }
            },
            judo::connect
        )

        registerFn<Unit>(
            "echo",
            doc {
                usage { withVarArgs() }
                body { "Print some output to the screen locally." }
            }
        ) { args: Array<Any> -> judo.echo(*args) }

        registerFn<Unit>(
            "send",
            doc {
                usage { arg("text", "String") }
                body { "Send some text to the connected server." }
            }
        ) { text: String -> judo.send(text, true) }
    }
}
