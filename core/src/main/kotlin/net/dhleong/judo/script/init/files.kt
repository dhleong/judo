package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import java.io.File

/**
 * @author dhleong
 */
fun ScriptInitContext.initFiles() {
    registerFn<Unit>(
        "load",
        doc {
            usage { arg("pathToFile", "String") }
            body { "Load and execute a script." }
        }
    ) { pathToFile: String -> mode.load(pathToFile) }

    registerFn<Unit>(
        "logToFile",
        doc {
            usage {
                arg("pathToFile", "String")
                arg("options", "String", isOptional = true)
            }
            body {
                """Enable logging to the given file with the given options. `options` is
          |a space-separated string that may contain any of:
          | append - Append output to the given file if it already exists, instead
          |          of replacing it
          | raw - Output the raw data received from the server, including ANSI codes
          | plain - Output the plain text received from the server, with no coloring
          | html - Output the data received from the server formatted as HTML.
        """.trimMargin()
            }
        }
    ) { args: Array<Any> ->
        if (args.size == 1) mode.logToFile(args[0] as String)
        else mode.logToFile(args[0] as String, args[1] as String)
    }

    registerFn<Unit>(
        "persistInput",
        doc {
            usage { }
            usage { arg("path", "String") }
            body {
                """Enable input history persistence for the current world, optionally
          |providing the path to save the history. If not provided, it will pick
          |a path in the ~/.config/judo directory with a filename based on the
          |currently-connected world (which means this should be called AFTER a
          |call to connect()).
          |This will immediately attempt to import input history from the given
          |file, and writes the new history on disconnect. Persistence is also
          |disabled on disconnect, so you'll need to call this again the next time
          |you connect.
        """.trimMargin()
            }
        }
    ) { args: Array<Any> ->
        if (args.isNotEmpty()) {
            judo.persistInput(File(args[0] as String))
        } else {
            mode.persistInput()
        }
    }
}
