package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import java.io.File

/**
 * @author dhleong
 */
fun ScriptInitContext.initFiles() = sequenceOf(
    FilesScripting(this)
)

@Suppress("unused")
class FilesScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Load and execute a script.
    """)
    fun load(pathToFile: String) {
        context.mode.load(pathToFile)
    }

    @Doc("""
        Enable logging to the given file with the given options. `options` is
        a space-separated string that may contain any of:
        
            append - Append output to the given file if it already exists, instead
                     of replacing it
            raw - Output the raw data received from the server, including ANSI codes
            plain - Output the plain text received from the server, with no coloring
            html - Output the data received from the server formatted as HTML.
    """)
    fun logToFile(pathToFile: String) = context.mode.logToFile(pathToFile)
    fun logToFile(pathToFile: String, options: String) =
        context.mode.logToFile(pathToFile, options)

    @Doc("""
        Enable input history persistence for the current world, optionally
        providing the path to save the history. If not provided, it will pick
        a path in the ~/.config/judo directory with a filename based on the
        currently-connected world (which means this should be called AFTER a
        call to connect()).
        This will immediately attempt to import input history from the given
        file, and writes the new history on disconnect. Persistence is also
        disabled on disconnect, so you'll need to call this again the next time
        you connect.
    """)
    fun persistInput() = context.mode.persistInput()
    fun persistInput(path: String) = context.judo.persistInput(File(path))

    @Doc("""
        Enable output persistence for the current world, optionally providing
        the path to save the history. If not provided, it will pick
        a path in the ~/.config/judo directory with a filename based on the
        currently-connected world (which means this should be called AFTER a
        call to connect()). 
        This will immediately attempt to import output history from the given
        file, and writes the new history on disconnect. Persistence is also
        disabled on disconnect, so you'll need to call this again the next time
        you connect.
    """)
    fun persistOutput() = context.mode.persistOutput()
    fun persistOutput(path: String) {
        context.mode.persistOutput(File(path))
    }

}
