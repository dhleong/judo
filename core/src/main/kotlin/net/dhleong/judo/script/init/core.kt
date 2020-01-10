package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject
import net.dhleong.judo.script.adaptSuspend

/**
 * @author dhleong
 */
fun ScriptInitContext.initCore() =
    sequenceOf(CoreScripting(this))

@Suppress("unused")
class CoreScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Set or get the value of a setting, or list all settings
    """)
    fun config() {
        context.mode.config(emptyArray())
    }

    fun config(setting: String) {
        context.mode.printSettingValue(setting)
    }

    fun config(setting: String, value: Any) {
        context.mode.config(arrayOf(setting, value))
    }

    @Doc("Echo some transient text to the screen locally.")
    fun echo(vararg args: Any) {
        context.judo.echo(*args)
    }

    @Doc("Print some output into the current buffer locally.")
    fun print(vararg args: Any) {
        context.judo.print(*args)
    }

    @Doc("""
        Feed some text into the text completion system.
        NOTE: This does not yet guarantee that the provided words will
        be suggested in the sequence provided, but it may in the future.
    """)
    fun complete(text: String) {
        context.judo.seedCompletion(text)
    }

    @Doc("""
        Request a string from the user, returning whatever they typed.
        NOTE: Unlike the equivalent function in Vim, input() DOES NOT currently
        consume pending input from mappings.
    """)
    fun input() = input("")
    fun input(prompt: String) = context.adaptSuspend {
        context.mode.readInput(prompt)
    }

    @Doc("""
        Process [keys] as though they were typed by the user in normal mode.
        To perform this operation with remaps disabled (as in nnoremap), pass
        False for the second parameter.
    """)
    fun normal(keys: String) = normal(keys, true)
    fun normal(keys: String, remap: Boolean) = context.adaptSuspend {
        context.judo.feedKeys(keys, remap, mode = "normal")
    }

    @Doc("""
        Force a redraw of the screen; clears any echo()'d output
    """)
    fun redraw() {
        context.judo.redraw()
    }

    @Doc("""
        Exit Judo.
    """)
    fun quit() {
        context.judo.quit()
    }

    @Doc("""
        Reload the last-loaded, non-MYJUDORC script file.
    """)
    fun reload() {
        context.mode.reload()
    }

    @Doc("""
        Send some text to the connected server.
    """)
    fun send(text: String) {
        context.judo.send(text, true)
    }

}
