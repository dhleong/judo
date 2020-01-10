package net.dhleong.judo.script.init

import net.dhleong.judo.script.Doc
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingObject

/**
 * @author dhleong
 */
fun ScriptInitContext.initUtil() = sequenceOf(
    UtilScripting(this)
)

@Suppress("unused")
class UtilScripting(
    private val context: ScriptInitContext
) : ScriptingObject {

    @Doc("""
        Access various "paths":
        
        <init>      Full path to the loaded init script
        <lastread>  Full path to the last-read script
        <sfile>     Full path to the currently-executing script (if any)
    """)
    fun expandpath(pathType: String) = context.mode.expandPath(pathType)

}