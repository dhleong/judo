package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject

/**
 * @author dhleong
 */
fun ScriptInitContext.initConsts() =
    sequenceOf(ConstScripting(this))

@Suppress("PropertyName", "unused")
class ConstScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Path to the Judo config file
    """)
    val MYJUDORC: String = context.userConfigFile.absolutePath

    @Doc("""
         Reference to the core Judo scripting object
    """)
    val judo: Any = with (context.engine) {
        wrapCore(context.judo)
    }

}
