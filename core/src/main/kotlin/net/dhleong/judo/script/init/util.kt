package net.dhleong.judo.script.init

import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn

/**
 * @author dhleong
 */
fun ScriptInitContext.initUtil() = with(mode) {
    registerFn<String?>(
        "expandpath",
        doc {
            usage { arg("pathType", "String?") }

            body { """
                Access various "paths":
                
                <init>      Full path to the loaded init script
                <lastread>  Full path to the last-read script
                <sfile>     Full path to the currently-executing script (if any)
            """.trimIndent() }
        }
    ) { type: String -> expandPath(type) }
}