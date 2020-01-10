package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject

/**
 * @author dhleong
 */
fun ScriptInitContext.initModes() = sequenceOf(
    ModesScripting(this)
)

@Suppress("unused")
class ModesScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Create a new mode with the given name. Mappings can be added to it
        using the createMap function
    """)
    fun createUserMode(modeName: String) = with(context) {
        judo.createUserMode(modeName)
    }

    @Doc("""
        Enter the mode with the given name.
    """)
    fun enterMode(modeName: String) = with(context) {
        judo.enterMode(modeName)
    }

    @Doc("Exit the current mode.")
    fun exitMode() = with(context) {
        judo.exitMode()
    }

    @Doc("""
        Enter insert mode as if by pressing `i`
    """)
    fun startInsert() = with(context) {
        judo.enterMode("insert")
    }

    @Doc("Exit insert mode as soon as possible.")
    fun stopInsert() = with(context) {
        judo.exitMode()
    }

}
